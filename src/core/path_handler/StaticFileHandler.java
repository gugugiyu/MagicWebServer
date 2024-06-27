package core.path_handler;

import core.config.Config;
import core.models.Request;
import core.models.Response;
import core.utils.FileAttributeRetriever;

import java.io.File;
import java.io.IOException;

public class StaticFileHandler implements Handler {
    //The base directory of the current request
    private final FileAttributeRetriever baseDir;
    private final String rawPath;

    public StaticFileHandler(String path) {
        if (path.charAt(0) == '.')
            path = path.substring(1); //Remove the dot

        baseDir = new FileAttributeRetriever(new File(Config.STATIC_DIR + path));
        rawPath = path;
    }

    @Override
    public void handle(Request req, Response res) throws IOException {
        res.sendFile(baseDir, rawPath);
    }
}
