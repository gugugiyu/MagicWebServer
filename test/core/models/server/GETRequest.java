package core.models.server;

import core.consts.HttpCode;
import org.junit.jupiter.api.Test;

/**
 * The prerequisite to run any of these test cases here is that the {@link ConnectionTest} must be all passed. <br> <br>
 * All the routes specified in diagram below will be used for testing, and conducted under the {@code localhost} hostname
 *
 * @see <a href="../../data/routingTree.drawio">Example route diagram</a>
 */
class GETRequest {
    @Test
    void root() {
        final String TEST_URL = "/";
        final String EXPECTED_CONTENT = "root";

        //GET request
        Server app = new Server(ConnectionTest.HTTP_PORT);

        app.get(TEST_URL, (req, res) -> {
            res.send(EXPECTED_CONTENT);
        });

        Thread serverThread = new Thread(app);
        serverThread.start();

        //Please create and call the method to fetch at the endpoint here
        ConnectionTest.Should_Serve_Expected_Content(
                "http",
                "localhost",
                ConnectionTest.HTTP_PORT,
                TEST_URL,
                EXPECTED_CONTENT
        );

        serverThread.interrupt();
    }

    @Test
    void about() {
        final String TEST_URL = "/about";
        final String EXPECTED_CONTENT = "about";

        //GET request
        Server app = new Server(ConnectionTest.HTTP_PORT);

        app.get(TEST_URL, (req, res) -> {
            res.send(EXPECTED_CONTENT);
        });

        Thread serverThread = new Thread(app);
        serverThread.start();

        //Please create and call the method to fetch at the endpoint here
        ConnectionTest.Should_Serve_Expected_Content(
                "http",
                "localhost",
                ConnectionTest.HTTP_PORT,
                TEST_URL,
                EXPECTED_CONTENT
        );

        serverThread.interrupt();
    }

    @Test
    void secret_txt() {
        final String TEST_URL = "/secret.txt";
        final String EXPECTED_CONTENT = "secret text with file extension";

        //GET request
        Server app = new Server(ConnectionTest.HTTP_PORT);

        app.get(TEST_URL, (req, res) -> {
            res.send(EXPECTED_CONTENT);
        });

        Thread serverThread = new Thread(app);
        serverThread.start();

        //Please create and call the method to fetch at the endpoint here
        ConnectionTest.Should_Serve_Expected_Content(
                "http",
                "localhost",
                ConnectionTest.HTTP_PORT,
                TEST_URL,
                EXPECTED_CONTENT
        );

        serverThread.interrupt();
    }

    @Test
    void regexPass() {
        // Borrowed from expressJs routing case
        // https://expressjs.com/en/guide/routing.html

        final String TEST_URL = "/butterfly";
        final String REGEX = ".*fly$";
        final String EXPECTED_CONTENT = "Regex matched!";

        //GET request
        Server app = new Server(ConnectionTest.HTTP_PORT);

        app.get(REGEX, (req, res) -> {
            res.send("Regex matched!");
        });

        Thread serverThread = new Thread(app);
        serverThread.start();

        //Please create and call the method to fetch at the endpoint here
        ConnectionTest.Should_Serve_Expected_Content(
                "http",
                "localhost",
                ConnectionTest.HTTP_PORT,
                TEST_URL,
                EXPECTED_CONTENT
        );

        serverThread.interrupt();
    }

    @Test
    void regexFailNotFound() {
        // Borrowed from expressJs routing case
        // https://expressjs.com/en/guide/routing.html

        final String TEST_URL = "/butterflyman";
        final String REGEX = ".*fly$";
        final String EXPECTED_CONTENT = "Not found";

        //GET request
        Server app = new Server(ConnectionTest.HTTP_PORT);

        app.get(REGEX, (req, res) -> {
            res.send("Regex matched!");
        });

        Thread serverThread = new Thread(app);
        serverThread.start();

        //Please create and call the method to fetch at the endpoint here
        ConnectionTest.Should_Serve_Expected_Code(
                "http",
                "localhost",
                ConnectionTest.HTTP_PORT,
                TEST_URL,
                HttpCode.NOT_FOUND
        );

        serverThread.interrupt();
    }

