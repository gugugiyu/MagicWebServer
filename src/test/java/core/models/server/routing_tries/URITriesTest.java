package core.models.server.routing_tries;

import com.github.magic.core.consts.HttpMethod;
import com.github.magic.core.models.routing_tries.URITries;
import junit.framework.TestCase;

public class URITriesTest extends TestCase {

    public void testGet() {
        URITries testTries = new URITries();

        //Simple case
        testTries.get("/", null, (req, res) -> res.send("root"));
        testTries.get("/about", null, (req, res) -> res.send("about"));

        assertEquals("The number of children in root node should be 1, since root / is has only been overwritten, not inserted", 1, testTries.getRoot().getChildren().size());

        assertEquals("/", testTries.getRoot().getToken());
        assertEquals("about", testTries.getRoot().getChildren().get(0).getToken());

        assertEquals(HttpMethod.GET, testTries.getRoot().getHttpMethod());
        assertEquals(HttpMethod.GET, testTries.getRoot().getChildren().get(0).getHttpMethod());

        //No slash at the front
        //Since this is not the wildcard path, this endpoint will be added in the front of the list
        testTries.get("noslash", null, (req, res) -> res.send("noslash"));
        assertEquals("noslash", testTries.getRoot().getChildren().get(0).getToken());
        assertEquals(HttpMethod.GET,  testTries.getRoot().getChildren().get(0).getHttpMethod());

        //More nested object
        testTries.get("/nested1/nested2/nested3", null, (req, res) -> res.send("triple nested"));
        assertEquals("nested1", testTries.getRoot().getChildren().get(0).getToken());
        assertEquals("nested2", testTries.getRoot().getChildren().get(0).getChildren().get(0).getToken());
        assertEquals("nested3", testTries.getRoot().getChildren().get(0).getChildren().get(0).getChildren().get(0).getToken());

        //With route params argument and nested
        testTries.get("/nested1/nested2/:data", null, (req, res) -> res.send(req.params.get("data")));
        assertEquals("nested2", testTries.getRoot().getChildren().get(0).getChildren().get(0).getToken());
        assertEquals(1, testTries.find(HttpMethod.GET, "/nested1/nested2/23").params().size());

        //Nested route params
        testTries.get("/manager/:id/it_manager/:name", null, (req, res) -> res.send("."));
        assertEquals(2, testTries.find(HttpMethod.GET, "/manager/3/it_manager/Luna").params().size());
        assertEquals("3", testTries.find(HttpMethod.GET, "/manager/3/it_manager/Luna").params().get("id"));
        assertEquals("Luna", testTries.find(HttpMethod.GET, "/manager/3/it_manager/Luna").params().get("name"));
        assertNull(testTries.find(HttpMethod.GET, "/manager/3/it_manager/Luna").params().get("random"));

        //Nested route params with wildcard
        testTries.get("/wild*card/manager/:id/it_manager/:name", null, (req, res) -> res.send("."));
        assertEquals(3, testTries.find(HttpMethod.GET, "/wildcard/manager/3/it_manager/Luna").params().size());
        assertEquals("3", testTries.find(HttpMethod.GET, "/wilddddcard/manager/3/it_manager/Luna").params().get("id"));
        assertEquals("Luna", testTries.find(HttpMethod.GET, "wild12ABC23card/manager/3/it_manager/Luna").params().get("name"));
        assertEquals("/wild12ABC23card/manager/3/it_manager/Luna", testTries.find(HttpMethod.GET, "wild12ABC23card/manager/3/it_manager/Luna").params().get("wildcard"));


        //Wildcard is case-sensitive
        assertNull(testTries.find(HttpMethod.GET, "/Wildcard/manager/3/it_manager/Luna").handler());
        assertNull(testTries.find(HttpMethod.GET, "/wildeard/manager/3/it_manager/Luna").handler());
    }

    public void testPost() {
    }

    public void testDelete() {
    }

    public void testPut() {
    }

    public void testOptions() {
    }

    public void testGetRoot() {
    }
}