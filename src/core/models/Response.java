package core.models;

import core.config.Config;
import core.consts.HttpCode;
import core.encoder.Encoder;
import core.encoder.EncoderFactory;
import core.models.header.Header;
import core.models.header.Headers;
import core.utils.FileAttributeRetriever;
import core.utils.Formatter;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static core.consts.HttpDes.statuses;
import static core.consts.Misc.CRLF;

public class Response implements Closeable {
    //The associated request for this response, also can be used for checking client's compatability
    private final Request req;

    private final OutputStream oStream;
    private final Headers headers;
    //This flag tells if the underlying connection (socket connection) should be closed after this request-response cycle
    private boolean isClosed;

    private final Encoder encoder;

    private boolean isHeaderSent;

    private short status; //Http status code of the response

    private int reqCounter; //How many request could be handle left (used for Keep-Alive header)

    private boolean discardBody; //Used for HEAD http method

    private String hostOrigin = "";

    private boolean isHandshakeCompleted;

    private boolean defaultHeader; //Auto-generated header

    public Response(OutputStream oStream) throws IOException {
        this.req = null;
        this.isHeaderSent = false;
        this.defaultHeader = true;

        this.oStream = new ResponseOutputStream(oStream);
        this.headers = new Headers();
        this.encoder = shouldEncode();
    }

    /**
     * Initialize the response, and decides whether compression should be done for this request
     *
     * @param req the request corresponding to this response
     * @param reqCounter the number of request to handle before closing this connection
     * @param isHandshakeCompleted used for logger, indicating if the current socket request still from the ssl handshake
     * @throws IOException when an IO exception occur when trying to write back to the output stream
     */
    public Response(Request req, int reqCounter, boolean isHandshakeCompleted) throws IOException {
        this.req = req;
        this.headers = new Headers();
        this.encoder = shouldEncode();
        this.oStream = getOutputStreamType();

        //If the handler force this connection to close (in case of time out request), then let it be
        this.isClosed = isConnectionClosed();

        this.reqCounter = reqCounter;
        this.isHandshakeCompleted = isHandshakeCompleted;
        this.defaultHeader = true;
        this.isHeaderSent = false;
    }

    public boolean isHeaderSent() {
        return isHeaderSent;
    }

    public boolean isConnectionClosed() {
        String connectionHeader = req.getHeaders().find("Connection");
        return connectionHeader.isEmpty() || connectionHeader.equalsIgnoreCase("close");
    }

    /**
     * Determines if the current response should be encoded, and returns the corresponding encoder. A response will be considered compressible if meets the following criteria:
     * <ul>
     * <li>When the request header "Accept-Encoding" is presented from the request, which contains any method that's supported by this server</li>
     * <br>
     * <li>When the MIME type of the response type isn't already compressed (check out the list from the {@code COMPRESSED_DATE_TYPE } constant)</li>
     * </ul>
     *
     * @return
     * @throws IOException
     */
    private Encoder shouldEncode() throws IOException {
        if (req == null || req.getHeaders().find("Accept-Encoding").isEmpty())
            return null;

        //In case this isn't a static file request, we can't possibly determine what file type it is
        //We should probably encode it
        String path = req.getPath().getPath();

        //We can check the part after the last "/" to see if there's any "." (which signifies a file extension)
        int lastSlashIdx = path.lastIndexOf("/");

        if (!path.substring(lastSlashIdx).contains(".")) {
            return null;
        }

        //Handles logic for whether we should encode this http response
        //For now, let say that for file types that are already compressed, we won't compress it anymore
        //Lossy or lossless doesn't matter here
        final String[] COMPRESSED_DATA_TYPE = {
                "png",  "mp3",  "mp4",  "pdf",  "rar", "apk",
                "jpg",  "aac",  "avi",  "docx", "7z",  "mpg",
                "jpeg", "ogg",  "mkv",  "xlsx", "gz",  "tar.gz",
                "gif",  "wma",  "mov",  "pptx", "iso",
                "webp", "flac", "webm", "zip",  "epub"
        };

        for (String string : COMPRESSED_DATA_TYPE) {
            if (path.endsWith(string)) {
                return null;
            }
        }

        String validEncodingRange = req.getHeaders().find("Accept-Encoding");

        //Going through the encoding list (usually separated with the comma delimiter)
        String[] encodingTypes = validEncodingRange.split(",");

        for (String type : encodingTypes) {
            type = type.trim();

            if (EncoderFactory.isImplemented(type.toLowerCase())) {
                return EncoderFactory.getEncoder(type);
            }
        }

        return null;
    }

