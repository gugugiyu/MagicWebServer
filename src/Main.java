import core.consts.HttpCode;
import core.models.Server;
import core.path_handler.StaticFileHandler;

public class Main {
    public static void main(String[] args) {
        Server app = new Server();
        //This route path will match requests to the root route, /.
        app.get("/", (req, res) -> {
            res.send("root", HttpCode.OK);
        });

        //This route path will match requests to /about.
        app.get("/about", (req, res) -> {
            res.send("about", HttpCode.OK);
        });

        //This route path will match requests to /random.text.
        app.get("/random.text", (req, res) -> {
            res.send("random.text", HttpCode.OK);
        });

        //This route path will match acd and abcd.
        app.get("/ab?cd", (req, res) -> {
            res.send("ab?cd", HttpCode.OK);
        });

        //This route path will match abcd, abbcd, abbbcd, and so on.
        app.get("/ab+cd", (req, res) -> {
            res.send("ab+cd", HttpCode.OK);
        });

        //This route path will match abcd, abxcd, abRANDOMcd, ab123cd, and so on.
        app.get("/ab*cd", (req, res) -> {
            res.send("ab*cd", HttpCode.OK);
        });

        //This route path will match butterfly and dragonfly, but not butterflyman, dragonflyman, and so on.
        app.get("/.*fly$", (req, res) -> {
            res.send("/.*fly$", HttpCode.OK);
        });

        //To define routes with route parameters, simply specify the route parameters in the path of the route as shown below.
        app.get("/users/:userId/books/:bookId", (req, res) -> {
            String userId = req.params.get("userId");
            String bookId = req.params.get("bookId");

            System.out.println("UserId: " + userId);
            System.out.println("BookId: " + bookId);

            res.send("Route parameter test!", HttpCode.OK);
        });

        //Example of route with parameter
        app.get("/users/:userId(\\d+)", (req, res) -> {
            String userId = req.params.get("userId");

            System.out.println("UserId: " + userId);

            res.send("Route parameter with regex test!", HttpCode.OK);
        });

        //Built-in static file server
        app.get("/index", new StaticFileHandler("./index.html"));

        app.listen(3000);
    }
}