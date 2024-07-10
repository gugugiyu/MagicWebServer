package com.github.magic.examples;

import com.github.magic.core.models.server.Server;

public class RouteParameter {
    public static void main(String[] args) {  
        //Initialize the server      
        Server app = new Server();
        
        //Basic route paramter
        app.get("/test/:testId", (req, res) -> {
            String testId = req.params.get("testId");

            res.send("Test id: " + testId);
        });

        //Route paramter with regex, only match positive ID numbers 
        app.get("/testRegex/:testId((^\\d+$))", (req, res) -> {
            String testId = req.params.get("testId");

            res.send("Positive test id: " + testId);
        });

        //Start up the server on port 80
        app.listen();
    }
}
