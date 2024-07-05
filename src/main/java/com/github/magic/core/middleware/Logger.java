package com.github.magic.core.middleware;

import com.github.magic.core.models.Request;
import com.github.magic.core.models.Response;

import java.io.IOException;
import java.io.OutputStream;

public class Logger implements Middleware {
    private String message = null;
    private final OutputStream oStream;

    public Logger(String message, OutputStream stream) {
        this.message = message;
        this.oStream = stream;
    }

    public Logger() {
        this.oStream = System.out;
    }

    public Logger(String message) {
        this.oStream = System.out;
        this.message = message;
    }

    @Override
    public void handle(Request req, Response res, NextCallback next) throws IOException, OutOfMemoryError {
        oStream.write((message != null ? message + "\n" : "[!] Logger middleware: " + req.getPath().getPath() + "\n").getBytes());

        //This middleware doesn't serve the content
        next.next();
    }
}
