import com.github.magic.core.consts.HttpMethod;
import core.models.server.HttpTest;
import org.junit.Test;
import test_utils.TestUtils;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;

import static org.junit.Assert.*;

public class UtilTest extends HttpTest {
    @Test
    public void test_get_response() {
        try{
            HttpURLConnection validConnection = TestUtils.getResponse(new URL("http://localhost:3000"), HttpMethod.GET);
            HttpURLConnection nullURL = TestUtils.getResponse(null, HttpMethod.GET);

            assertNotNull("This connection should be valid", validConnection);
            assertNull("Connection with null URl should be null", nullURL);
        } catch (IOException e){
            fail("IOException raised, details: " + e.getMessage());
        }
    }


    @Test
    public void compare_file(){
        String mockString = "{\n" +
                "  \"library\": {\n" +
                "    \"name\": \"City Library\",\n" +
                "    \"location\": \"123 Library St, Booktown\",\n" +
                "    \"books\": [\n" +
                "      {\n" +
                "        \"id\": 1,\n" +
                "        \"title\": \"The Great Gatsby\",\n" +
                "        \"author\": \"F. Scott Fitzgerald\",\n" +
                "        \"published_year\": 1925,\n" +
                "        \"genres\": [\"Fiction\", \"Classic\"]\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 2,\n" +
                "        \"title\": \"To Kill a Mockingbird\",\n" +
                "        \"author\": \"Harper Lee\",\n" +
                "        \"published_year\": 1960,\n" +
                "        \"genres\": [\"Fiction\", \"Historical\"]\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 3,\n" +
                "        \"title\": \"1984\",\n" +
                "        \"author\": \"George Orwell\",\n" +
                "        \"published_year\": 1949,\n" +
                "        \"genres\": [\"Fiction\", \"Dystopian\"]\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 4,\n" +
                "        \"title\": \"The Catcher in the Rye\",\n" +
                "        \"author\": \"J.D. Salinger\",\n" +
                "        \"published_year\": 1951,\n" +
                "        \"genres\": [\"Fiction\", \"Classic\"]\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 5,\n" +
                "        \"title\": \"Pride and Prejudice\",\n" +
                "        \"author\": \"Jane Austen\",\n" +
                "        \"published_year\": 1813,\n" +
                "        \"genres\": [\"Fiction\", \"Romance\"]\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}";

        try{
            File tempFile = File.createTempFile("testData", ".json");
            Files.writeString(tempFile.toPath(), mockString);

            assertTrue(TestUtils.compareFile(mockString, tempFile.toPath()));
            assertFalse(TestUtils.compareFile("test", tempFile.toPath()));

            assertTrue(TestUtils.compareFile(null, tempFile.toPath()));
            assertFalse(TestUtils.compareFile(mockString, null));
        } catch (IOException e){
            fail("Can't create temp file for testing");
        }
    }
}
