package com.github.magic.core.models.threads;

import com.github.magic.core.config.Config;
import com.github.magic.core.consts.HttpCode;
import com.github.magic.core.consts.HttpMethod;
import com.github.magic.core.middleware.Middleware;
import com.github.magic.core.models.Request;
import com.github.magic.core.models.Response;
import com.github.magic.core.models.header.Header;
import com.github.magic.core.models.header.Headers;
import com.github.magic.core.models.routing_tries.URITries;
import com.github.magic.core.models.server.Server;
import com.github.magic.core.path_handler.HandlerWithParam;
import com.github.magic.core.path_handler.StaticFileHandler;
import com.github.magic.core.utils.Formatter;
import com.github.magic.core.utils.StreamTransfer;
import com.github.magic.ssl.models.SSLServer;

import javax.net.ssl.SSLSocket;
import java.io.Closeable;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.Socket;
import java.util.*;

public class TransactionThread implements Runnable, Closeable {
    //Making sure that each "next" callback from middlewares should only be called once
    private static boolean isNextMiddlewareCalled = false;

    private final Socket   sock;
    private final Server serverInstance;
    private Response res;
    private final URITries tries;
    private Request req;

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
    @Override
    public void close() {
        if (res == null)
            return;

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
            ((SSLSocket) sock).addHandshakeCompletedListener((ignored) -> isHandshakeCompleted = true);
        }else{
            //HTTP doesn't have ssl handshake, simply skip this step
            isHandshakeCompleted = true;
        }

        HandlerWithParam handlerWithParam = new HandlerWithParam(null, null, null);

        //The total amount of request, response cycle can be done through this connection
        int counter = serverInstance.getServerConfig().getMaxServePerConnection();

        do {
            req = null;
            res = new Response(sock.getOutputStream());

            try {
                //handle this case the same as request timeout
                if (counter < 0) throw new InterruptedIOException();

                req = new Request(sock);

                //Protocol mismatched then close the connection immediately
                if (req == null || req.isMismatched()) break;

                res = new Response(req, counter, isHandshakeCompleted);

                //Only support from version 1.1 downwards
                if (!compatibleHttpVersion() || upgradeSecure()) break;

                handlerWithParam = tries.find(req.getMethod(), req.getPath().getPath());

                req.setParams(handlerWithParam.params());

                if (handlerWithParam.handler() == null) {
                    // use default version
                    handleDefaultMethod();
                } else {
                    //Only run the subsequent middlewares and the main handler if none former middlewares have served the response
                    List<Middleware> middlewares = handlerWithParam.middlewares();
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

                    handlerWithParam.handler().handle(req, res);
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

    /**
     * When no handler for the current resource is provided, this method will be invoked
     * 
     * Current support {@link HttpMethod}
     * 
     * @throws IOException
     */
    protected void handleDefaultMethod() throws IOException {
        switch (req.getMethod()){
            case GET:
                new StaticFileHandler(req.getPath().getPath()).handle(req, res);
                break;

            case HEAD:
                req.setMethod(HttpMethod.GET);
                res.setDiscardBody(true);
                handleDefaultMethod(); //Recurse this function with GET method
                break;

            case TRACE:
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

    private void handleException(Throwable t) {
        t.printStackTrace();
        //Connections that are abruptedly disconnected by client doesn't need a response
        if (t.getMessage().startsWith("Connection reset"))
            return;
           
        if (req == null) {
            //Failed parsing request or timeout
            if (t instanceof InterruptedIOException || 
                t instanceof IOException && t.getMessage().contains("line"))
                return;

            // RFC9112#3 - must return 414 if URI is too long
            if (t instanceof IOException && t.getMessage().contains("URI too long")){
                res.sendError(HttpCode.URI_TOO_LONG);
                return;
            }

            if (isHandshakeCompleted) 
                res.sendError(HttpCode.BAD_REQUEST, "Invalid request: " + t.getMessage());
        } else {
            res.sendError(HttpCode.INTERNAL_SERVER_ERROR, "Server error :(\nHere's what happened: " + t.getMessage());
        }
    }

    private boolean compatibleHttpVersion(){
        if (!req.getVersion().startsWith("1")) {
            res.setHeader("Connection", "close");
            res.sendError(HttpCode.HTTP_VERSION_NOT_SUPPORTED);
            return false;
        }

        return true;
    }

    /**
     * Redirect
     * @return
     * @throws IOException
     */
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

        return true;
    }

    /**
     * Decide whether this current http session should continue. By default, any Http/1.1 connection will be persistent, unless explicitly specified "Connection"
     * header's value to be "close"
     * 
     * @return flag tells if the current connection should be continue
     */
    private boolean transactionContinue(){
        String reqConnectionStatus = req.getHeaders().find("Connection");

        return  reqConnectionStatus.isEmpty()
                && !"close".equalsIgnoreCase(reqConnectionStatus)
                && !req.getVersion().equalsIgnoreCase("1.0");
    }

    /**
     * Handles TRACE http method. This method is default to be always non-persistent
     * @throws IOException
     */
    private void handleTrace() throws IOException{
        StringBuilder traceBody = new StringBuilder();
        String traceBodyToString;

        traceBody.append("TRACE ").append(req.getPath().toString()).append(" HTTP/").append(req.getVersion()).append("\r\n");

        //Prepare the header sent from request
        Headers reqHeaders = req.getHeaders();

        for (Header header : reqHeaders)
            traceBody.append(header.getKey()).append(": ").append(header.getValue()).append("\r\n");

            traceBodyToString = traceBody.toString();

        res.setHeader("Date", Formatter.convertTime(null));
        res.setHeader("Content-Type", "message/http");
        res.setHeader("Content-Length", "" + traceBodyToString.trim().length());
        res.setHeader("Connection", "close"); // TRACE is meant for debugging purpose, thus persisting this connection has no usage

        res.setDefaultHeader(false);

        res.send(traceBodyToString);

        //Only consume if input's available
        if (req.getRequestSocket().getInputStream().available() > 0)
            StreamTransfer.transfer(req.getRequestSocket().getInputStream(), res.getOutputStream(), -1); // RFC9110#9.3.8 - client must not send content (but we echo it anyway)
    }
}
