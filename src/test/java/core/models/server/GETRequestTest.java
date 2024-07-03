package core.models.server;

import core.consts.HttpCode;
import org.junit.Test;

/**
 * The prerequisite to run any of these test cases here is that the {@link ConnectionTest} must be all passed. <br> <br>
 * All the routes specified in diagram below will be used for testing, and conducted under the {@code localhost} hostname
 *
 * @see <a href="../../data/routingTree.drawio">Example route diagram</a>
 */
public class GETRequestTest {
    @Test
    public void testGET_root() {
        final String TEST_URL = "/";
        final String EXPECTED_CONTENT = "root";

        //Please create and call the method to fetch at the endpoint here
        ConnectionTest.Should_Serve_Expected_Content(
                "http",
                "localhost",
                ConnectionTest.HTTP_PORT,
                TEST_URL,
                EXPECTED_CONTENT
        );
    }

    @Test
    public void testGET_about() {
        final String TEST_URL = "/about";
        final String EXPECTED_CONTENT = "about";

        //Please create and call the method to fetch at the endpoint here
        ConnectionTest.Should_Serve_Expected_Content(
                "http",
                "localhost",
                ConnectionTest.HTTP_PORT,
                TEST_URL,
                EXPECTED_CONTENT
        );
    }

    @Test
    public void testGET_secret_txt() {
        final String TEST_URL = "/secret.txt";
        final String EXPECTED_CONTENT = "secret text with file extension";

        //Please create and call the method to fetch at the endpoint here
        ConnectionTest.Should_Serve_Expected_Content(
                "http",
                "localhost",
                ConnectionTest.HTTP_PORT,
                TEST_URL,
                EXPECTED_CONTENT
        );
    }

    @Test
    public void testGET_regexPass() {
        // Borrowed from expressJs routing case
        // https://expressjs.com/en/guide/routing.html

        final String TEST_URL = "/butterfly";
        final String REGEX = ".*fly$";
        final String EXPECTED_CONTENT = "Regex matched!";

        //Please create and call the method to fetch at the endpoint here
        ConnectionTest.Should_Serve_Expected_Content(
                "http",
                "localhost",
                ConnectionTest.HTTP_PORT,
                TEST_URL,
                EXPECTED_CONTENT
        );
    }

    @Test
    public void testGET_regexFailNotFound() {
        // Borrowed from expressJs routing case
        // https://expressjs.com/en/guide/routing.html

        final String TEST_URL = "/butterflyman";
        final String REGEX = ".*fly$";
        final String EXPECTED_CONTENT = "Not found";

        //Please create and call the method to fetch at the endpoint here
        ConnectionTest.Should_Serve_Expected_Code(
                "http",
                "localhost",
                ConnectionTest.HTTP_PORT,
                TEST_URL,
                HttpCode.NOT_FOUND
        );
    }

    @Test
    public void testGET_wildcardPass() {
        final String TEST_URL = "/wiiiii123iiild";
        final String EXPECTED_CONTENT = "Wildcard matched!";

        //Please create and call the method to fetch at the endpoint here
        ConnectionTest.Should_Serve_Expected_Content(
                "http",
                "localhost",
                ConnectionTest.HTTP_PORT,
                TEST_URL,
                EXPECTED_CONTENT
        );
    }

    @Test
    public void testGET_wildcardFailNotFound() {
        //Case sensitive, so the first "W" isn't the same as "w"
        final String TEST_URL = "/Wiiiiii123iiild";
        final String WILDCARD = "wi*ld";
        final String EXPECTED_CONTENT = "Not Found";

        //Please create and call the method to fetch at the endpoint here
        ConnectionTest.Should_Serve_Expected_Code(
                "http",
                "localhost",
                ConnectionTest.HTTP_PORT,
                TEST_URL,
                HttpCode.NOT_FOUND
        );
    }

    @Test
    public void testGET_routeParamaters() {
        final int EMPLOYEE_ID = 12;
        final String TEST_URL = "/about/employee/" + EMPLOYEE_ID;
        final String EXPECTED_CONTENT = "Received manager id: " + EMPLOYEE_ID;

        //Please create and call the method to fetch at the endpoint here
        ConnectionTest.Should_Serve_Expected_Content(
                "http",
                "localhost",
                ConnectionTest.HTTP_PORT,
                TEST_URL,
                EXPECTED_CONTENT
        );
    }


    @Test
    public void testGET_routeParamatersWithRegex() {
        int EMPLOYEE_ID = 12;
        String TEST_URL = "/about/employee/" + EMPLOYEE_ID;
        String EXPECTED_CONTENT = "Received manager id: " + EMPLOYEE_ID;

        //Please create and call the method to fetch at the endpoint here
        ConnectionTest.Should_Serve_Expected_Content(
                "http",
                "localhost",
                ConnectionTest.HTTP_PORT,
                TEST_URL,
                EXPECTED_CONTENT
        );

        EMPLOYEE_ID = -12; //fail this case
        TEST_URL = "/about/employee/" + EMPLOYEE_ID;

        ConnectionTest.Should_Serve_Expected_Code(
                "http",
                "localhost",
                ConnectionTest.HTTP_PORT,
                TEST_URL,
                HttpCode.NOT_FOUND
        );
    }

    @Test
    public void testGET_infinityLoop() {
        //Mock up an freezing inside the worker thread execution, thus force the server to time out the request
        final String TEST_URL = "/infiniteLoop";

        //Please create and call the method to fetch at the endpoint here
        ConnectionTest.Should_Serve_Expected_Code(
                "http",
                "localhost",
                ConnectionTest.HTTP_PORT,
                TEST_URL,
                HttpCode.INTERNAL_SERVER_ERROR
        );
    }
}