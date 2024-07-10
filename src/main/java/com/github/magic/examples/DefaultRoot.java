package com.github.magic.examples;

import com.github.magic.core.models.server.Server;

public class DefaultRoot {
    public static void main(String[] args) {  
        //Initialize the server      
        Server app = new Server();

        //Serve "root" text at the root path
        app.get("/", (req, res) -> res.send("root"));

        //Start up the server on port 80
        app.listen();
    }
}