package examples;

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

        //Match all the url path starts with "/all"

        //You can get the wildcard path starting from the wildcard symbol
        //Through the "wildcard" attribute in the params
        app.get("/all/*", (req, res) -> {
            String wildcardPath = req.params.get("wildcard");

            res.send("The path you query: " + wildcardPath);
        });

        //Start up the server on port 80
        app.listen();
    }
}
