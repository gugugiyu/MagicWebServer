package core.models.server;

import core.config.Config;

import java.util.concurrent.*;

public class TimeoutThreadPool extends ThreadPoolExecutor {
    private TimeoutThreadPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
    }

    public static TimeoutThreadPool getDefault() {
        //Set up the thread pools
        ArrayBlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(100);
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
        //Handle with Future<?> for timeout
        return super.submit(task);
    }
}
