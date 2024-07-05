package com.github.magic.core.path_handler;

import com.github.magic.core.models.Request;
import com.github.magic.core.models.Response;

import java.io.IOException;

public class EchoPathHandler implements Handler {
    @Override
    public void handle(Request req, Response res) throws IOException {
        String data = req.getPath().getPath() == null ? "" : req.getPath().getPath();

        res.send(data);
    }
}
