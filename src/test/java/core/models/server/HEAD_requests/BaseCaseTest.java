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
    public void request_root_path() {
        try {
            HttpURLConnection connection = TestUtils.getResponse(new URL(BASE_URL), HttpMethod.HEAD);
            String responseBody = TestUtils.readResponseBody(connection.getContent());

            Assert.assertEquals("Status code should be 200", HttpCode.OK, connection.getResponseCode());
            Assert.assertEquals("There should be no content", "", responseBody);
        } catch (IOException e) {
            Assert.fail("Exception raised: " + e);
        }
    }
}
