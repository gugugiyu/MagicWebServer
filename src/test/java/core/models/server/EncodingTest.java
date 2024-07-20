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
import java.nio.file.Files;
import java.nio.file.Path;

import static com.github.magic.core.config.Config.STATIC_DIR;

public class EncodingTest extends HttpTest {
    @Test
    public void request_media_deflate_encoding_test() {
        try {
            Headers headers = new Headers();
            headers.add(new Header("Accept-Encoding", "deflate"));

            HttpURLConnection connection = TestUtils.getResponse(new URL(BASE_URL + "image.png"), HttpMethod.GET, headers);
            Assert.assertEquals("Status code should be 200", HttpCode.OK, connection.getResponseCode());

            String encoding = connection.getHeaderField("Content-Encoding");
            Assert.assertEquals("Content-Encoding should be deflate", "deflate", encoding);

            long actualFileLength = Files.size(Path.of(STATIC_DIR + "/img.png"));
            long compressedFileLength = Long.parseLong(connection.getHeaderField("Content-Length"));

            Assert.assertTrue("Compress file should be smaller than actual file", compressedFileLength < actualFileLength);
        } catch (IOException e) {
            Assert.fail("Exception raised: " + e.getMessage());
        }
    }

    @Test
    public void request_media_gzip_encoding_test() {
        try {
            Headers headers = new Headers();
            headers.add(new Header("Accept-Encoding", "gzip"));

            HttpURLConnection connection = TestUtils.getResponse(new URL(BASE_URL + "image.png"), HttpMethod.GET, headers);
            Assert.assertEquals("Status code should be 200", HttpCode.OK, connection.getResponseCode());

            String encoding = connection.getHeaderField("Content-Encoding");
            Assert.assertEquals("Content-Encoding should be gzip", "gzip", encoding);

            long actualFileLength = Files.size(Path.of(STATIC_DIR + "/img.png"));
            long compressedFileLength = Long.parseLong(connection.getHeaderField("Content-Length"));

            Assert.assertTrue("Compress file should be smaller than actual file", compressedFileLength < actualFileLength);
        } catch (IOException e) {
            Assert.fail("Exception raised: " + e.getMessage());
        }
    }
}