    /**
     * Determine the type of output stream for the response
     *
     * @return {@link ChunkedOutputStream} or {@link ResponseOutputStream}
     * @throws IOException I/O exception that might raise when tried to get the output stream
     */
    private OutputStream getOutputStreamType() throws IOException {
        OutputStream out = req.getRequestSocket().getOutputStream();
        String header = req.getHeaders().find("Transfer-Encoding");

        if (header.equalsIgnoreCase("chunked"))
            return new ChunkedOutputStream(out);

        return new ResponseOutputStream(out);
    }

    public void send(byte[] text, int length, Date dateModified, String mimeType, short status) {
        this.status = status;

        int realLength = length > 0 ? length : text.length; //Length could be -1, indicating this method to use text.length instead

        byte[] content = null;

        // Encode the response. Typically, we should encode this it if the file is too large
        // or the file type of the current file isn't compressed by nature

        try{
            if (encoder != null && length > Config.COMPRESS_THRESHOLD) {
                //Readjust the length
                content = encoder.encode(text, realLength);
            }

            if (content == null){
                content = new byte[realLength];
                System.arraycopy(text, 0, content, 0, realLength);
            }

            if (!isHeaderSent){
                prepareHeader(dateModified, content.length, mimeType);
                sendHeaders();
                isHeaderSent = true;
            }

            if (!discardBody)
                oStream.write(content, 0, content.length);

        } catch (IOException e){
            if (Config.SHOW_ERROR) System.err.println("[-] Failed to serve: " + (req == null ? "[Unable to get req path]" : req.getPath().getPath()));
            return;
        }

        if (Config.VERBOSE){
            String semanticPath = processSemanticPath();
            System.out.printf("[+] %-30s %5d %-50s %3d %-35s %8s\n",
                    Formatter.convertTime(null),
                    Thread.currentThread().getId(),
                    semanticPath,
                    status,
                    statuses[status],
                    Formatter.getFormatedLength(content.length)
            );
        }
    }

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

    /**
     * Send back the response with just the text and any custom HTTP status in the HTTPCode range (0 <= code <= 600)
     *
     * @param text The text to be sent (Leave blank or empty equals discarding body)
     * @param status The status to be returned
     */
    public void send(String text, String mimeType, short status){
        if (text.isEmpty() || text.isBlank())
            discardBody = true;

        send(text.trim().getBytes(), -1, null, mimeType, status);
    }

    /**
     * Send back the response with just any string of text, the code will default to be 200 OK
     *
     * @param text The text to be sent
     */
    public void send(String text){
        send(text.trim(), "text/plain", HttpCode.OK);
    }

    /**
     * Send back the response, but overwrite the MIME-type of the content to be "application/json" instead
     *
     * @param text The text to be sent
     * @param status The status to be returned
     */
    public void json(String text, short status){
        send(text.trim().getBytes(), -1, null, "application/json", status);
    }

