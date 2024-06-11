package core.path_handler;

import core.models.Request;
import core.models.Response;

import java.io.IOException;

public class TimeoutHandler implements Handler {
    protected int status;
    private static final int RETRY_AFTER_SECONDS = 3600;

    public TimeoutHandler(int status) {
        this.status = status;
    }

    @Override
    public void handle(Request request, Response response) throws IOException {
        response.setHeader("Retry-After", ""+RETRY_AFTER_SECONDS);

        response.sendError(status);
    }
}
