package com.github.magic.examples;

import com.github.magic.core.models.server.Server;

public class Wildcard {
    public static void main(String[] args) {  
        //Initialize the server      
        Server app = new Server();

        //Matches the following cases:
        //1. "wildddddddcard"
        //2. "wild123ccccard"
        //3. "wildcard"

        app.get("/wild*card", (req, res) -> res.send("wildcard"));

        //Start up the server on port 80
        app.listen();
    }
}