    /**
     * Set a new header for the response. This method preserve the encapsulation state of the {@link Headers} object. Invoke this method will create
     * a new {@link Header} object <br>
     *
     * If the header already existed, it's value will be overwritten <br>
     *
     *
     * @param key The key (return if be blank or empty)
     * @param value The value (return if blank or empty)
     */
    public void setHeader(String key, String value) {
        if (key == null || key.isEmpty() || key.isBlank())
            return;

        if (value == null || value.isEmpty() || value.isBlank())
            return;

        String existed = headers.find(key);

        if (existed.isEmpty()){
            headers.add(new Header(key.trim(), value.trim()));
        }else{
            headers.set(existed, value);
        }
    }


    //Auto generate the error header and body
    public void sendError(short status){
        //The message will be displayed to the client side
        sendError(status, statuses[status]);
    }

    public void sendError(short status, String errorText){
        //TODO need more fault-tolerance error here
        if (status != HttpCode.NOT_FOUND)
            isClosed = true;

        send(errorText.getBytes(), errorText.length(), null, "text/plain", status);
    }

    /**
     * Prepares the <a href="https://developer.mozilla.org/en-US/docs/Glossary/CORS-safelisted_response_header">CORS safe list header</a> <br> <br>
     * This method also take in consideration the "Access-Control-Expose-Headers" header from cors for an extension of CORS safe list
     *
     * @param lastModified Last date when the resource was modified
     * @param length The length (of the byte array). If given negative numbers, this value represents the chunk-based serving style
     * @param mimeType The MIME-type of the content
     */
    private void prepareHeader(Date lastModified, int length, String mimeType) {
        setHeader("Content-Encoding", (encoder != null && length > Config.COMPRESS_THRESHOLD)
                ? encoder.toString()
                : null
        ); //Content-Encoding is an exception, it must be set for every requests

        if (!defaultHeader) {
            return;
        }

        if (!discardBody){
            //Set the common serving header
            //If error response, no need to set "Last-Modified"
            if (status < 400)
                setHeader("Last-Modified", Formatter.convertTime(lastModified));

            if (length != - 1){
                setHeader("Content-Length", "" + length);
            }

            setHeader("Content-Type", mimeType + ";charset=utf-8");
        }

        //Set the "dangerous" (not in the safe list) headers
        setHeader("Host", hostOrigin);
        setHeader("Server", "MagicWebServer/1.2");
        setHeader("Date", Formatter.convertTime(null));
        setHeader("Connection", isClosed ? "close" : "keep-alive");
        setHeader("Accept-Ranges", "bytes");

        if (!isClosed)
            setHeader("Keep-Alive", "timeout=" + (Config.THREAD_REQUEST_READ_TIMEOUT_DURATION / 1000) + ", max=" + reqCounter);

        //TODO after done on the cache part, make sure to add Cache-Control, Pragma and Expires (in case of backward compatibility) to here
        //as it's in the safe list

        //Check for this header "Access-Control-Expose-Headers" added by the cors middleware
        //as it provides extension to the current cors safe list

        String exposedHeader = headers.find("Access-Control-Expose-Headers");

        if (exposedHeader.equals("*")) // Allow all header to be exposed
            return;

        if (!exposedHeader.isEmpty()){
            ArrayList<String> exposedHeaderList = new ArrayList<>(List.of(exposedHeader.split(",")));
            headers.getHeaders().removeIf(i -> !exposedHeaderList.contains(i.getKey()));
        }
    }

    private void sendResponseLine() throws IOException {
        //Compose and write the response line
        String responseLine = "HTTP/1.1 " + status + " " + statuses[status];

        oStream.write(responseLine.getBytes(StandardCharsets.UTF_8));
        oStream.write(CRLF);
    }

    private void sendHeaders() throws IOException {
        sendResponseLine();

        //Write the header
        headers.write(oStream);
    }

    public void download(String path, String fileName) throws IOException{
        if (fileName.isEmpty() || fileName.isBlank())
            return;

        if (path.startsWith("./"))
            path = path.substring(1);

        String realPath = Config.STATIC_DIR + path;

        if (!Files.exists(Path.of(realPath))
                || Files.isDirectory(Path.of(realPath))
                || !Files.isReadable(Path.of(realPath))){
            sendError(HttpCode.NOT_FOUND);
            return;
        }

        //Sent as attachment
        setHeader("Content-disposition", "attachment; filename=" + fileName);
        sendFile(new FileAttributeRetriever(new File(realPath)), path);
    }

