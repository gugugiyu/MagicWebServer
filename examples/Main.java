package examples;

import com.github.magic.core.models.server.Server;

public class Main {
    public static void main(String[] args) {  
        //Initialize the server      
        Server app = new Server();

        //Serve "root" text at the root path
        app.get("/", (req, res) -> res.send("root"));

        //Redirect all other path to the root path
        app.get("/*", (req, res) -> {
            res.redirect("http://localhost/");
        });

        //Start up the server on port 80
        app.listen();
    }
}