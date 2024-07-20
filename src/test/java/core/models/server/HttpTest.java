package core.models.server;

import com.github.magic.core.models.server.Server;
import com.github.magic.core.path_handler.StaticFileHandler;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@FixMethodOrder()
@RunWith(JUnit4.class)
public class HttpTest{
    final static int HTTP_PORT = 3000;

    private static Thread serverThread;
    private static boolean isServerStarted = false; //This is to prevent the server being boot up multiple times
    public static final String BASE_URL = "http://localhost:" + HTTP_PORT + "/";

    @BeforeClass
    public static void initServer(){
        if (isServerStarted) return;

        isServerStarted = true;

        Server app = new Server(HTTP_PORT);

        //Root path
        app.get("/", (req, res) -> {
            res.send("root");
        });

        //Route parameter path
        app.get("/:data", (req, res) -> {
            String data = req.params.get("data");
            res.send(data);

            System.out.println("Route parameter case triggered");
        });

        //Nested route parameter path
        app.get("/:data/:data2", (req, res) -> {
            String data1 = req.params.get("data");
            String data2 = req.params.get("data2");
            res.send(data1 + " " + data2);

            System.out.println("Nested parameter case triggered");
        });

        //Nested route parameter path with regex
        //This regex will match if the following conditions hold

        //The string must start with letter 'w'
        //The string have at least 1 '@' symbol
        //The string must have length of 6

        app.get("/test/:data3(^w[@\\w]{4}[@\\w]$)", (req, res) -> {
            String data3 = req.params.get("data3");
            res.send(data3);

            System.out.println("Nested parameter case with regex triggered");
        });

        //Wildcard case
        //Returns the path that matches from the wildcard token
        app.get("/wild*card", (req, res) -> {
            String wildcardPath = req.params.get("wildcard");
            res.send(wildcardPath);

            System.out.println("Wildcard case triggered");
        });

        //Wildcard case, but covers all possible sub-route
        app.get("/wild/card/*", (req, res) -> {
            String wildcardPath = req.params.get("wildcard");
            res.send(wildcardPath);
        });

        //Regex case, simple email matching regex
        //Copy from https://regexr.com/3e48o
        app.get("/regex/^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$", (req, res) -> {
            res.send("email");

            System.out.println("Regex case with regex triggered");
        });

        app.get("/infiniteLoop", (req, res) -> {
            while (true) {
            }
        });

        //For testing encoding
        app.get("/image.png", new StaticFileHandler("img.png"));

        serverThread = new Thread(app);
        serverThread.setDaemon(true);
        serverThread.start();
    }

    @Test
    public void auto(){
        Assert.assertTrue(true);
    }

    @AfterClass
    public static void close(){
        serverThread.interrupt();
    }
}
