package com.github.magic.core.models;

import com.github.magic.core.config.Config;
import com.github.magic.core.consts.HttpMethod;
import com.github.magic.core.consts.Misc;
import com.github.magic.core.models.header.Header;
import com.github.magic.core.models.header.Headers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import static com.github.magic.core.consts.Misc.CRLF;

public class Request {
    //Currently supporting methods are: GET, POST, DELETE
    private HttpMethod method;

    //We're not supporting trailing headers
    private final Headers headers;

    private byte[] content;

    private final Socket requestSocket;

    //The part comes after the "?" symbol
    public Map<String, String> query;

    //The route parameters (starts with ":")
    public Map<String, String> params;

    private String version;

    //The request path, without the request queryParams
    private URI path;

    //For same site request, requestOrigin will typically be null, as the "Origin" header tends to be omitted.
    private URI requestOrigin;

    //Control flag to halt request parsing process when the protocol is mismatched
    private boolean isMismatched;

    /**
     * This constructor will act as a parser for the incoming data from the connection socket
     *
     * @param socket the connection socket
     * @throws IllegalArgumentException when the request failed to parse the receive data
     * @throws SocketException when the request parse being invoked again with the do-while loop after finish the first request, response cycle
     * @throws InterruptedException 
     */
    public Request(Socket socket) throws IOException, SocketException, IllegalArgumentException {
        InputStream data = socket.getInputStream();

        query = new HashMap<>();
        headers = new Headers();

        this.requestSocket = socket;


        extractRequestLine(data);
        
        if (isMismatched && Config.SHOW_ERROR) System.err.printf("[-] %s Error: Protocol mismatched\n", requestSocket.getInetAddress());

        if (!isMismatched){
            extractHeaders(data);
            extractBody(data);
        }
    }

    /**
     * Get the request line of any request, typically looks like: "GET /testPath HTTP/1.1"
     * 
     * @param iStream the input stream contain the request data
     * @throws IOException exception may raise when reading from the input stream
     */
    private void extractRequestLine(InputStream iStream) throws IOException {
        //Parse the method, version and path from the request line;
        //Index: 0        1        2
        //Data : [method] [path]   [version]
        int c = iStream.read();
        int firstSpaceIdx = 0;

        String requestLine = "";

        //While we're not at the EOF and the newline character
        while (c != -1 && c != Misc.CRLF[1]) {
            if (c != 32)
                firstSpaceIdx++;

            if (requestLine.length() == 7 && firstSpaceIdx == 0){
                if (!validHttpMethod(requestLine.substring(0, firstSpaceIdx))){
                    isMismatched = true;
                    return;
                }
            }

            requestLine += (char) c;
            c = iStream.read();
        }

        //Maximum length for the URL is set based on this article
        //https://saturncloud.io/blog/what-is-the-maximum-length-of-a-url-in-different-browsers/
        if (requestLine.length() >= 2083)
            throw new IOException("URI too long");

        String[] firstLineTokens = requestLine.split(" ");

        if (firstLineTokens.length < 3) {
            //Invalid argument when parsing request line
            throw new IOException("[-] Request line not found");
        }

        try {
            method = HttpMethod.valueOf(firstLineTokens[0].toUpperCase());
        } catch (IllegalArgumentException e) {
            //Use default GET in case we're unable to tell what request it is
            method = HttpMethod.GET;
        }

        path = URI.create(firstLineTokens[1]);

        version = firstLineTokens[2].substring(0, firstLineTokens[2].length() - 1); //Trim off the trailing "\r" from the CRLF combo
        version = version.substring(version.indexOf("/") + 1);            //Trim off the "http" or "https" part

        //Setting the queryParams
        String queryStr = path.getQuery();

        if (queryStr != null) {
            String[] queryTokens = queryStr.split("&");

            for (String token : queryTokens) {
                int delimIdx = token.indexOf("=");

                //In case of key-only param, we'll treat it as a boolean flag
                if (delimIdx == -1) {
                    query.put(token, "true");
                } else {
                    query.put(token.substring(0, delimIdx), token.substring(delimIdx + 1));
                }
            }
        }
    }

    /**
     * Gets all the headers sent from the request and insert them into the {@link #headers} field
     * 
     * @param iStream the input stream contain the request data
     * @throws IOException exception may raise when reading from the input stream
     */
    private void extractHeaders(InputStream iStream) throws IOException {
        //Parse the header pairs
        int c = iStream.read();
        String line = "";

        //While we're not at the EOF
        while (c != -1) {
            //If we're not at the newline
            if (c != CRLF[1]) {
                line += (char) c;
            } else {
                if (line.length() < 2 && (line.charAt(0) == 13)) {
                    //If we're at the empty line delimiter, then stop reading
                    break;
                }

                //Split up the line and add it into the header list
                String[] headerEntry = line.split(": ", 0);

                //Index: 0        1
                //Data : [key] :  [value]

                headers.add(new Header(headerEntry[0], headerEntry[1]));

                line = "";
            }

            c = iStream.read();
        }

        //We can resolve the hostname here if the host header is available
        String hostHeader = headers.find("Origin");

        requestOrigin = URI.create(hostHeader);
    }

    /**
     * Read and write the request's body into the {@link #content} field of this class
     * @param iStream
     */
    private void extractBody(InputStream iStream) {
        ByteArrayOutputStream returnStream = new ByteArrayOutputStream();

        try {
            //Request's body is optional, so we need to check in order not to accidentally block the parsing process
            int possibleReadWithoutBlocking = iStream.available();

            if (possibleReadWithoutBlocking > 0) {
                for (int i = 0; i < possibleReadWithoutBlocking; i++) {
                    returnStream.write(iStream.read());
                }
            }
        } catch (IOException ignored) {}

        this.content = returnStream.toByteArray();
    }

    /**
     * Get the http method of the request
     * @return The retrieved http method 
     */
    public HttpMethod getMethod() {
        return method;
    }

    public Headers getHeaders() {
        return headers;
    }

    /**
     * Get the byte array form of the parsed request body
     * @return The retrieved byte array
     */
    public byte[] bodyRaw() {
        return content;
    }
    
    public String body(Charset charset) {
        return new String(content, charset);
    }

    public Socket getRequestSocket() {
        return requestSocket;
    }

    public String getVersion() {
        return version;
    }

    public URI getPath() {
        return path;
    }

    public byte[] getContent() {
        return content;
    }

    public URI getRequestOrigin() {
        return requestOrigin;
    }

    public boolean isMismatched() {
        return isMismatched;
    }

    public static boolean validHttpMethod(String test) {

        for (HttpMethod c : HttpMethod.values()) {
            if (c.name().equals(test)) {
                return true;
            }
        }

        return false;
    }

    public void setMethod(HttpMethod method) {
        this.method = method;
    }

    public void setParams(Map<String, String> params){
        this.params = params;
    }
}
