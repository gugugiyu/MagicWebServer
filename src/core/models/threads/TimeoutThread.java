package core.models.threads;

import core.consts.HttpCode;
import core.path_handler.TimeoutHandler;
import core.models.routing_tries.URITries;

import java.io.IOException;
import java.net.Socket;

public class TimeoutThread extends HttpThread{
    public TimeoutThread(Socket requestSocket, URITries routingTries) throws IOException {
        //Unused constructor
        super(requestSocket, false, routingTries);
    }

    public TimeoutThread(Socket requestSocket, boolean forceClose) throws IOException {
        super(requestSocket, forceClose, null);
    }

    public TimeoutThread(HttpThread timeoutThread) throws IOException {
        super(timeoutThread.request, timeoutThread.response, null);
    }

    @Override
    public void run() {
        try {
            new TimeoutHandler(HttpCode.SERVICE_UNAVAILABLE).handle(request, response);
        } catch (IOException e) {
            System.out.println("[-] Timeout thread can't execute");

            e.printStackTrace();
        } finally {
            //Clean up the http thread in case in got cancel by future and execute this thread
            try {
                request.requestSocket.close();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}
