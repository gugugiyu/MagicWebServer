package core.models.server;

import com.github.magic.core.consts.HttpCode;
import com.github.magic.core.consts.HttpMethod;
import com.github.magic.core.models.header.Header;
import com.github.magic.core.models.header.Headers;
import org.junit.Assert;
import org.junit.Test;
import test_utils.TestUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class EncodingTest extends HttpTest {
    @Test(timeout = 5000)
    public void request_media_encoding_test() {
        try {
            Headers headers = new Headers();
            headers.add(new Header("Accept-Encoding", "deflate"));

            HttpURLConnection connection = TestUtils.getResponse(new URL(BASE_URL + "/image.png"), HttpMethod.GET);
            Assert.assertEquals("Status code should be 200", HttpCode.OK, connection.getResponseCode());

            String encoding = connection.getHeaderField("Content-Encoding");
            Assert.assertEquals("Content-Encoding should be deflate", "deflate", encoding);

        } catch (IOException e) {
            Assert.fail("Exception raised: " + e.getMessage());
        }
    }
}
