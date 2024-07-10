package core.models.server;

import com.github.magic.core.config.Config;
import com.github.magic.core.config.ServerConfig;
import com.github.magic.core.middleware.Cors;
import com.github.magic.core.middleware.Logger;
import com.github.magic.core.middleware.Middleware;
import com.github.magic.core.models.server.Server;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.MethodSorters;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

import static org.junit.Assert.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(JUnit4.class)
public class ConnectionTest {
    final static int HTTP_PORT = 3000;
    final static int SERVER_DEFAULT_TIMEOUT = 100;

    private static HttpURLConnection setupConnection(String urlString) throws IOException, URISyntaxException {
        ServerConfig serverConfig= new ServerConfig(); //using default server config

        URL url = new URI(urlString).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000); // 5 seconds timeout
        connection.setReadTimeout((serverConfig.getThreadRequestReadTimeoutDuration() + 2) * 1000); // Timeout for worker thread execution set from server with 2 extra seconds

        return connection;
    }

    public static void Should_Serve_Expected_Code(String scheme, String host, int port, String resource, int code) {
        String urlString = scheme + "://" + host + ":" + port + resource;

        try {
            HttpURLConnection connection = setupConnection(urlString);

            int responseCode = connection.getResponseCode();

            assertEquals("Different response code!", code, responseCode);
        } catch (IOException e) {
            System.err.println("Exception: " + e.getMessage());
        } catch (URISyntaxException e) {
            System.err.println("Invalid URI syntax, tester's fault");
        }
    }

    /**
     * Helper function for retrieving content at an URL
     *
     * @param scheme          The given scheme of the server (usually {@code http} or {@code https})
     * @param host            The host of the server (only tested in {@code localhost}
     * @param port            The port (defaulted to 80 when given negative)
     * @param resource        The resource path
     * @param expectedContent Expected content to be retrieved (pass null to skip)
     * @return Whether the connection has been successfully established
     */
    public static boolean Should_Serve_Expected_Content(String scheme, String host, int port, String resource, String expectedContent) {
        String urlString = scheme + "://" + host + ":" + port + resource;

        try {
            HttpURLConnection connection = setupConnection(urlString);

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                // Print and check response if needed
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder content = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                in.close();

                if (expectedContent != null) {
                    assertEquals("Content did not match", expectedContent, content.toString());
                }

                return true;
            }
        } catch (IOException e) {
            System.err.println("Exception: " + e.getMessage());
        } catch (URISyntaxException e) {
            System.err.println("Invalid URI syntax, tester's fault");
        }
        return false;
    }

    /**
     * Should only be called once, set up the only one instance of the server with predefined testing route, assign with a {@link java.util.TimerTask} to auto interrupted it
     */
    @BeforeClass
    public static void initServer() {
        Server app = new Server(HTTP_PORT);

        app.get("/.*fly$", new Middleware[]{new Logger()}, (req, res) -> {
            res.send("Regex matched!");
        });

        app.get("/wi*ld", (req, res) -> {
            res.send("Wildcard matched!");
        });

        app.get("/", new Middleware[]{new Cors(app), new Logger()}, (req, res) -> {
            res.send("root");
        });

        app.options("/", new Middleware[]{new Cors(app)}, (req, res) -> {
            res.send("");
        });

        app.get("/about", (req, res) -> {
            res.send("about");
        });


        app.get("/about/employee/:manager(^\\d+$)", (req, res) -> {
            String devId = req.params.get("manager");
            res.send("Received manager id: " + devId);
        });

        app.get("/about/employee/:manager/it_manager/:name", (req, res) -> {
            String managerId = req.params.get("manager");
            String managerName = req.params.get("name");

            res.send(managerId + ". " + managerName);
        });

        app.get("/secret.txt", (req, res) -> {
            res.send("secret text with file extension");
        });

        app.get("/infiniteLoop", (req, res) -> {
            while (true) {
            }
        });

        Thread serverThread = new Thread(app);
        serverThread.start();

        Timer timer = new Timer();

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                serverThread.interrupt();
            }
        }, SERVER_DEFAULT_TIMEOUT * 1000); // 100 seconds by default
    }

    @Test
    public void testPortServingWebServer() {
        int testPort = 3000;

        String host = "localhost"; // Replace with your host

        boolean isServing = Should_Serve_Expected_Content("http", host, testPort, "/", null);
        assertTrue("Port " + testPort + " should be serving a web server.", isServing);
    }

    @Test
    public void testPortNotServingWebServer() {
        int testPort = 9999;

        String host = "localhost";

        boolean isServing = Should_Serve_Expected_Content("http", host, testPort, "/", null);
        assertFalse("Port " + testPort + " should not be serving a web server.", isServing);
    }
}
