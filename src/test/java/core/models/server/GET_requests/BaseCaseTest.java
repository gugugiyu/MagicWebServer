package core.models.server.GET_requests;

import com.github.magic.core.consts.HttpCode;
import com.github.magic.core.consts.HttpMethod;
import core.models.server.HttpTest;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import test_utils.TestUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class BaseCaseTest extends HttpTest {
    @Test(timeout = 5000)
    public void request_root_path() {
        try {
            HttpURLConnection connection = TestUtils.getResponse(new URL(BASE_URL), HttpMethod.GET);
            String responseBody = TestUtils.readResponseBody(connection.getContent());

            Assert.assertEquals("Status code should be 200", HttpCode.OK, connection.getResponseCode());
            Assert.assertEquals("Content should be 'root'", "root", responseBody);

            int lengthHeader = Integer.parseInt(connection.getHeaderField("Content-Length"));
            Assert.assertEquals("Content-Length should be 4", "root".length(), lengthHeader);

        } catch (IOException e) {
            Assert.fail("Exception raised: " + e.getMessage());
        }
    }

    @Test(timeout = 5000)
    public void request_not_found_path() {
        try {
            HttpURLConnection connection = TestUtils.getResponse(new URL(BASE_URL + "random/random2/random3"), HttpMethod.GET);

            Assert.assertEquals("Status code should be 404", HttpCode.NOT_FOUND, connection.getResponseCode());

            int lengthHeader = Integer.parseInt(connection.getHeaderField("Content-Length"));
            Assert.assertEquals("Content-Length should be 9", "Not Found".length(), lengthHeader);

        } catch (IOException e) {
            Assert.fail("Exception raised: " + e.getMessage());
        }
    }

    @Test(timeout = 5000)
    @Ignore
    public void request_with_emoji(){
        try {
            //Server doesn't support punycode encode, so it basically can't cope with emoji within URLs
            HttpURLConnection connection = TestUtils.getResponse(new URL(BASE_URL + "👏"), HttpMethod.GET);

            Assert.assertEquals("Status code should be 404", HttpCode.NOT_FOUND, connection.getResponseCode());

            int lengthHeader = Integer.parseInt(connection.getHeaderField("Content-Length"));
            Assert.assertEquals("Content-Length should be 9", "Not Found".length(), lengthHeader);

        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail("Exception raised: " + e.getMessage());
        }
    }

    @Test(timeout = 5000)
    public void request_super_long_url() {
        //TODO implement the upperbound limit for url of which the server could handle
        /* Code here */

        String testData = "acbdefjhijklmnopqrstuvwxyz";

        for (int i = 0; i < 7; i++)
            testData += testData; //Gives roughly 3300 characters, which exceed the amount of 2083 characters from the server

        try {
            //Server doesn't support punycode encode, so it basically can't cope with emoji within URLs
            HttpURLConnection connection = TestUtils.getResponse(new URL(BASE_URL + testData), HttpMethod.GET);

            Assert.assertEquals("Status code should be 414", HttpCode.URI_TOO_LONG, connection.getResponseCode());

            int lengthHeader = Integer.parseInt(connection.getHeaderField("Content-Length"));
            Assert.assertEquals("Content-Length should be 12", "URI Too long".length(), lengthHeader);

        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail("Exception raised: " + e.getMessage());
        }

    }
}
