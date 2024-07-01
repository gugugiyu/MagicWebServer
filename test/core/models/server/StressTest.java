package core.models.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

public class StressTest {

    private static final int THREAD_COUNT = 50;
    private static final int REQUEST_COUNT = 100000;
    private static final String TEST_URL = "http://localhost:" + ConnectionTest.HTTP_PORT + "/";
    private static final String EXPECTED_CONTENT = "root";

    @Test
    public void testConcurrentRequests() throws InterruptedException {
        //SERVER INITIALIZATION
        Server app = new Server(ConnectionTest.HTTP_PORT);

        app.get(TEST_URL, (req, res) -> {
            res.send(EXPECTED_CONTENT);
        });

        Thread serverThread = new Thread(app);
        serverThread.start();

        //STRESS TEST

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

        for (int i = 0; i < REQUEST_COUNT; i++) {
            executor.submit(() -> {
                try {
                    URL url = new URL(TEST_URL);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    int responseCode = connection.getResponseCode();

                    assertEquals(200, responseCode, "Response code should be 200");

                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String inputLine;
                    StringBuilder content = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        content.append(inputLine);
                    }
                    in.close();

                    assertEquals(EXPECTED_CONTENT, content.toString(), "Content should be 'root'");

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

