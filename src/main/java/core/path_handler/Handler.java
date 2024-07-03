package core.path_handler;

import core.models.Request;
import core.models.Response;

import java.io.IOException;

public interface Handler {
    void handle(Request req, Response res) throws IOException;
}
