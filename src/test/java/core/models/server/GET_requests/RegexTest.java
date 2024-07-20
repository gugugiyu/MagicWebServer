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

import static core.models.server.HttpTest.BASE_URL;

public class RegexTest extends HttpTest {
    @Test
    public void request_regex_valid() {
        try {
            HttpURLConnection connection = TestUtils.getResponse(new URL(BASE_URL + "regex/magic@gmail.com"), HttpMethod.GET);
            String responseBody = TestUtils.readResponseBody(connection.getContent());

            Assert.assertEquals("Status code should be 200", HttpCode.OK, connection.getResponseCode());
            Assert.assertEquals("Content should be 'email'", "email", responseBody);

            int lengthHeader = Integer.parseInt(connection.getHeaderField("Content-Length"));
            Assert.assertEquals("Content-Length should be 5", "email".length(), lengthHeader);

        } catch (IOException e) {
            Assert.fail("Exception raised: " + e.getMessage());
        }
    }

    @Test
    public void request_regex_invalid() {
        try {
            HttpURLConnection connection = TestUtils.getResponse(new URL(BASE_URL + "regex/magic-gmail.com"), HttpMethod.GET);

            Assert.assertEquals("Status code should be 404", HttpCode.NOT_FOUND, connection.getResponseCode());
        } catch (IOException e) {
            Assert.fail("Exception raised: " + e.getMessage());
        }
    }
}
