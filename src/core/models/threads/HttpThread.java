package core.models.threads;

import core.models.Request;
import core.models.Response;
import core.path_handler.Handler;
import core.path_handler.HandlerWithParam;
import core.path_handler.StaticFileHandler;
import core.models.routing_tries.URITries;

import java.io.*;
import java.net.Socket;

public class HttpThread implements Runnable{
    protected Request request;
    protected Response response;
    protected HandlerWithParam handlerWithParam;
    protected boolean forceClose;

    public HttpThread(Request request, Response response, Handler handler){
        this.request = request;
        this.response = response;
        this.handlerWithParam  = (HandlerWithParam) handler;
    }

    public HttpThread(Socket requestSocket, boolean forceClose, URITries routingTries) throws IOException {
        //In case we failed to parse out the request, then reject it immediately
        try{
            this.request = new Request(requestSocket);
        } catch (IllegalArgumentException e){
            System.out.println("[-] " + e.getMessage());
            return;
        }

        this.response = new Response(request, forceClose);

        if (routingTries == null){
            handlerWithParam = null;
        }else{
            this.handlerWithParam = routingTries.find(request.method ,request.path.getPath());

            //Set the route parameters
            this.request.params = handlerWithParam.getParams();
        }

        this.forceClose = forceClose;
    }

    @Override
    public void run() {
        try {
            //It mustn't be force close
            if (handlerWithParam.getHandler() == null){
                //Either this route has not been registered
                //or it isn't valid at all

                if (!forceClose)
                    new StaticFileHandler(request.path.getPath()).handle(request, response);
            }else{
                handlerWithParam.getHandler().handle(request, response);
            }
        } catch (IOException e){
            System.out.println("[-] Thread can't execute");
        } finally {
            if (!response.isSent()){
                //Enter an infinite loop, waiting for the timeout thread to kick in and clean up this thread
                System.out.println("[-] Thread executes successfully but doesn't respond. Check your code if you forget to send back a response in any place?");

                while(true){
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored){}
                }
            }
        }
    }
}
