package com.github.magic.core.consts.path_handler;

import com.github.magic.core.models.Request;
import com.github.magic.core.models.Response;

import java.io.IOException;

@FunctionalInterface
public interface Handler {
    void handle(Request req, Response res) throws IOException;
}
