package core.models.server;

import core.config.Config;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;

public class ConnectionTest {
    final static int HTTP_PORT = 3000;

    private static HttpURLConnection setupConnection(String urlString) throws IOException, URISyntaxException {

        URL url = new URI(urlString).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000); // 5 seconds timeout
        connection.setReadTimeout(Config.THREAD_TIMEOUT_DURATION * 1000); // Timeout for worker thread execution set from server

        return connection;
    }

    public static boolean Should_Serve_404(String scheme, String host, int port, String resource) {
        String urlString = scheme + "://" + host + ":" + port + resource;

        try {
            HttpURLConnection connection = setupConnection(urlString);

            int responseCode = connection.getResponseCode();
            if (responseCode == 404) {
                return true;
            }
        } catch (IOException e) {
            System.err.println("Exception: " + e.getMessage());
        } catch (URISyntaxException e) {
            System.err.println("Invalid URI syntax, tester's fault");
        }
        return false;
    }

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
                    assertEquals(expectedContent, content.toString(), "Content did not match");
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

    @Test
    public void testPortServingWebServer() {
        int testPort = 3000;
        Thread serverThread = new Thread(new Server(HTTP_PORT));
        serverThread.start();

        String host = "localhost"; // Replace with your host

        boolean isServing = Should_Serve_Expected_Content("http", host, testPort, "/", null);
        assertTrue(isServing, "Port " + testPort + " should be serving a web server.");

        serverThread.interrupt();
    }

    @Test
    public void testPortNotServingWebServer() {
        int testPort = 9999;
        Thread serverThread = new Thread(new Server(HTTP_PORT));
        serverThread.start();

        String host = "localhost";

        boolean isServing = Should_Serve_Expected_Content("http", host, testPort, "/", null);
        assertFalse(isServing, "Port " + testPort + " should not be serving a web server.");

        serverThread.interrupt();
    }
}
