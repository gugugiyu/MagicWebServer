package core.models.threads;

import core.config.Config;
import core.consts.HttpCode;
import core.consts.HttpMethod;
import core.middleware.Middleware;
import core.models.Request;
import core.models.Response;
import core.models.header.Header;
import core.models.header.Headers;
import core.models.routing_tries.URITries;
import core.models.server.Server;
import core.path_handler.HandlerWithParam;
import core.path_handler.StaticFileHandler;
import core.utils.Formatter;
import core.utils.StreamTransfer;
import ssl.models.SSLServer;

import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSocket;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.*;

import static core.consts.Misc.CRLF;

public class TransactionThread implements Runnable{
    //Making sure that each "next" callback from middlewares should only be called once
    private static boolean isNextMiddlewareCalled = false;

    private final Socket   sock;
    private Request  req;
    private Response res;
    private final URITries tries;
    private final Server   serverInstance;

    //Used for Https connection, so that the request won't accidentally parse the handshake request
    private boolean        isHandshakeCompleted;


    public TransactionThread(Socket sock, URITries tries, Server serverInstance) {
        this.sock = sock;
        this.tries = tries;
        this.serverInstance = serverInstance;
    }

    @Override
    public void run() {
        try {
            try {
                handleConnection(sock);
            } finally {
                try {
                    // [RFC9112#9.6] close socket gracefully
                    // (except SSL socket which doesn't support half-closing)
                    if (!(sock instanceof SSLSocket)) {
                        sock.shutdownOutput(); // half-close socket (only output)
                        StreamTransfer.transfer(sock.getInputStream(), null, -1); // consume input
                    }
                } finally {
                    sock.close(); // and finally close socket fully
                }
            }
        } catch (IOException ignored){}
    }

    /**
     * Should only be called when wish to interrupt the current thread after {@value Config#THREAD_TIMEOUT_DURATION} seconds (set by the @code THREAD_TIMEOUT_DURATION)
     */
    public void end(){
        res.sendError(HttpCode.INTERNAL_SERVER_ERROR);


        //Handle just like the run method
        try{
            try {
                res.close();

                if (!(sock instanceof SSLSocket)) {
                    sock.shutdownOutput(); // half-close socket (only output)
                    StreamTransfer.transfer(sock.getInputStream(), null, -1);
                }
            } finally {
                sock.close();
            }
        } catch (IOException ignored){}
    }

    private void handleConnection(Socket sock) throws IOException {
        if (sock instanceof SSLSocket){
            ((SSLSocket) sock).addHandshakeCompletedListener(_ -> isHandshakeCompleted = true);
        }else{
            //HTTP doesn't have ssl handshake, simply skip this step
            isHandshakeCompleted = true;
        }

        HandlerWithParam handlerWithParam;

        //The total amount of request, response cycle can be done through this connection
        int counter = Config.MAX_SERVE_PER_CONNECTION;

        do {
            req = null;
            res = new Response(sock.getOutputStream());

            try {
                //handle this case the same as request timeout
                if (counter < 0) throw new InterruptedIOException();

                req = new Request(sock);

                //Protocol mismatched then close the connection immediately
                if (req.isMismatched()) break;

                res = new Response(req, counter, isHandshakeCompleted);

                //Check for http version compatibility
                if (!compatibleHttpVersion()) break;

                //Check for insecure upgrade request
                if (upgradeSecure()) break;

                handlerWithParam = tries.find(req.getMethod(), req.getPath().getPath());

                req.params = handlerWithParam.getParams();

                if (handlerWithParam.getHandler() == null) {
                    // use default version
                    handleDefaultMethod();
                } else {
                    //Only run the subsequent middlewares and the main handler if none former middlewares have served the response
                    List<Middleware> middlewares = handlerWithParam.getMiddlewares();
                    Queue<Middleware> callStack = new LinkedList<>(middlewares);

                    while (!callStack.isEmpty()){
                        isNextMiddlewareCalled = false;
                        Middleware currentMiddleware = callStack.element();

                        currentMiddleware.handle(req, res, () -> {
                            if (!isNextMiddlewareCalled){
                                try{
                                    callStack.remove();
                                    isNextMiddlewareCalled = true;
                                } catch (NoSuchElementException ignored) {}
                            }
                        });
                    }

                    handlerWithParam.getHandler().handle(req, res);
                }
            } catch (Throwable t) {
                handleException(t);
                break; // proceed to close connection
            } finally {
                res.close(); // close response and flush output
            }

            counter--;
        } while (transactionContinue());
    }

