package core.path_handler;

import core.models.Request;
import core.models.Response;

import java.io.IOException;

public class EchoPathHandler implements Handler {
    @Override
    public void handle(Request req, Response res) throws IOException {
        String data = req.getPath().getPath() == null ? "" : req.getPath().getPath();

        res.send(data);
    }
}
