package com.github.magic.core.models.routing_tries;

import com.github.magic.core.consts.HttpMethod;
import com.github.magic.core.middleware.Middleware;
import com.github.magic.core.path_handler.Handler;
import com.github.magic.core.path_handler.HandlerWithParam;

import java.util.ArrayList;

public class URITries {
    Node root;

    public URITries(Handler func) {
        //The root path for serving
        this.root = new Node(HttpMethod.GET, "/", func);
    }

    public URITries(){
        this(null);
    }

    //Register handler for get request
    public void get(String endpoint, ArrayList<Middleware> middlewares, Handler func) {
        root.register(HttpMethod.GET, endpoint, middlewares, func);
    }

    //Register handler for post request
    public void post(String endpoint, ArrayList<Middleware> middlewares, Handler func) {
        root.register(HttpMethod.POST, endpoint, middlewares, func);
    }

    //Register handler for delete request
    public void delete(String endpoint, ArrayList<Middleware> middlewares, Handler func) {
        root.register(HttpMethod.DELETE, endpoint, middlewares, func);
    }

    public void put(String endpoint, ArrayList<Middleware> middlewares, Handler func) {
        root.register(HttpMethod.PUT, endpoint, middlewares, func);
    }

    public void options(String endpoint, ArrayList<Middleware> middlewares, Handler func) {
        root.register(HttpMethod.OPTIONS, endpoint, middlewares, func);
    }

    //Get the handler method based on the url endpoint
    public HandlerWithParam find(HttpMethod method, String path) {
        return root.find(method, path);
    }

    public Node getRoot() {
        return root;
    }

    public void setRoot(Node root) {
        this.root = root;
    }
}
