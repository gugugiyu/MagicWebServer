package core.models;

import core.models.routing_tries.URITries;
import core.models.threads.HttpThread;
import core.models.threads.ShutdownThread;
import core.models.threads.TimeoutThread;
import core.path_handler.Handler;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;

public class Server {
    private URITries routingTree;
    private long timeout;

    private final int corePoolSize = 5;
    private final int maximumPoolSize = 20;
    private final long keepAliveTime = 500;
    private final TimeUnit unit = TimeUnit.SECONDS;

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    //bytes array representation of CRLF
    public static final byte[] CRLF = { 0x0d, 0x0a };

    public Server(URITries routingTree) {
        this.routingTree = routingTree;
    }

    public Server(){
        this.routingTree = new URITries(null);
        this.timeout = 100; //Timeout after 100 seconds
    }

    public void listen(int PORT){
        try {
            ServerSocket serverSocket = new ServerSocket(PORT);

            //Set up the thread pools
            ArrayBlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(100);
            RejectedExecutionHandler handler = new ThreadPoolExecutor.CallerRunsPolicy();
            ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(corePoolSize,
                    maximumPoolSize, keepAliveTime, unit, workQueue, handler);

            //Set up the scheduler to handle thread frozen case
            //In this scenario, we have to return either 503 Service Unavailable
            //or 504 Gateway Timeout in case we're fetching from another sources
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(corePoolSize);

            //Server listening
            System.out.println("[+] Server is listening on port " + PORT);

            while (true) {
                Socket connectionSocket = serverSocket.accept();

                //The http task
                HttpThread executeThread = new HttpThread(connectionSocket, false, routingTree);

                //Handle with Future<?> for timeout
                Future<?> futureRes = threadPoolExecutor.submit(executeThread);

                scheduler.schedule(() -> {
                    if (!futureRes.isDone()){
                        futureRes.cancel(true);
                        System.out.println("[-] Request has been time out");

                        //Execute the timeout thread that handles the error response
                        try {
                            threadPoolExecutor.execute(new TimeoutThread(executeThread));
                        } catch (IOException e) {
                            //This case should be really rare, we'll have to shut down the connection entirely
                            System.out.println("[-] Timeout thread initialization failed");
                        }
                    }
                }, timeout, TimeUnit.SECONDS);
            }

        } catch (IOException e) {
            System.out.println("[-] Server failed to boot up at port " + PORT);
            e.printStackTrace();
        }

        //Handle unexpected reboot and shutdown
        Runtime.getRuntime().addShutdownHook(new ShutdownThread());
    }

    //Integrated routing registration inside server to imitate nodeJS behavior
    public void get(String path, Handler handler){
        routingTree.get(path, handler);
    }

    public void get(String[] paths, Handler handler){
        for (String path : paths){
            routingTree.get(path, handler);
        }
    }

    public void post(String path, Handler handler){
        routingTree.post(path, handler);
    }

    public void delete(String path, Handler handler){
        routingTree.delete(path, handler);
    }
}
