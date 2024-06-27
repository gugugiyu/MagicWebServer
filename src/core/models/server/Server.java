package core.models.server;

import core.config.Config;
import core.middleware.Middleware;
import core.models.routing_tries.URITries;
import core.models.threads.ShutdownThread;
import core.models.threads.TransactionThread;
import core.path_handler.Handler;

import javax.net.ServerSocketFactory;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Server implements Runnable{
    private final URITries tries;

    private int port;
    private InetSocketAddress hostIP = Config.DEFAULT_HOST_IP;

    private ServerSocketFactory serverSocketFactory;

    private String upgradeInsecureRequestURL = "";

    public Server(int port, URITries tries) {
        this.tries = tries;
        this.port = port;
    }

    //Default port for http is 80
    public Server(URITries tries) {
        this(80, tries);
    }

    public Server(int port) {
        this(port, new URITries());
    }

    public Server() {
        this(80, new URITries());
    }

    public synchronized void listen(){
        listen(port);
    }

    public synchronized void listen(int port) {
        this.port = port;

        try {
            ServerSocket serverSocket;
            TransactionThread transactionThread;

            try {
                serverSocket = configureServer();
            } catch (IOException e) {
                if (Config.SHOW_ERROR) System.err.println("[-] Error while configuring server: " + e.getMessage());
                return;
            }

            if (Config.VERBOSE) System.out.println("[+] Server is listening on port " + port);

            TimeoutThreadPool threadPool = TimeoutThreadPool.getDefault();

            while (serverSocket != null && !serverSocket.isClosed()) {
                final Socket sock = serverSocket.accept();
                //Set the timer and starts executing
                sock.setSoTimeout(Config.THREAD_REQUEST_READ_TIMEOUT_DURATION);
                //sock.setTcpNoDelay(true); // we buffer anyway, so improve latency
                sock.setKeepAlive(true);

                transactionThread = new TransactionThread(sock, tries, this);
                //TODO use submit for timeout handler
                Future<?> status = threadPool.submit(transactionThread);

                try{
                    status.get(Config.THREAD_TIMEOUT_DURATION, TimeUnit.SECONDS);
                } catch (TimeoutException e){
                    status.cancel(true); //Switches to the 500 Internal Server Error case
                    transactionThread.end();
                } catch (InterruptedException | ExecutionException ignored){ /* Probably won't ever happen */}
            }

            //Handle unexpected reboot and shutdown
            Runtime.getRuntime().addShutdownHook(new ShutdownThread());
        } catch (IOException e) {
            if (Config.SHOW_ERROR) System.err.println("[-] Exception occur. Using port: " + port);
        }
    }

    protected ServerSocket configureServer() throws IOException {
        setServerSocketFactory(ServerSocketFactory.getDefault());

        return getServerSocket();
    }

    @Override
    public void run() {
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
        listen();
    }

    /**
     * NodeJs adding handler behavior
     */

    //Integrated routing registration inside server to imitate nodeJS behavior
    public void get(String path, Middleware[] middlewares, Handler mainHandler) {
        tries.get(path, new ArrayList<>(List.of(middlewares)), mainHandler);
    }

    public void post(String path, Middleware[] middlewares, Handler mainHandler) {
        tries.post(path, new ArrayList<>(List.of(middlewares)), mainHandler);
    }

    public void delete(String path, Middleware[] middlewares, Handler mainHandler) {
        tries.delete(path, new ArrayList<>(List.of(middlewares)), mainHandler);
    }

    public void options(String path, Middleware[] middlewares, Handler mainHandler) {
        tries.options(path, new ArrayList<>(List.of(middlewares)), mainHandler);
    }

    public void put(String path, Middleware[] middlewares, Handler mainHandler) {
        tries.delete(path, new ArrayList<>(List.of(middlewares)), mainHandler);
    }

    public void get(String path, Handler mainHandler) {
        tries.get(path, null, mainHandler);
    }

    public void put(String path, Handler mainHandler) {
        tries.put(path, null, mainHandler);
    }

    public void options(String path, Handler mainHandler) {
        tries.options(path, null, mainHandler);
    }


    /**
     * Setter & Getter
     */

    protected void setServerSocketFactory(ServerSocketFactory factory) {
        //From jhttp, with factory approach
        this.serverSocketFactory = factory;
    }

    public ServerSocket getServerSocket() throws IOException {
        ServerSocket serverSocket = serverSocketFactory.createServerSocket();

        //Config server for being to being able to rebind after previous timeout state
        serverSocket.setReuseAddress(true);
        serverSocket.bind(new InetSocketAddress(hostIP.getHostName(), port));

        return serverSocket;
    }

    /**
     * Used for the Upgrade-Insecure-Request header, when the client wants to update into a more secure version of the server
     * This feature should only be used when an {@link ssl.models.SSLServer} is up and running
     *
     * @param hostname The host name of the SSL server (must not be blank or empty)
     * @param port The port of which the SSL server is running on (if invalid inputted, will be defaulted to 443)
     */
    public void setUpgradeInsecureRequest(String hostname, int port){
        if (hostname == null || hostname.isEmpty() || hostname.isBlank())
            return;

        if (port <= 0 || port >= (1 << 16 /* 65535 */))
            port = 443;

        upgradeInsecureRequestURL = hostname + ":" + port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setHostIP(InetSocketAddress hostIP) {
        this.hostIP = hostIP;
    }

    public int getPort() {
        return port;
    }

    public String getScheme(){
        return "http";
    }

    public InetSocketAddress getHostIP() {
        return hostIP;
    }

    public String getHostname(){
        return hostIP.getHostName();
    }

    public String getUpgradeInsecureRequestURL() {
        return upgradeInsecureRequestURL;
    }
}
