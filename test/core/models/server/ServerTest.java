package core.models.server;

import core.consts.HttpCode;
import core.middleware.Cors;
import core.middleware.Logger;
import core.middleware.Middleware;
import core.path_handler.StaticFileHandler;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ServerTest {

    @Test
    void root() {
        request("", "root", HttpCode.OK);
    }

    @Test
    void about() {
        request("/about", "about", HttpCode.OK);
    }

    @Test
    void secret() {
        request("/secret.txt", "secret.txt", HttpCode.OK);
    }

    @Test
    void butterfly() {
        request("/butterfly", "Regex matched!", HttpCode.OK);
    }

    @Test
    void butterflyman() {
        request("/butterflyman", "Not Found", HttpCode.NOT_FOUND);
    }

    @Test
    void aboutEmployee() {
        request("/about/employee", "Not Found", HttpCode.NOT_FOUND);
    }

    @Test
    void aboutEmployeeManager() {
        int id = 123;
        request("/about/employee/" + id, "Received manager id: " + id, HttpCode.OK);
    }


    private void request(String path, String expected, int expectedCode){
        HttpURLConnection connection = null;

        try{
            URL url= new URL("http://localhost:3000" + path);

            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(2000);

            int status = connection.getResponseCode();

            assertEquals(expectedCode, status);
            assertEquals(expected, extractData(connection.getInputStream()));
            assertNotNull(connection.getHeaderFields());

        } catch (FileNotFoundException e){
            //https://stackoverflow.com/questions/41355894/http-request-causes-java-io-filenotfoundexception
            if (expectedCode != HttpCode.NOT_FOUND)
                fail("File not found exception");
        } catch (SocketTimeoutException e){
            fail("Established connection timeout");
        } catch (EOFException e){
            fail("Unable to parse out the retrieved content");
        } catch (IOException e){
            e.printStackTrace();
            fail("Exception occurs!");
        }

        assertNotNull(connection);
        connection.disconnect();
    }

    private String extractData(InputStream in) throws IOException {
        DataInputStream dataInputStream = new DataInputStream(in);

        StringBuilder retStr = new StringBuilder();

        int avaiForRead = dataInputStream.available();

        for (int i = 0; i < avaiForRead; i++)
            retStr.append((char) dataInputStream.read());

        return retStr.toString();
    }
}