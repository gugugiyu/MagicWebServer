package com.github.magic.examples;

import com.github.magic.core.models.server.Server;
import com.github.magic.core.path_handler.StaticFileHandler;

public class ServeStaticFile {
        public static void main(String[] args) {  
        //Initialize the server      
        Server app = new Server();

        app.get("/", (req, res) -> res.send("Example: Serve static file"));


        // First, please visit the Config class and make sure the STATIC_DIR is set to the right directory

        // If your static files are at "%ROOT_DIR%/data/static_data/" then please edit it like such:
        // public static final String STATIC_DIR = ROOT_DIR + "/data/static_data"; 

        // There're two ways to server a static file, first way is calling the "sendFile()" method like this
        app.get("/sendFile", (req, res) -> res.sendFile("./index.html"));

        //Or there's no logic required, you can use the built in StaticFileHandler handler to do the job
        app.get("/staticFileHandler", new StaticFileHandler("./index.html"));

        //And it works with pretty much every file, as long as the MIME-type is resolvable
        app.get("/catJson", new StaticFileHandler("./cat.json"));

        //Start up the server on port 80
        app.listen();
    }
}