    public void sendFile(FileAttributeRetriever baseDir, String rawPath) throws IOException {
        if (baseDir.file().isDirectory()) {
            //Redirect URL for directories requests that don't start with "/"
            if (!rawPath.endsWith("/")) {
                redirect(rawPath + "/", true);
            } else {
                //If the path points to the directory, 200 OK
                serveDirectory(baseDir, rawPath);
            }
        } else if (!baseDir.file().exists()
                || baseDir.file().isHidden()
                || baseDir.file().getName().startsWith(".")) {

            //Not Found
            sendError(HttpCode.NOT_FOUND);
        } else if (!baseDir.file().canRead()) {

            //Can't read the file
            //403 Forbidden
            sendError(HttpCode.FORBIDDEN);
        } else {
            //200 OK
            readAndSendFile(baseDir);
        }
    }

    /**
     * Se
     *
     * @param baseDir
     * @throws IOException
     */
    private void readAndSendFile(FileAttributeRetriever baseDir) throws IOException {
        FileInputStream iStream = new FileInputStream(baseDir.file());
        long fileLength = baseDir.file().length();
        byte[] arr;
        int[] byteRead;

        //TODO chunk-based response
       /* if ( baseDir.file().length() > Config.BODY_BUFFER_SIZE  && oStream instanceof ChunkedOutputStream){
            //Sending in chunks instead
            String mimeType = baseDir.getMimeType();

            setHeader("Transfer-Encoding", "chunked");

            prepareHeader(new Date(), -1, mimeType);
            sendResponseLine();
            setHeaderSent(true); //"trick" the sent() method so that we can trigger it manually later after the chunked contents

            //Rest of the header comes at the "Trailer"
            List<String> headerList = new ArrayList<>();

            for (Header header : headers)
                headerList.add(header.getKey());

            setHeader("Trailer", String.join(", ", headerList));

            arr = new byte[Config.MAXIMUM_CHUNK_SIZE];
            byteRead = iStream.read(arr);

            while (byteRead != -1){
                send(arr, byteRead, new Date(baseDir.file().lastModified()), baseDir.getMimeType(), HttpCode.OK);

                int avaiByte = iStream.available();

                if (avaiByte < 0)
                    ((ChunkedOutputStream) oStream).writeTerminateChunk();
                else
                    byteRead = iStream.read(arr);
            }

            headers.write(oStream);
            return;
        }*/

        arr = new byte[Config.BODY_BUFFER_SIZE];
        byteRead = readWithRangeHeader(iStream, arr, Config.BODY_BUFFER_SIZE);
        short resCode = HttpCode.OK;

        if (byteRead[0] == -2){
            //416 Range Not Satisfiable
            sendError(HttpCode.RANGE_NOT_SATISFIABLE);
            return;
        }

        if (fileLength > byteRead[0] + byteRead[1]){
            //Read partially, response with 216 Partial Content
            resCode = HttpCode.PARTIAL_CONTENT;

            int endByte = byteRead[0] + byteRead[1] > fileLength
                    ? (int) fileLength - 1
                    : byteRead[0] + byteRead[1];


            //Set the Content-Range header
            //https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Range
            //The part after the "/" could be set to * if unknown
            setHeader("Content-Range","bytes " + byteRead[0] + "-" + endByte + "/" + fileLength);
        }

        send(arr, byteRead[1], new Date(baseDir.file().lastModified()) , baseDir.getMimeType(), resCode);
    }

