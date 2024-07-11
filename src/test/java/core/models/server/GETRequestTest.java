package core.models.server;

import com.github.magic.core.config.Config;
import com.github.magic.core.config.ServerConfig;
import com.github.magic.core.consts.HttpCode;
import com.github.magic.core.middleware.Cors;
import com.github.magic.core.middleware.Logger;
import com.github.magic.core.middleware.Middleware;
import com.github.magic.core.models.server.Server;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.MethodSorters;

import java.util.Timer;
import java.util.TimerTask;

import static core.models.server.ConnectionTest.SERVER_DEFAULT_TIMEOUT;

/**
 * The prerequisite to run any of these test cases here is that the {@link ConnectionTest} must be all passed. <br> <br>
 * All the routes specified in diagram below will be used for testing, and conducted under the {@code localhost} hostname
 *
 * @see <a href="../../data/routingTree.drawio">Example route diagram</a>
 */

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(JUnit4.class)
public class GETRequestTest {
    private static Thread serverThread;

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

    @AfterClass
    public static void closeServer() {
        serverThread.interrupt();
    }

    /**
     * Should only be called once, set up the only one instance of the server with predefined testing route, assign with a {@link java.util.TimerTask} to auto interrupted it
     */
    @BeforeClass
    public static void initServer() {
        Server app;

        app = new Server(ConnectionTest.HTTP_PORT);

        app.get("/.*fly$", new Middleware[]{new Logger()}, (req, res) -> {
            res.send("Regex matched!");
        });

        app.get("/wi*ld", (req, res) -> {
            res.send("Wildcard matched!");
        });

        app.get("/", new Middleware[]{new Cors(app), new Logger()}, (req, res) -> {
            res.send("root");
        });

        app.options("/", new Middleware[]{new Cors(app)}, (req, res) -> {
            res.send("");
        });

        app.get("/about", (req, res) -> {
            res.send("about");
        });


        app.get("/about/employee/:manager(^\\d+$)", (req, res) -> {
            String devId = req.params.get("manager");
            res.send("Received manager id: " + devId);
        });

        app.get("/about/employee/:manager/it_manager/:name", (req, res) -> {
            String managerId = req.params.get("manager");
            String managerName = req.params.get("name");

            res.send(managerId + ". " + managerName);
        });

        app.get("/secret.txt", (req, res) -> {
            res.send("secret text with file extension");
        });

        app.get("/infiniteLoop", (req, res) -> {
            System.out.println("sleep lambda trigger!");
            //Mocking up a test timeout
            try {
                Thread.sleep(new ServerConfig().getThreadTimeoutDuration() + 1000);
            } catch (InterruptedException ignored) {
            }
        });

        serverThread = new Thread(app);
        serverThread.start();

        Timer timer = new Timer();

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                serverThread.interrupt();
            }
        }, SERVER_DEFAULT_TIMEOUT * 1000); // 100 seconds by default
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