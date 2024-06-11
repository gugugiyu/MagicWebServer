package core.path_handler;

import core.models.Request;
import core.models.Response;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public interface Handler{
    void handle(Request request, Response response) throws IOException;
}
