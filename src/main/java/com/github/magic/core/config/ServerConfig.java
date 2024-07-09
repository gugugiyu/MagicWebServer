package com.github.magic.core.config;

import java.io.File;
import java.net.InetSocketAddress;

public class ServerConfig {
    //Maximum time allow for each thread to process the request and produce the response (ms)
    private int threadTimeoutDuration = 5000;

    //The min number of active thread per server (int)
    private int corePoolSize = 10;

    //The maximum number of thread to spawn when the request queue is full and require more threads to handle (int)
    private int maxPoolSize = 20;

    //The time for extra threads (created when the queue is full) should stay idle for (s)
    private long keepAliveTime = 500;

    //The time it takes to block the flow while InputStream is reading (Sent 408 Request Timeout in case this number is exceeded) (ms)
    private int threadRequestReadTimeoutDuration = 10000;

    //Default host IP (bind to everything)
    private InetSocketAddress hostIp = new InetSocketAddress("localhost", 80);
    
    public int getThreadTimeoutDuration() {
        return threadTimeoutDuration;
    }

    public void setThreadTimeoutDuration(int threadTimeoutDuration) {
        this.threadTimeoutDuration = threadTimeoutDuration;
    }

    public int getCorePoolSize() {
        return corePoolSize;
    }

    public void setCorePoolSize(int corePoolSize) {
        this.corePoolSize = corePoolSize;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public long getKeepAliveTime() {
        return keepAliveTime;
    }

    public void setKeepAliveTime(long keepAliveTime) {
        this.keepAliveTime = keepAliveTime;
    }

    public int getThreadRequestReadTimeoutDuration() {
        return threadRequestReadTimeoutDuration;
    }

    public void setThreadRequestReadTimeoutDuration(int threadRequestReadTimeoutDuration) {
        this.threadRequestReadTimeoutDuration = threadRequestReadTimeoutDuration;
    }

    public InetSocketAddress getHostIp() {
        return hostIp;
    }

    public void setHostIp(InetSocketAddress hostIp) {
        this.hostIp = hostIp;
    }

    
}
