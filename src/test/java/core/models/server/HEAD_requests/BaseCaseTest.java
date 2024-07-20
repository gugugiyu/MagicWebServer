package core.models.server.HEAD_requests;

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

public class BaseCaseTest extends HttpTest {
    @Test
    public void should_have_no_content_request() {
        try {
            HttpURLConnection connection = TestUtils.getResponse(new URL(BASE_URL), HttpMethod.HEAD);

            String contentType = connection.getContentType();
            int contentLength = connection.getContentLength();

            Assert.assertEquals("Status code should be 200", HttpCode.OK, connection.getResponseCode());
            Assert.assertTrue("There should be no content length", contentLength < 0);
            Assert.assertNull("There should be no content type", contentType);
        } catch (IOException e) {
            Assert.fail("Exception raised: " + e);
        }
    }
}