    /**
     * If the "Range" header isn't provided in the request, then this method will simply read as much bytes as possible from the content, else, it'll try
     * and read only the bytes between the offset and the upper bound from the "Range" header
     *
     * @return the {@code [offset,length]} of how much bytes read, or {@code [0, -1]} if read none and {@code [-2, 0]} if invalid range
     */
    private int[] readWithRangeHeader(FileInputStream iStream, byte[] arr, int maxLength) throws IOException {
        //TODO support multiple ranges request, currently one range supported only
        //https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Range
        int offset       = 0;
        int separatorIdx = 0;
        int len          = maxLength;

        String header = req.getHeaders().find("Range");

        if (!header.isEmpty()){
            //TODO modify the logic here
            separatorIdx = header.lastIndexOf("=");

            //Usually for single range request, the index would typically look like this
            //Index        0         1
            //Value  [start]-    [end]

            if (separatorIdx == -1) return new int[]{-2, 0};

            String[] range = header.substring(separatorIdx + 1).split("-");

            if (!range[0].isEmpty())                     offset = Integer.parseInt(range[0]);
            if (range.length > 1 && !range[1].isEmpty()) len    = Integer.parseInt(range[1]);
        }

        iStream.skip(offset);
        return new int[]{offset, iStream.read(arr, 0, len)};
    }

    private void serveDirectory(FileAttributeRetriever baseDir, String rawPath) {
        //Mimic behavior of Apache Web server

        StringBuilder template = new StringBuilder("<!DOCTYPE html>\n" +
                "<html>\n" +
                " <head>\n" +
                "  <title>Index of " + rawPath + "</title>\n" +
                " </head>\n" +
                " <body>\n" +
                "<h1>Index of " + rawPath + "</h1>\n" +
                "<ul>");

        //Retrieve file name from a directory
        File[] files = baseDir.file().listFiles(pathname -> !pathname.isHidden() && pathname.canRead());

        if (files == null) {
            template.append("<h3> There's no file in this directory </h3>");
        } else {
            for (File file : files) {
                template.append("<li>").append(buildAnchorLink(
                        rawPath + file.getName(),
                        file.isDirectory() ? file.getName() + "/" : file.getName()
                )).append("</li>");
            }
        }

        //Close the tags
        template.append("</ul></body></html>");

        send(template.toString().getBytes(), -1,  null, "text/html", HttpCode.OK);
    }

    private String processSemanticPath(){
        if (status == HttpCode.REQUEST_TIMEOUT || status == HttpCode.GATEWAY_TIMEOUT)
            return "[Timeout]";

        if (status == HttpCode.INTERNAL_SERVER_ERROR)
            return "[Server error]";

        if (req == null){
            if (!isHandshakeCompleted)
                return "[SSL handshake]";
            else
                return "[Path unknown]";
        }

        return req.getMethod() + " " + req.getPath().getPath();
    }

    private String buildAnchorLink(String href, String name) {
        // Returns the following format:
        // <a href="favicon.ico"> favicon.ico</a>

        return "<a href=" + "\"" + href + "\"" + ">" + name + "</a>";
    }

    @Override
    public void close() throws IOException {
        //Close the output stream, and not the underlying stream since we've already overridden the close method
        //from that class

        //Also, we need to flush the output (since we're reusing the same stream to server the client
        if (oStream != null) {
            oStream.close();
            oStream.flush();
        }
    }

    public void setStatus(short status) {
        this.status = status;
    }

    public Headers getHeaders() {
        return headers;
    }

    public void setDiscardBody(boolean discardBody) {
        this.discardBody = discardBody;
    }

    public void setHostOrigin(String hostOrigin) {
        this.hostOrigin = hostOrigin;
    }

    public OutputStream getOutputStream() {
        return oStream;
    }

    public void setDefaultHeader(boolean defaultHeader) {
        this.defaultHeader = defaultHeader;
    }

    public boolean isDiscardBody() {
        return discardBody;
    }

    public void setHeaderSent(boolean headerSent) {
        isHeaderSent = headerSent;
    }
}
