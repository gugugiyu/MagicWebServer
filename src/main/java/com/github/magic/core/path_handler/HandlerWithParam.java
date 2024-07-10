package com.github.magic.core.path_handler;

import com.github.magic.core.middleware.Middleware;

import java.util.List;
import java.util.Map;

public record HandlerWithParam(Handler handler, Map<String, String> params, List<Middleware> middlewares) {}
