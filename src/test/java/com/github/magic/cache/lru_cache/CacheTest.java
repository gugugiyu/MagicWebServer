package com.github.magic.cache.lru_cache;

import com.github.magic.core.consts.HttpCode;
import com.github.magic.core.consts.HttpMethod;
import com.github.magic.core.models.header.Header;
import com.github.magic.core.models.header.Headers;
import core.models.server.HttpTest;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import test_utils.TestUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class CacheTest extends HttpTest {
    @Test(timeout = 5000)
    public void multiple_requests_gives_304_Not_Modified() {
        try {
            HttpURLConnection connection = TestUtils.getResponse(new URL(BASE_URL + "image.png"), HttpMethod.GET);
            String responseBody = TestUtils.readResponseBody(connection.getContent());

            Assert.assertEquals("Status code should be 200", HttpCode.OK, connection.getResponseCode());
            //Assert.assertEquals("Content should be 'root'", "root", responseBody);

            String Etag = connection.getHeaderField("Etag");
            Assert.assertNotNull("There should be an Etag", Etag);

            connection.disconnect();

            //Recalling multiples connection, since the response is cached, we should be getting 304 Not Modified from now on
            for (int i = 0; i < 100; i++) {
                Headers headers = new Headers();
                //add the eTag
                headers.add(new Header("If-None-Match", Etag));

                connection = TestUtils.getResponse(new URL(BASE_URL + "image.png"), HttpMethod.GET, headers);

                Assert.assertEquals("Status code should be 304", HttpCode.NOT_MODIFIED, connection.getResponseCode());
                //Assert.assertEquals("Content should be 'root'", "root", responseBody);

                connection.disconnect();
            }

        } catch (IOException e) {
            Assert.fail("Exception raised: " + e.getMessage());
        }
    }
}
