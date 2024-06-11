package core.models.routing_tries;

import core.path_handler.HandlerWithParam;
import core.path_handler.Handler;
import core.consts.HttpMethod;

public class URITries {
    Node root;

    public URITries(Handler func){
        //The root path for serving
        this.root = new Node(HttpMethod.GET, "/", func);
    }

    //Register handler for get request
    public void get(String endpoint, Handler func){
        root.register(HttpMethod.GET, endpoint, func);
    }

    //Register handler for post request
    public void post(String endpoint, Handler func){
        root.register(HttpMethod.POST, endpoint, func);
    }

    //Register handler for delete request
    public void delete(String endpoint, Handler func){
        root.register(HttpMethod.DELETE, endpoint, func);
    }

    //Get the handler method based on the url endpoint
    public HandlerWithParam find(HttpMethod method, String path){
        return root.find(method, path);
    }

    public Node getRoot() {
        return root;
    }

    public void setRoot(Node root) {
        this.root = root;
    }
}
