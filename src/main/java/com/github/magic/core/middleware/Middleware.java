package com.github.magic.core.middleware;

import com.github.magic.core.models.Request;
import com.github.magic.core.models.Response;

public interface Middleware {
    interface NextCallback{
        void next();
    }

    /**
     * The handle interface inspired by ExpressJs architecture
     *
     * @param req Parsed request
     * @param res Response to be sent back
     * @param next The callback function, if not called, the request-response cycle will be hanged, and eventually cleared out by the timeout handler
     * @throws Exception Any exception that may be thrown from the interface
     */
    void handle(Request req, Response res, NextCallback next) throws Exception;
}
