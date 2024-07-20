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

public class RouteParametersTest extends HttpTest {
    @Test
    public void request_route_parameter() {
        try {
            HttpURLConnection connection = TestUtils.getResponse(new URL(BASE_URL + "testData"), HttpMethod.GET);
            String responseBody = TestUtils.readResponseBody(connection.getContent());

            Assert.assertEquals("Status code should be 200", HttpCode.OK, connection.getResponseCode());
            Assert.assertEquals("Content should be 'testData'", "testData", responseBody);

            int lengthHeader = Integer.parseInt(connection.getHeaderField("Content-Length"));
            Assert.assertEquals("Content-Length should be 8", "testData".length(), lengthHeader);

        } catch (IOException e) {
            Assert.fail("Exception raised: " + e.getMessage());
        }
    }

    @Test
    public void request_nested_route_parameter() {
        try {
            HttpURLConnection connection = TestUtils.getResponse(new URL(BASE_URL + "testData1/testData2"), HttpMethod.GET);
            String responseBody = TestUtils.readResponseBody(connection.getContent());

            Assert.assertEquals("Status code should be 200", HttpCode.OK, connection.getResponseCode());
            Assert.assertEquals("Content should be 'testData1 testData2'", "testData1 testData2", responseBody);

            int lengthHeader = Integer.parseInt(connection.getHeaderField("Content-Length"));
            Assert.assertEquals("Content-Length should be 19", "testData1 testData2".length(), lengthHeader);

        } catch (IOException e) {
            Assert.fail("Exception raised: " + e.getMessage());
        }
    }

    @Test
    public void request_nested_route_parameter_with_regex_valid_case() {
        try {
            //Check the 'HttpTest.java' file for more infos about the regex
            HttpURLConnection connection = TestUtils.getResponse(new URL(BASE_URL + "test/why@12"), HttpMethod.GET);
            String responseBody = TestUtils.readResponseBody(connection.getContent());

            Assert.assertEquals("Status code should be 200", HttpCode.OK, connection.getResponseCode());
            Assert.assertEquals("Content should be 'why@12'", "why@12", responseBody);

            int lengthHeader = Integer.parseInt(connection.getHeaderField("Content-Length"));
            Assert.assertEquals("Content-Length should be 6", "why@12".length(), lengthHeader);

        } catch (IOException e) {
            Assert.fail("Exception raised: " + e.getMessage());
        }
    }

    @Test
    public void request_nested_route_parameter_with_regex_invalid_case() {
        try {
            //Check the 'HttpTest.java' file for more infos about the regex
            HttpURLConnection connection = TestUtils.getResponse(new URL(BASE_URL + "test/hhy@1342"), HttpMethod.GET);

            Assert.assertEquals("Status code should be 404", HttpCode.NOT_FOUND, connection.getResponseCode());

        } catch (IOException e) {
            Assert.fail("Exception raised: " + e.getMessage());
        }
    }
}
