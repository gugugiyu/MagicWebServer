package core.models;

import core.consts.HttpCode;
import core.encoder.Encoder;
import core.encoder.EncoderFactory;
import core.models.header.Header;
import core.models.header.Headers;
import core.utils.FileAttributeRetriever;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import static core.models.Server.CRLF;
import static core.consts.HttpDes.statuses;

public class Response implements Closeable{
    //The associated request for this response, also can be use for checking client's compatability
    protected Request request;

    protected OutputStream oStream;

    protected Headers headers;

    //This flag could be figured out from the request's header
    //More specifically, the "Accept-Encoding" header
    protected boolean isEncodedSupported;

    //This flag tells if the response need to be compressed or not
    protected boolean isEncoding;

    //This flag tells if the underlying connection (socket connection) should be closed after this request-response cycle
    protected boolean isClosed;

    protected Encoder encoder;

    //This flag tells if the response is sent back to the client
    protected boolean isSent;

    public boolean isSent() {
        return isSent;
    }

    public Response(Request request, boolean forceClose) throws IOException {
        this.request = request;
        this.isEncodedSupported = isEncodedSupported();
        this.isEncoding = shouldEncode();
        this.oStream = request.getRequestSocket().getOutputStream();
        this.headers = new Headers();
        this.isSent = false;

        //If the handler force this connection to close (in case of time out request), then let it be
        if (forceClose){
            this.isClosed = true;
        }else{
            this.isClosed = isConnectionClosed();
        }
    }

    private boolean isConnectionClosed(){
        Header connectionHeader = request.headers.find("Connection");
        return connectionHeader != null && connectionHeader.getValue().equalsIgnoreCase("close");
    }

    private boolean isEncodedSupported(){
        Headers headers = request.headers;

        //Check if the user request could handle encoding response
        Header status = headers.find("Accept-Encoding");

        return (status != null && !status.getValue().isEmpty());
    }

    private boolean shouldEncode(){
        //In case this isn't a static file request, we can't possibly determine what file type it is
        //We should probably encode it
        String path = request.path.getPath();

        //We can check the part after the last "/" to see if there's any "." (which signifies a file extension)
        int lastSlashIdx = path.lastIndexOf("/");

        if (!path.substring(lastSlashIdx).contains(".")){
            return true;
        }

        //Handles logic for whether we should encode this http response
        //For now, let say that for file types that are already compressed, we won't compress it anymore
        final String[] COMPRESSED_DATA_TYPE = {
                "png",
                "jpg",
                "jpeg",
                "gif",
                "webp",
        };

        for (String string : COMPRESSED_DATA_TYPE){
            if (path.contains(string)){
                return false;
            }
        }

        return true;
    }

    public void send(byte[] text, int length, String mimeType, int status) throws IOException {
        byte[] content = sendHeaders(text, length, mimeType, status);

        oStream.flush();
        oStream.write(content);

        //Set the sent flag
        isSent = true;

        close();
    }

    /*
     * Handler redirect version
     */
    public void redirect(String url) throws IOException {
        redirect(url, true);
    }

    /* Borrowed from jhttp */
    public void redirect(String url, boolean permanent) throws IOException {
        try {
            url = new URI(url).toASCIIString();
        } catch (URISyntaxException e) {
            throw new IOException("Malformed URL " + url);
        }

        setHeader("Location", url);

        // some user-agents expect a body, so we send it
        if (permanent)
            sendError(HttpCode.MOVED_PERMANENTLY);
        else
            sendError(HttpCode.TEMPORARY_REDIRECT);
    }

    public void send(String text, int status) throws IOException {
        send(text.trim().getBytes(), -1, "text/plain", status);
    }

    //TODO: work on this sendFile function
    /*public void sendFile(File file, int status, boolean encode) throws IOException {
        this.isEncoding = encode;

        if (!file.exists()){
            throw new FileNotFoundException("[-] File not found");
        }

        if (file.isDirectory() && file.isHidden() && !file.canRead()){
            throw new InvalidObjectException("[-] File is either a directory, is hidden or no permission to read");
        }

        try(FileInputStream iStream = new FileInputStream(file)){
            int bufferSize = 1 << 15;
            byte[] arr = new byte[bufferSize];

            int byteRead = iStream.read(arr);
            send(arr, byteRead, new FileAttributeRetriever(file).getMimeType(), HttpCode.OK);
        }
    }*/

    public void json(String text, int status) throws IOException {
        send(text.trim().getBytes(), -1, "application/json", status);
    }

    public void setHeader(String key, String value){
        this.headers.add(new Header(key.trim(), value.trim()));
    }

    //Auto generate the error header and body
    public void sendError(int status) throws IOException {
        //The message will be displayed to the client side
        String retMessage = statuses[status];

        isClosed = true;

        send(retMessage.getBytes(), -1, "text/plain", status);
    }

    private byte[] sendHeaders(byte[] text , int length,  String mimeType, int status) throws IOException {
        //Length that will be assigned to the Content-Length header
        byte[] encodedTextCopy = null;

        if (length != -1){
            encodedTextCopy = new byte[length]; //This will be the byte array when we don't encode
        }else{
            encodedTextCopy = text;
        }

        //If the status is detected to be 404, we can instantly serve the body
        //with the template

        if (request.headers.find("Content-Type") == null){
            setHeader("Content-Type", mimeType + ";charset=utf-8");
        }

        //Set the encoding header
        if (isEncodedSupported && isEncoding){
            Header validEncodingRange = request.headers.find("Accept-Encoding");

            //Going through the encoding list (usually separated with the comma delimiter)
            String[] encodingTypes = validEncodingRange.getValue().split(",");

            for (String type : encodingTypes){
                type = type.trim();

                if (EncoderFactory.isImplemented(type.toLowerCase())){
                    encoder = EncoderFactory.getEncoder(type);
                    setHeader("Content-Encoding", type.toLowerCase());
                    break;
                }
            }

            //Readjust the length
            if (encoder != null){
                encodedTextCopy = encoder.encode(text, length != -1 ? length : text.length);
            }

            System.out.println("After encode: " + encodedTextCopy.length);
        }

        setHeader("Content-Length", "" + encodedTextCopy.length);

        //Set the closing header
        setHeader("Connection", isClosed ? "close" : "keep-alive");

        //Compose and write the response line
        String responseLine = request.version + " " + status + " " + statuses[status];

        oStream.write(responseLine.getBytes(StandardCharsets.UTF_8));
        oStream.write(CRLF);

        //Write the header
        headers.write(oStream);
        
        return encoder == null ? text : encodedTextCopy;
    }

    @Override
    public void close() throws IOException {
        //Close the output and input stream
        oStream.close();

        // However, if either the client or the server includes the Connection: close header in the request or response
        // Then connection should be closed after the current request-response cycle is completed
        if (isClosed){
            request.requestSocket.close();
        }
    }
}
