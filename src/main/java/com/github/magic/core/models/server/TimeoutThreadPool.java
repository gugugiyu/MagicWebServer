package com.github.magic.core.models.server;

import com.github.magic.core.config.Config;
import com.github.magic.core.models.threads.TransactionThread;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.*;

public class TimeoutThreadPool extends ThreadPoolExecutor {
    private TimeoutThreadPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
    }

    public static TimeoutThreadPool getDefault() {
        //Set up the thread pools
        ArrayBlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(10);
        RejectedExecutionHandler handler = new ThreadPoolExecutor.CallerRunsPolicy();

        TimeoutThreadPool retPool = new TimeoutThreadPool(
                Config.CORE_POOL_SIZE,
                Config.MAX_POOL_SIZE,
                Config.KEEP_ALIVE_TIME,
                TimeUnit.SECONDS,
                workQueue,
                handler
        );

        return retPool;
    }

    @Override
    public void execute(Runnable command) {
        super.execute(command);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return super.submit(task);
    }

    /**
     * Submits the task to the thread pool while spawning an extra thread to ensure non-blocking behavior when time out at {@value Config#THREAD_TIMEOUT_DURATION}
     *
     * @param transactionThread The transaction thread to be run
     * @param serverSocket      current server socket instance
     * @deprecated I'm currently re-writing this method as it's very expensive in terms of memory.
     */
    protected void submitWithTimer(TransactionThread transactionThread, ServerSocket serverSocket) {
        //Handle with Future<?> for timeout
        Future<?> status = submit(transactionThread);

        //System.out.println(getActiveCount());

        new Thread(() -> {
            try {
                status.get(Config.THREAD_TIMEOUT_DURATION, TimeUnit.MILLISECONDS);
            } catch (InterruptedException | TimeoutException | ExecutionException e) {
                status.cancel(true);
                transactionThread.close(); //Serve back 500 Internal Server Error case

                if (e instanceof InterruptedException) {
                    if (Config.VERBOSE)
                        System.out.println("[+] FATAL: Server has been interrupted. Shutting down... ");

                    if (serverSocket.isBound()) {
                        try {
                            serverSocket.close();
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    }

                    Thread.currentThread().interrupt();
                }
            }
        }).start();
    }
}
