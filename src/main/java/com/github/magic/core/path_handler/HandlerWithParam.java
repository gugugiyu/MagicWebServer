package com.github.magic.core.path_handler;

import com.github.magic.core.middleware.Middleware;

import java.util.List;
import java.util.Map;

public class HandlerWithParam {
    private final Handler handler;
    private final Map<String, String> params;
    private final List<Middleware> middlewares;

    public HandlerWithParam(Handler handler, Map<String, String> params, List<Middleware> middlewares) {
        this.handler = handler;
        this.params = params;
        this.middlewares = middlewares;
    }

    public Handler getHandler() {
        return handler;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public List<Middleware> getMiddlewares() {
        return middlewares;
    }
}
