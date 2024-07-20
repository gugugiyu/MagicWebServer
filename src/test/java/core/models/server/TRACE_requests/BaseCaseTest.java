package core.models.server.TRACE_requests;

import com.github.magic.core.consts.HttpCode;
import com.github.magic.core.consts.HttpMethod;
import com.github.magic.core.models.header.Header;
import com.github.magic.core.models.header.Headers;
import core.models.server.HttpTest;
import org.junit.Assert;
import org.junit.Test;
import test_utils.TestUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import static core.models.server.HttpTest.BASE_URL;
import static core.models.server.HttpTest.HTTP_PORT;

public class BaseCaseTest extends HttpTest {
    @Test(timeout = 5000)
    public void request_root_path() {
        try {
            Headers headers = new Headers();

            headers.add(new Header("Host", "localhost:" + HTTP_PORT));
            headers.add(new Header("Max-Forwards", "5"));

            HttpURLConnection connection = TestUtils.getResponse(new URL(BASE_URL), HttpMethod.TRACE, headers);

            String responseBody = TestUtils.readResponseBody(connection.getContent());
            String contentType = connection.getContentType();

            Assert.assertEquals("Status code should be 200", HttpCode.OK, connection.getResponseCode());
            Assert.assertEquals("Content type should be 'message/http'", "message/http", contentType);
        } catch (IOException e) {
            Assert.fail("Exception raised: " + e.getMessage());
        }
    }
}