    protected void handleDefaultMethod() throws IOException {
        switch (req.getMethod()){
            case HttpMethod.GET:
                new StaticFileHandler(req.getPath().getPath()).handle(req, res);
                break;

            case HttpMethod.HEAD:
                req.setMethod(HttpMethod.GET);
                res.setDiscardBody(true);
                handleDefaultMethod(); //Recurse this function with GET method
                break;

            case HttpMethod.TRACE:
                handleTrace();
                break;

            default:
                List<String> methods = new ArrayList<>(Arrays.asList("GET", "HEAD", "TRACE", "OPTIONS")); // built-in methods
                res.setHeader("Allow", String.join(",", methods)); // [RFC9110#10.2.1]

                if (req.getMethod() == HttpMethod.OPTIONS) {
                    // OPTIONS method handler
                    res.setHeader("Content-Length", "0");
                    res.send("", "text/plain", HttpCode.NO_CONTENT);
                } else if (!methods.contains(String.valueOf(req.getMethod()))) {
                    res.send("", "text/plain", HttpCode.METHOD_NOT_ALLOWED);
                } else {
                    res.send("", "text/plain",HttpCode.NOT_IMPLEMENTED);
                }

                break;
        }
    }

    private void handleException(Throwable t) throws IOException {
        if (req == null) {
            //Failed parsing request
            if (t instanceof IOException && t.getMessage().contains("line"))
                return;

            //Socket timeout, current thread interrupted
            if (t instanceof InterruptedIOException){
                res.sendError(HttpCode.REQUEST_TIMEOUT);
                return;
            }

            // RFC9112#3 - must return 414 if URI is too long
            if (t instanceof IOException && t.getMessage().contains("URI too long")){
                res.sendError(HttpCode.URI_TOO_LONG);
                return;
            }

            if (!isHandshakeCompleted) {
                res.send("", "text/plain", HttpCode.CONTINUE);
            } else {
                //TODO redirect header conflict between "Transfer-Encoding" and "Content-Encoding" to this case
                res.sendError(HttpCode.BAD_REQUEST, "Invalid request: " + t.getMessage());
            }
        } else {
            res.sendError(HttpCode.INTERNAL_SERVER_ERROR, "Error processing request: " + t.getMessage());
        }
    }

    private boolean compatibleHttpVersion(){
        if (!req.getVersion().equals("1.1")){
            res.setHeader("Connection", "close");
            res.sendError(HttpCode.HTTP_VERSION_NOT_SUPPORTED);
            return false;
        }

        return true;
    }

    private boolean upgradeSecure() throws IOException {
        if (serverInstance instanceof SSLServer)
            return false;

        String location = serverInstance.getUpgradeInsecureRequestURL();

        String path  =  req.getPath().getPath() == null ? "" : req.getPath().getPath();
        String query =  req.getPath().getQuery() == null ? "" : req.getPath().getQuery();
        String frag  =  req.getPath().getFragment() == null ? "" : req.getPath().getFragment();

        if (location.isEmpty())
            return false;

        res.setHeader("Vary", "Upgrade-Insecure-Requests"); //For clients who don't support HTTPS
        res.redirect("https://" + location + path + query + frag , true);

        return false;
    }

    private boolean transactionContinue(){
        String reqConnectionStatus = req.getHeaders().find("Connection");
        String resConnectionStatus = res.getHeaders().find("Connection");

        //No "Connection" header will be deemed as non-persistent connection
        return  !reqConnectionStatus.isEmpty()
                && !"close".equalsIgnoreCase(reqConnectionStatus)
                && resConnectionStatus.isEmpty()
                && !"close".equalsIgnoreCase(res.getHeaders().find("Connection"))
                && req.getVersion().equalsIgnoreCase("1.1");
    }

    private void handleTrace() throws IOException{
        StringBuilder traceBody = new StringBuilder();
        String traceBodyConverted;

        traceBody.append("TRACE ").append(req.getPath().toString()).append(" HTTP/").append(req.getVersion()).append("\r\n");

        //Prepare the header sent from request
        Headers reqHeaders = req.getHeaders();

        for (Header header : reqHeaders)
            traceBody.append(header.getKey()).append(": ").append(header.getValue()).append("\r\n");

        traceBodyConverted = traceBody.toString();

        res.setHeader("Date", Formatter.convertTime(new Date()));
        res.setHeader("Content-Type", "message/http");
        res.setHeader("Content-Length", "" + traceBodyConverted.trim().length());
        res.setHeader("Connection", "close"); // TRACE is meant for debugging purpose, thus persisting this connection has no usage

        res.setDefaultHeader(false);

        res.send(traceBodyConverted);

        //Only consume if input is not previously blocked
        if (req.getRequestSocket().getInputStream().available() > 0)
            StreamTransfer.transfer(req.getRequestSocket().getInputStream(), res.getOutputStream(), -1); // RFC9110#9.3.8 - client must not send content (but we echo it anyway)
    }
}
