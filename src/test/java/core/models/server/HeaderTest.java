package core.models.server;

import com.github.magic.core.consts.HttpCode;
import com.github.magic.core.consts.HttpMethod;
import org.junit.Assert;
import org.junit.Test;
import test_utils.TestUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class HeaderTest extends HttpTest{
    public final static String SERVER_NAME = "MagicWebServer/1.2";

    @Test(timeout = 5000)
    public void request_get_root_path_header_test() {
        try {
            HttpURLConnection connection = TestUtils.getResponse(new URL(BASE_URL), HttpMethod.GET);
            String responseBody = TestUtils.readResponseBody(connection.getContent());

            Assert.assertEquals("Status code should be 200", HttpCode.OK, connection.getResponseCode());
            Assert.assertEquals("Content should be 'root'", "root", responseBody);

            int lengthHeader = Integer.parseInt(connection.getHeaderField("Content-Length"));
            Assert.assertEquals("Content-Length should be 4", "root".length(), lengthHeader);

            String connectionStatus = connection.getHeaderField("Connection");
            Assert.assertEquals("Connection should be keep alive", "keep-alive", connectionStatus);

            String serverName = connection.getHeaderField("Server");
            Assert.assertEquals("Server name should be " + SERVER_NAME, SERVER_NAME, serverName);

            String acceptRange = connection.getHeaderField("Accept-Ranges");
            Assert.assertEquals("Accept range should be bytes", "bytes", acceptRange);
        } catch (IOException e) {
            Assert.fail("Exception raised: " + e.getMessage());
        }
    }
}
