package test_utils;

import com.github.magic.core.config.ServerConfig;
import com.github.magic.core.consts.HttpMethod;
import com.github.magic.core.models.header.Header;
import com.github.magic.core.models.header.Headers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

public class TestUtils {
    private static final String USER_AGENT = "Test client/1.0";
    private static final int    CONNECT_TIMEOUT_DURATION = 5000; //5s per connection
    private static final int    READ_TIMETOUT_DURATION   = new ServerConfig().getThreadRequestReadTimeoutDuration();

    /**
     * <p>Open a connection to the given url.</p>
     * <br>
     * <p>Note that this method has run the {@link HttpURLConnection#connect()} method by default</p>
     *
     * @see #readResponseBody(Object)
     *
     * @param url The url to be connected.
     * @param method The http method to be used for this request, default is GET is pass {@code null}
     * @param headers the header list to be sent
     * @return an {@link HttpURLConnection} instance of the current connection, or {@code null} if the current URL isn't valid
     * @throws IOException exception when trying to open connection to the url
     */
    public static HttpURLConnection getResponse(final URL url, HttpMethod method, final Headers headers) throws IOException {
        if (url == null) return null;

        if (method == null)
            method = HttpMethod.GET;

        HttpURLConnection con = (HttpURLConnection) url.openConnection();

        con.setRequestMethod(method.name());
        con.setRequestProperty("User-Agent", USER_AGENT);
        con.setConnectTimeout(CONNECT_TIMEOUT_DURATION);
        con.setReadTimeout(READ_TIMETOUT_DURATION);

        if (headers != null){
            for (Header header : headers){
                con.setRequestProperty(header.getKey(), header.getValue());
            }
        }

        con.connect();

        return con;
    }

    public static HttpURLConnection getResponse(final URL url, HttpMethod method) throws IOException {
        return getResponse(url, method, null);
    }

    /**
     * <p>Read the body of the response</p>
     *
     * @param con The input stream which should be retrieved through the {@link HttpURLConnection#getContent()} method
     * @return The string of content from the file. Or null if {@code con} isn't an instance of type {@link InputStream}
     * @throws IOException exception raised when reading the input stream
     */
    public static String readResponseBody(Object con) throws IOException {
        if (!(con instanceof InputStream))
            return null;

        // Read response
        BufferedReader in = new BufferedReader(new InputStreamReader((InputStream) con));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        return response.toString();
    }

    /**
     * Compare a file to a given String
     * @param expected The string as the expect value
     * @param file The path to the file to be compared
     * @return True if {@code expected} is null or the two resources matched, fail if {@code file} is null or the resources don't match
     * @throws IOException exception raised when reading the file
     */
    public static boolean compareFile(String expected, Path file) throws IOException {
        if (file == null) return false;
        if (expected == null) return true;

        String fileContent = Files.readString(file);
        return fileContent.contentEquals(expected);
    }
}
