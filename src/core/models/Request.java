package core.models;

import core.consts.HttpMethod;
import core.models.header.Header;
import core.models.header.Headers;

import java.io.*;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import static core.models.Server.CRLF;

public class Request {
    //Currently supporting methods are: GET, POST, DELETE
    public HttpMethod method;

    //We're not supporting trailing headers
    public Headers headers;

    public byte[] content;

    public Socket requestSocket;

    //The part comes after the "?" symbol
    public Map<String, String> query;
    
    //The route parameters (starts with ":")
    public Map<String, String> params;

    public String version;

    //The request path, without the request queryParams
    public URI path;

    public Request(Socket socket) throws IOException, IllegalArgumentException {
        InputStream data = socket.getInputStream();

        query = new HashMap<>();
        headers = new Headers();

        this.requestSocket = socket;
        extractData(data);
    }

    private void extractData(InputStream data) throws IOException, IllegalArgumentException {
        extractRequestLine(data);
        extractHeaders(data);
        extractBody(data);

        //And close the InputStream
        //data.close();
    }


    private void extractRequestLine(InputStream data) throws IOException, IllegalArgumentException {
        //Parse the method, version and path from the request line;
        //Index: 0        1        2
        //Data : [method] [path]   [version]

        int c = data.read();
        String requestLine = "";

        //While we're not at the EOF and the newline character
        while (c != -1 && c != CRLF[1]) {
            requestLine += (char) c;
            c = data.read();
        }

        String[] firstLineTokens = requestLine.split(" ");

        if (firstLineTokens.length < 3){
            //Invalid argument when parsing request line
            throw new IllegalArgumentException("Invalid request line");
        }

        try{
            method  = HttpMethod.valueOf(firstLineTokens[0].toUpperCase());
        }catch (IllegalArgumentException e){
            //Use default GET in case we're unable to tell what request it is
            method  = HttpMethod.GET;
        }

        path    = URI.create(firstLineTokens[1]);
        version = firstLineTokens[2].substring(0, firstLineTokens[2].length() - 1); //Trim off the trailing "\r" from the CRLF combo

        //Setting the queryParams
        String queryStr = path.getQuery();

        if (queryStr != null){
            String[] queryTokens = queryStr.split("&");

            for (String token : queryTokens){
                int delimIdx = token.indexOf("=");

                //In case of key-only param, we'll treat it as a boolean flag
                if (delimIdx == -1){
                    query.put(token, "true");
                }else{
                    query.put(token.substring(0, delimIdx), token.substring(delimIdx + 1));
                }
            }
        }

//        for (Map.Entry<String, String> entry : queryParams.entrySet()){
//            System.out.println("Key: " + entry.getKey() + ", Value: " + entry.getValue());
//        }

        System.out.println("[+] Request parsed: " + version + " "  + method + " " + path.getPath());
    }

    private void extractHeaders(InputStream data) throws IOException{
        //Parse the header pairs
        int c = data.read();
        String line = "";

        //While we're not at the EOF
        while (c != -1){
            //If we're not at the newline
            if (c != CRLF[1]){
                line += (char) c;
            }else{
                if (line.length() < 2 && (line.charAt(0) == 13)){
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

            c = data.read();
        }
    }

    private void extractBody(InputStream data){
        ByteArrayOutputStream returnStream = new ByteArrayOutputStream();

        try{
            //Http body from request side is optional, so we might need to check the input stream to see if there's any more character to read in
            int possibleReadWithoutBlocking = data.available();

            if (possibleReadWithoutBlocking > 0){
                for (int i = 0; i < possibleReadWithoutBlocking; i++){
                    returnStream.write(data.read());
                }
            }
        } catch (IOException ignored){}

        this.content = returnStream.toByteArray();
    }

    public HttpMethod getMethod() {
        return method;
    }

    public Headers getHeaders() {
        return headers;
    }

    public byte[] bodyRaw() {
        return content;
    }

    public String body(Charset charset) {
        return new String(content, charset);
    }

    public Socket getRequestSocket() {
        return requestSocket;
    }

    public Map<String, String> getQuery() {
        return query;
    }

    public String getVersion() {
        return version;
    }

    public URI getPath() {
        return path;
    }

    public void setParams(Map<String, String> params) {
        this.params = params;
    }
}
