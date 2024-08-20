package core;

import core.models.server.HttpTest;
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


public class StressTest extends HttpTest {

    private static final int THREAD_COUNT = 15; // Number of concurrent threads
    private static final int REQUEST_COUNT = 5000; // Total number of requests

    @Test
    public void stressTest() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);

        for (int i = 0; i < REQUEST_COUNT; i++) {
            executorService.execute(new HttpTask("http://localhost:3000"));
        }

        executorService.shutdown();

        if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
            executorService.shutdownNow();
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                fail("Executor service did not terminate");
            }
        }
    }

    static class HttpTask implements Runnable {
        private final String url;

        public HttpTask(String url) {
            this.url = url;
        }

        @Override
        public void run() {
            try {
                URL urlObj = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();
                connection.setRequestMethod("GET");

                int responseCode = connection.getResponseCode();
                assertEquals("Response Code: " + responseCode, HttpURLConnection.HTTP_OK, responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String inputLine;
                    StringBuilder response = new StringBuilder();

                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();
                } else {
                    fail("GET request failed with response code: " + responseCode);
                }
            } catch (Exception e) {
                fail("Exception occurred: " + e.getMessage());
            }
        }
    }
}
