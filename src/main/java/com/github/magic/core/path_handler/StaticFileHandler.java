package com.github.magic.core.path_handler;

import com.github.magic.core.models.Request;
import com.github.magic.core.models.Response;

import java.io.IOException;

/**
 * Simple handle for serving static files. Should be used when no computational operation is needed to serve the request
 */
public class StaticFileHandler implements Handler {
    //The base directory of the current request
    private final String rawPath;

    public StaticFileHandler(String path) {
        rawPath = path;
    }

    @Override
    public void handle(Request req, Response res) throws IOException {
        res.sendFile(rawPath);
    }
}