    @Test
    void wildcardPass() {
        final String TEST_URL = "/wiiiii123iiild";
        final String WILDCARD = "wi*ld";
        final String EXPECTED_CONTENT = "Wildcard matched!";

        //GET request
        Server app = new Server(ConnectionTest.HTTP_PORT);

        app.get(WILDCARD, (req, res) -> {
            res.send("Wildcard matched!");
        });

        Thread serverThread = new Thread(app);
        serverThread.start();

        //Please create and call the method to fetch at the endpoint here
        ConnectionTest.Should_Serve_Expected_Content(
                "http",
                "localhost",
                ConnectionTest.HTTP_PORT,
                TEST_URL,
                EXPECTED_CONTENT
        );

        serverThread.interrupt();
    }

    @Test
    void wildcardFailNotFound() {
        //Case sensitive, so the first "W" isn't the same as "w"
        final String TEST_URL = "/Wiiiiii123iiild";
        final String WILDCARD = "wi*ld";
        final String EXPECTED_CONTENT = "Not Found";

        //GET request
        Server app = new Server(ConnectionTest.HTTP_PORT);

        app.get(WILDCARD, (req, res) -> {
            res.send("Wildcard matched!");
        });

        Thread serverThread = new Thread(app);
        serverThread.start();

        //Please create and call the method to fetch at the endpoint here
        ConnectionTest.Should_Serve_Expected_Code(
                "http",
                "localhost",
                ConnectionTest.HTTP_PORT,
                TEST_URL,
                HttpCode.NOT_FOUND
        );

        serverThread.interrupt();
    }

    @Test
    void routeParamaters() {
        final int EMPLOYEE_ID = 12;
        final String TEST_URL = "/about/employee/" + EMPLOYEE_ID;
        final String EXPECTED_CONTENT = "Received employee id: " + EMPLOYEE_ID;

        //GET request
        Server app = new Server(ConnectionTest.HTTP_PORT);

        app.get("/about/employee/:developer", (req, res) -> {
            res.send("Received employee id: " + req.params.get("developer"));
        });

        Thread serverThread = new Thread(app);
        serverThread.start();

        //Please create and call the method to fetch at the endpoint here
        ConnectionTest.Should_Serve_Expected_Content(
                "http",
                "localhost",
                ConnectionTest.HTTP_PORT,
                TEST_URL,
                EXPECTED_CONTENT
        );

        serverThread.interrupt();
    }


    @Test
    void routeParamatersWithRegex() {
        int EMPLOYEE_ID = 12;
        String TEST_URL = "/about/employee/" + EMPLOYEE_ID;
        String EXPECTED_CONTENT = "Received employee id: " + EMPLOYEE_ID;

        //GET request
        Server app = new Server(ConnectionTest.HTTP_PORT);

        //Matching only positive employee ID
        app.get("/about/employee/:developer(^\\d+$)", (req, res) -> {
            res.send("Received employee id: " + req.params.get("developer"));
        });

        Thread serverThread = new Thread(app);
        serverThread.start();

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

        serverThread.interrupt();
    }

    @Test
    void infinityLoop(){
        //Mock up an freezing inside the worker thread execution, thus force the server to time out the request
        final String TEST_URL = "/infiniteLoop";

        //GET request
        Server app = new Server(ConnectionTest.HTTP_PORT);

        app.get(TEST_URL, (req, res) -> {
            while(true){}
        });

        Thread serverThread = new Thread(app);
        serverThread.start();

        //Please create and call the method to fetch at the endpoint here
        ConnectionTest.Should_Serve_Expected_Code(
                "http",
                "localhost",
                ConnectionTest.HTTP_PORT,
                TEST_URL,
                HttpCode.INTERNAL_SERVER_ERROR
        );

        serverThread.interrupt();
    }
}