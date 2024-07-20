package core.models.server.GET_requests;

import com.github.magic.core.consts.HttpCode;
import com.github.magic.core.consts.HttpMethod;
import core.models.server.HttpTest;
import org.junit.Assert;
import org.junit.Test;
import test_utils.TestUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class WildcardTest extends HttpTest {
    @Test
    public void request_wildcard_valid() {
        try {
            HttpURLConnection connection = TestUtils.getResponse(new URL(BASE_URL + "wildAAAAAAAAAcard"), HttpMethod.GET);
            String responseBody = TestUtils.readResponseBody(connection.getContent());

            Assert.assertEquals("Status code should be 200", HttpCode.OK, connection.getResponseCode());
            Assert.assertEquals("Content should be '/wildAAAAAAAAAcard'", "/wildAAAAAAAAAcard", responseBody);

            int lengthHeader = Integer.parseInt(connection.getHeaderField("Content-Length"));
            Assert.assertEquals("Content-Length should be 18", "/wildAAAAAAAAAcard".length(), lengthHeader);

        } catch (IOException e) {
            Assert.fail("Exception raised: " + e.getMessage());
        }
    }

    @Test
    public void request_wildcard_invalid() {
        try {
            HttpURLConnection connection = TestUtils.getResponse(new URL(BASE_URL + "Wildcard"), HttpMethod.GET);

            Assert.assertEquals("Status code should be 404", HttpCode.OK, connection.getResponseCode());
        } catch (IOException e) {
            Assert.fail("Exception raised: " + e.getMessage());
        }
    }

    @Test
    public void request_wildcard_cover_all_subroute() {
        try {
            HttpURLConnection connection = TestUtils.getResponse(new URL(BASE_URL + "wild/card/test/test1/test2"), HttpMethod.GET);
            String responseBody = TestUtils.readResponseBody(connection.getContent());

            Assert.assertEquals("Status code should be 200", HttpCode.OK, connection.getResponseCode());
            Assert.assertEquals("Content should be '/test/test1/test2'", "/test/test1/test2", responseBody);

            int lengthHeader = Integer.parseInt(connection.getHeaderField("Content-Length"));
            Assert.assertEquals("Content-Length should be 17", "/test/test1/test2".length(), lengthHeader);

        } catch (IOException e) {
            Assert.fail("Exception raised: " + e.getMessage());
        }
    }
}
