package core.models.server;


import com.github.magic.core.models.server.Server;
import com.github.magic.core.path_handler.StaticFileHandler;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


public class StressTest {

    private static final int THREAD_COUNT = 30;
    private static final int REQUEST_COUNT = 300000;
    private static final String TEST_URL = "http://localhost:" + ConnectionTest.HTTP_PORT + "/";
    private static final String EXPECTED_CONTENT = "root";

    @Test
    public void testConcurrentRequests() throws InterruptedException {
        //SERVER INITIALIZATION
        Server app = new Server(ConnectionTest.HTTP_PORT);

        app.get("/", (req, res) -> {
            res.send("root");
        });

        Thread serverThread = new Thread(app);
        serverThread.start();

        //STRESS TEST

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

        for (int i = 0; i < REQUEST_COUNT; i++) {
            executor.execute(() -> {
                try {
                    URL url = new URL(TEST_URL);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setRequestProperty("Connection", "close");
                    int responseCode = connection.getResponseCode();

                    assertEquals("Response code should be 200", 200, responseCode);

                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String inputLine;
                    StringBuilder content = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        content.append(inputLine);
                    }

                    in.close();
                    assertEquals("Content should be 'root'", EXPECTED_CONTENT, content.toString());

                } catch (Exception e) {
                    fail("Exception raised: " + e.getMessage());
                }
            });
        }

        System.out.println("All passed. Await termination...");

        executor.shutdown();
        executor.awaitTermination(3, TimeUnit.SECONDS);
        serverThread.interrupt();
    }
}

