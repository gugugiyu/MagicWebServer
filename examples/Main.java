package examples;

import core.middleware.Cors;
import core.middleware.Logger;
import core.middleware.Middleware;
import core.models.server.Server;
import core.path_handler.StaticFileHandler;

public class Main {
    public static void main() {
        Server app = new Server(3000);

        //This route path will match requests to the root route, /.

        app.get("/", new Middleware[]{new Cors(app), new Logger()}, (req, res) -> {
            res.send("root");
        });

        app.options("/", new Middleware[]{new Cors(app)}, (req, res) -> {
            res.send("");
        });

        app.get("/about", (req, res) -> {
            res.send("about");
        });

        app.get("/about/employee/:developer", (req, res) -> {
            String devId = req.params.get("developer");
            res.send("Received employee id: " + devId);
        });

        app.get("/about/employee/:manager", (req, res) -> {
            String managerId = req.params.get("manager");

            res.send("Received manager id: " + managerId);
        });

        app.get("/about/employee/:manager/it_manager/:name", (req, res) -> {
            String managerId = req.params.get("manager");
            String managerName = req.params.get("name");

            res.send(managerId + ". " + managerName);
        });

        app.get("/secret.txt", (req, res) -> {
            res.send("secret.txt");
        });

        app.get("/.*fly$", new Middleware[]{new Logger()},  (req, res) -> {
            res.send("Regex matched!");
        });

        app.get("/wi*ld",  (req, res) -> {
            res.send("Wildcard");
        });

        app.get("/index", new StaticFileHandler("./song.html"));

        /*app.get("/*", new Middleware[]{new Cors(app), new Logger(), new Logger(), new Logger()}, (req, res) -> {
            res.send("Matches all!");
        });*/

        app.get("/download", (req, res) -> {
            res.download("./song.mp4", "song.mp4");
        });

        app.get("/infiniteLoop", new Middleware[]{new Logger("This shouldn't be returning!")},  (req, res) -> {
            while(true){}
        });

        app.listen();
    }
}