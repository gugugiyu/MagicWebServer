package com.github.magic.core.models;

import com.github.magic.cache.CacheEntry;
import com.github.magic.cache.ETagGenerator;
import com.github.magic.cache.lru_cache.LRUCache;
import com.github.magic.core.config.Config;
import com.github.magic.core.consts.HttpCode;
import com.github.magic.core.consts.HttpDes;
import com.github.magic.core.consts.Misc;
import com.github.magic.core.encoder.Encoder;
import com.github.magic.core.encoder.EncoderFactory;
import com.github.magic.core.models.header.AcceptHeader;
import com.github.magic.core.models.header.Header;
import com.github.magic.core.models.header.Headers;
import com.github.magic.core.models.server.Server;
import com.github.magic.core.utils.FileAttributeRetriever;
import com.github.magic.core.utils.Formatter;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Response implements Closeable {
    //The associated request for this response
    private final Request req;

    //THe output stream that will be used to send the response to client
    private final OutputStream oStream;

    //The header list
    private final Headers headers;

    //This flag tells if the underlying connection (socket connection) should be closed after this request-response cycle
    private boolean isClosed;

    //Will be set in case the current response needs to be encoded
    private Encoder encoder;

    //Tells if the header part has already been sent. You can't resend a sent header
    private boolean isHeaderSent;

    //Http status code of the response, is default to 200 OK unless set different
    private short status = HttpCode.OK;

    //An array with size of 2, trailer values will be ignored
    //Tells the maximum request that the current socket connection could handle and
    //the time when the host will allow an idle connection to remain open before it is closed
    private final int[] keepAlive = new int[2];

    //Used for HEAD http method, won't send the body of the response
    private boolean discardBody;

    //Tracks the execution time of the thread from its constructor until finish by invoking any send() method
    private final long totalExecutionTime;

    //The charset of the current response, default is UTF-8
    //This field should be set before invoking the send() method in order to take effect
    private Charset charset = StandardCharsets.UTF_8;

    //The locale of the current response, default to EN
    //This field should be set before invoking the send() method in order to take effect
    private Locale language = Locale.ENGLISH;

    //The server that's in charge processing this response
    private Server server;

    //If the current response derived from a file, then set it path here
    private Path staticPath;

    /**
     * Tells if the current response cycle is trigger by the SSL handshake.
     * <br>
     * All {@code send()} method has no effect when this isn't set to true.
     */
    private final boolean isHandshakeCompleted;

    public Response(OutputStream oStream) throws IOException {
        this.req = null;
        this.isHandshakeCompleted = true;

        this.oStream = new ResponseOutputStream(oStream);
        this.headers = new Headers();
        this.server = null;

        this.totalExecutionTime = System.currentTimeMillis();
    }

    /**
     * Initialize the response, and decides whether compression should be done for this request
     *
     * @param req the request corresponding to this response
     * @param keepAlive an int array with size of 2, specify the max request per socket connectiona and idle connection timeout duration. Trailing values is ignored
     * @param isHandshakeCompleted used for logger, indicating if the current socket request still from the ssl handshake
     * @param server The server in charge of this response
     * @throws IOException when an IO exception occur when trying to write back to the output stream
     */
    public Response(Request req, int[] keepAlive, boolean isHandshakeCompleted, Server server) throws IOException {
        this.req = req;
        this.headers = new Headers();
        this.oStream = getOutputStreamType();

        // If the handler force this connection to close (in case of time out request), then let it be
        // Else we check the value from the "Connection" header from the request
        this.isClosed = req.getHeaders().find("Connection").equalsIgnoreCase("close") || req.getVersion().equals("1.0");

        //Check the keepAlive field for more infos
        this.keepAlive[0] = keepAlive[0];
        this.keepAlive[1] = keepAlive[1] / 1000; //Convert s to ms

        this.isHandshakeCompleted = isHandshakeCompleted;
        this.server = server;

        this.totalExecutionTime = System.currentTimeMillis();
    }

    public boolean isHeaderSent() {
        return isHeaderSent;
    }

    /**
     * Determines if the current response should be encoded, and returns the corresponding encoder. A response will be considered compressible if meets the following criteria:
     * <ul>
     * <li>When the request header "Accept-Encoding" is presented from the request, which contains any method that's supported by this server</li>
     * <br>
     * <li>When the MIME type of the response type isn't already compressed (check out the list from the {@code COMPRESSED_DATE_TYPE } constant)</li>
     * </ul>
     *
     * <p>Updated on 23/7/2024, this method is now taken into consideration of the quality from the content-negotiation now. Therefore it will returns the most suitable encoder that is implemented</p>
     *
     * @param mimeType The MIME-type of the request
     * @return the encoder to be used from the {@link EncoderFactory}
     * @throws IOException exception when performing compression
     */
    private Encoder shouldEncode(String mimeType) throws IOException {
        if (mimeType == null || mimeType.isEmpty() || req  == null || req.getHeaders().find("Accept-Encoding").isEmpty())
            return null;

        Encoder returnEncoder = null;
        String subType = mimeType.substring(mimeType.lastIndexOf("/") + 1);

        //Handles logic for whether we should encode this http response
        //For now, let say that for file types that are already compressed, we won't compress it anymore
        //Lossy or lossless doesn't matter here
        final String[] COMPRESSED_DATA_TYPE = {
                "mp3",  "mp4",  "pdf",  "rar", "apk",
                "jpg",  "aac",  "avi",  "docx", "7z",  "mpg",
                "jpeg", "ogg",  "mkv",  "xlsx", "gz",  "tar.gz",
                "gif",  "wma",  "mov",  "pptx", "iso",
                "webp", "flac", "webm", "zip",  "epub"
        };

        for (String string : COMPRESSED_DATA_TYPE) {
            if (subType.endsWith(string)) {
                return null;
            }
        }

        //Going through the encoding list (usually separated with the comma delimiter)
        var encodingTypes = ((AcceptHeader) req.getHeaders().findHeader("Accept-Encoding")).getRepresentationAndQuality();

        while (!encodingTypes.isEmpty()){
            AcceptHeader.PriorityString type = Objects.requireNonNull(encodingTypes.poll());
            returnEncoder = EncoderFactory.getEncoder(type.value().trim());

            if (returnEncoder != null) break;
        }

        return returnEncoder;
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


    /**
     * The base method of sending back the response. If {@link #isHandshakeCompleted} isn't true, then calling this method has no effect
     *
     * @see #send(String)
     * @see #send(String, String, short)
     * @see FileAttributeRetriever#getMimeType(File)
     *
     * @param byteArr the array of bytes as the response body
     * @param length the actualy length of the body, passing negative value would be interpreted as using {@code byteArr.length}
     * @param dateModified the last date the content was modifed. Passing {@code null} would take current day instead
     * @param mimeType the mimeType of the content
     * @param status the http status code
     *
     * */
    public void send(byte[] byteArr, int length, Date dateModified, String mimeType, short status) {
        if (isHeaderSent){
            System.out.println("[-] Headers has already been sent");
            return;
        }

        if (dateModified == null) dateModified = new Date();

        //Skip if SSL handshake isn't completed
        if (!isHandshakeCompleted) return;

        this.status = status;

        try{
            this.encoder = shouldEncode(mimeType);
        } catch (IOException e){
            this.encoder = null;
        }

        int realLength = length > 0 ? length : byteArr.length;
        byte[] content = null;

        String eTag;

        try{
            // Encode the response. Typically, we should encode this it if the file is too large
            // or the file type of the current file isn't compressed by nature
            if (encoder != null && length > Config.COMPRESS_THRESHOLD) {
                content = encoder.encode(byteArr, realLength);
                realLength = content.length;
            }

            if (staticPath != null){
                eTag = ETagGenerator.generateEtagForStaticResponse(
                        realLength,
                        dateModified == null ? new Date().getTime() / 1000 : dateModified.getTime() / 1000, //to seconds
                        mimeType,
                        String.valueOf(language),
                        encoder == null ? "" : encoder.toString()
                );
            }else{
                eTag = ETagGenerator.generateEtagForDynamicResponse(byteArr);
            }

            prepareHeader(dateModified, realLength, mimeType, eTag);
            sendHeaders();
            isHeaderSent = true;

            if (!discardBody)
                oStream.write(content == null ? byteArr : content, 0, realLength);

        } catch (IOException e){
            System.err.println("[-] Failed to serve: " + (req == null ? "[Unable to get req path]" : req.getPath().getPath()));
            return;
        }

        //Write to cache, this method has no effect if this serve is duplicated
        //However, since we've checked the uniqueness of this response from the handleCacheRevalidation() method
        //we can ensure that the write to cache here will be successful

        if (status < 400){
            server.getCache().cache(eTag, new CacheEntry(
                    Config.RESPONSE_MAX_AGE,
                    dateModified,
                    staticPath,
                    encoder == null ? "" : encoder.toString(),
                    language.getLanguage(),
                    staticPath == null ? byteArr : null //This is to save memory, we don't need to keep track of the content if the is a static serve because Etag generation doesn't depend on it
            ));
        }

        logMessage(realLength);
    }

    private void logMessage(int realLength){
        String semanticPath = processSemanticPath();
        if (Config.VERBOSE){
            System.out.printf("[+] %-30s %5d %-50s %3d %-35s %8s %10ss Cached\n",
                    Formatter.convertTime(null),
                    Thread.currentThread().getId(),
                    semanticPath,
                    status,
                    HttpDes.statuses[status],
                    Formatter.getFormatedLength(realLength),
                    (float) (System.currentTimeMillis() - totalExecutionTime) / 1000
            );
        }
    }

    /**
     * Performs a redirect
     *
     * @param url the url to be redirected to
     * @throws IOException when given url is invalid
     */
    public void redirect(String url) throws IOException {
        redirect(url, true);
    }


    /**
     * Borrowed from jhttp: <br>
     * Perform a redirect
     *
     * @see #redirect(String)
     *
     * @param url the url to be redirected to (in absolute form)
     * @param permanent tells if this redirect if pernament
     * @throws IOException when given url is invalid
     */
    public void redirect(String url, boolean permanent) throws IOException {
        try {
            url = new URI(url).toASCIIString();
        } catch (URISyntaxException e) {
            throw new IOException("Malformed URL " + url);
        }

        setHeader("Location", url);

        // some user-agents expect a body, so we send it
        if (permanent)
            sendStatus(HttpCode.MOVED_PERMANENTLY);
        else
            sendStatus(HttpCode.TEMPORARY_REDIRECT);
    }

    /**
     * Send back the response with just the text and any custom HTTP status
     *
     * @see #send(byte[], int, Date, String, short)
     *
     * @param text The text to be sent (Leave blank or empty equals discarding body)
     * @param status The http code
     */
    public void send(String text, String mimeType, short status){
        if (text.isEmpty() || text.isBlank())
            discardBody = true;

        send(text.trim().getBytes(), -1, null, mimeType, status);
    }

    /**
     * Send back the response with just any string of text
     *
     * @see #send(String, String, short)
     * @param text The text to be sent (Leave blank or empty equals discarding body)
     */
    public void send(String text){
        send(text.trim(), "text/plain", HttpCode.OK);
    }

    /**
     * Send back the response, but overwrite the MIME-type of the content to be "application/json" instead
     *
     * @see #send(String, String, short)
     *
     * @param text The text to be sent (Leave blank or empty equals discarding body)
     * @param status The status to be returned
     */
    public void json(String text, short status) {
        send(text.trim(), "application/json", status);
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


    /**
     * Even a more concise way to send back error code to user, uses {@link HttpDes#statuses} description list as for the body message
     *
     * @see #sendStatus(short, String)
     *
     * @param status the http status code
     */
    public void sendStatus(short status){
        //The message will be displayed to the client side
        sendStatus(status, HttpDes.statuses[status]);
    }

    /**
     * Send back error to user with the status code and a custom message for the body message
     *
     * @see #send(byte[], int, Date, String, short)
     *
     * @param status the http status code
     * @param errorText the custom error message
     */
    public void sendStatus(short status, String errorText){
        //TODO need more fault-tolerance error here
        if (status == HttpCode.NOT_FOUND)
            isClosed = false;

        // We have the authority here to catch the exception as we know the send() method only being invoked once
        send(errorText.getBytes(), errorText.length(), null, "text/plain", status);
    }

    public void sendEtagMatched(String eTag){
        setHeader("ETag", eTag);

        send("", "", HttpCode.NOT_MODIFIED);
    }

    /**
     * Prepares the <a href="https://developer.mozilla.org/en-US/docs/Glossary/CORS-safelisted_response_header">CORS safe list header</a> <br> <br>
     * This method also take in consideration the "Access-Control-Expose-Headers" header from cors for an extension of CORS safe list
     *
     * @param lastModified Last date when the resource was modified
     * @param length The length (of the byte array). If given negative numbers, this value represents the chunk-based serving style
     * @param mimeType The MIME-type of the content
     * @param etag The ETag associated with this response
     */
    private void prepareHeader(Date lastModified, int length, String mimeType, String etag) {
        setHeader("Content-Encoding", (encoder != null && length > Config.COMPRESS_THRESHOLD)
                ? encoder.toString()
                : null
        ); //Content-Encoding is an exception, it must be set for every requests

        if (!discardBody){
            //Set the common serving header
            //If error response, no need to set "Last-Modified"
            if (status < 400){
                setHeader("Last-Modified", Formatter.convertTime(lastModified));

                if (headers.find("Etag").isEmpty())
                    setHeader("ETag", isClosed ? "" : "\"" + etag + "\"");
            }

            if (length != - 1){
                setHeader("Content-Length", "" + length);
            }

            setHeader("Content-Type", mimeType + "; charset=" + charset.name());
            setCacheHeader(mimeType);

            // Prevent MIME sniffing
            // https://en.wikipedia.org/wiki/Content_sniffing
            setHeader("X-Content-Type-Options", "nosniff");
        }

        //Set the "dangerous" (not in the safe list) headers
        setHeader("Server", "MagicWebServer/1.2");
        setHeader("Date", Formatter.convertTime(null));
        setHeader("Connection", isClosed ? "close" : "keep-alive");
        setHeader("Accept-Ranges", "bytes");

        if (!isClosed)
            setHeader("Keep-Alive", "timeout=" + keepAlive[1] + ", max=" + keepAlive[0]);

        String exposedHeader = headers.find("Access-Control-Expose-Headers");

        if (exposedHeader.equals("*")) // Allow all header to be exposed
            return;

        if (!exposedHeader.isEmpty()){
            ArrayList<String> exposedHeaderList = new ArrayList<>(List.of(exposedHeader.split(",")));
            headers.getHeaders().removeIf(i -> !exposedHeaderList.contains(i.getKey()));
        }
    }

    /**
     * A simple helper function that helps set the "Cache-Control" header
     */
    private void setCacheHeader(String mimeType){
        //List of common mime types which usually is static file
        //A resource is considered static when either it exists as a file, or has the MIME-type matching the below list
        boolean isMatched = false;
        
        String[] staticType = new String[]{
                "image/*",
                "video/*",
                "font/*",
                "audio/*",
                "text/css",
                "text/html",
                "text/javascript"
        };

        int i = 0;

        while(i < staticType.length){
            isMatched = FileAttributeRetriever.compareMimeType(staticType[i], mimeType) == 2;

            if (isMatched)
                break;

            i++;
        }

        setHeader("Cache-Control", staticPath != null && isMatched
                ? "public, max-age=" + Config.RESPONSE_MAX_AGE
                : "no-cache, must-revalidate"
        );
    }

    /**
     * Send the response line independently
     *
     * @see #sendHeaders()
     *
     * @throws IOException i/o error when sending
     */
    private void sendResponseLine() throws IOException {
        //Compose and write the response line
        String responseLine = "HTTP/" + (req == null ? "1.1" : req.getVersion()) + " " + status + " " + HttpDes.statuses[status];

        oStream.write(responseLine.getBytes(StandardCharsets.UTF_8));
        oStream.write(Misc.CRLF);
    }

    /**
     * Send BOTH the {@link #sendResponseLine()} and the headers of the current response
     *
     * @throws IOException i/o error when sending
     */
    private void sendHeaders() throws IOException {
        sendResponseLine();

        //Write the header
        headers.write(oStream);
    }

    /**
     * Basically the send method, but with {@code Content-Disposition} header set
     *
     * @see #send(byte[], int, Date, String, short)
     *
     * @param path The path of the file to be downloaded
     * @param fileName The name of the file
     * @throws IOException i/o error when sending
     */
    public void download(String path, String fileName) throws IOException{
        if (fileName.isEmpty() || fileName.isBlank())
            return;

        if (path.startsWith("./"))
            path = path.substring(1);

        String realPath = Config.STATIC_DIR + path;

        if (!Files.exists(Path.of(realPath))
                || Files.isDirectory(Path.of(realPath))
                || !Files.isReadable(Path.of(realPath))){
            sendStatus(HttpCode.NOT_FOUND);
            return;
        }

        //Sent as attachment
        setHeader("Content-disposition", "attachment; filename=" + fileName);
        sendFile(realPath);
    }

    /**
     * Send back a static file from the {@link Config#STATIC_DIR} directory. Generate index page for directory path
     *
     * @param path The path to the static file
     * @throws IOException I/O Exception may occur when perform writing to the output stream
     */
    public void sendFile(String path) throws IOException {
        if (path.charAt(0) == '.')
            path = path.substring(1);

        File file = new File(Config.STATIC_DIR + path);

        if (file.isDirectory()) {
            //Redirect URL for directories requests that don't start with "/"
            if (!path.endsWith("/")) {
                redirect(path + "/", true);
            } else {
                //If the path points to the directory, 200 OK
                serveDirectory(file);
            }
        } else if (!file.exists()
                || file.isHidden()
                || file.getName().startsWith(".")) {

            //Not Found
            sendStatus(HttpCode.NOT_FOUND);
        } else if (!file.canRead()) {

            //Can't read the file
            //403 Forbidden
            sendStatus(HttpCode.FORBIDDEN);
        } else {
            //200 OK
            readAndSendFile(file);
        }
    }

    /**
     * <p>Read the content of the file and call the {@link #send(byte[], int, Date, String, short)} method to send the file. </p>
     *
     * <p>This method support <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/206">206 Partial Content </a>response
     * when the "Range" header is found</p>
     *
     * @see #readWithRangeHeader(String, FileInputStream, byte[], int)
     * @see #send(byte[], int, Date, String, short)
     *
     * @param file The base directory of the file
     * @throws IOException i/o error when sending
     */
    private void readAndSendFile(File file) throws IOException {
        FileInputStream iStream = new FileInputStream(file);
        long fileLength = file.length();
        String rangeHeader = req.getHeaders().find("Range");
        byte[] arr;
        int[] byteRead;

        boolean isClientSupportMimeType = checkClientSupport(FileAttributeRetriever.getMimeType(file));

        if (!isClientSupportMimeType){
            sendStatus(HttpCode.NOT_FOUND);
            return;
        }

        //TODO chunk-based response
       /* if ( dir.file().length() > Config.BODY_BUFFER_SIZE  && oStream instanceof ChunkedOutputStream){
            //Sending in chunks instead
            String mimeType = dir.getMimeType();

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
                send(arr, byteRead, new Date(dir.file().lastModified()), dir.getMimeType(), HttpCode.OK);

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

        byteRead = readWithRangeHeader(
                rangeHeader,
                iStream,
                arr,
                Config.BODY_BUFFER_SIZE
        );

        short resCode = HttpCode.OK;

        if (byteRead[0] == -2){
            //416 Range Not Satisfiable
            sendStatus(HttpCode.RANGE_NOT_SATISFIABLE);
            return;
        }

        if (byteRead[0] + byteRead[1] <= fileLength && !rangeHeader.isEmpty()){
            //Read partially, response with 216 Partial Content
            resCode = HttpCode.PARTIAL_CONTENT;

            int endByte = byteRead[0] + byteRead[1] >= fileLength
                    ? (int) fileLength - 1
                    : byteRead[0] + byteRead[1];


            //Set the Content-Range header
            //https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Range
            //The part after the "/" could be set to * if unknown
            setHeader("Content-Range","bytes " + byteRead[0] + "-" + endByte + "/" + fileLength);
        }

        staticPath = file.toPath();
        send(arr, byteRead[1], new Date(file.lastModified()), FileAttributeRetriever.getMimeType(file), resCode);
    }

    /**
     * If the "Range" header isn't provided in the request, then this method will simply read as much bytes as possible from the content, else, it'll try
     * and read only the bytes between the offset and the upper bound from the "Range" header
     *
     * @return the {@code [offset,length]} of how much bytes read, or {@code [0, -1]} if read none and {@code [-2, 0]} if invalid range
     */
    private int[] readWithRangeHeader(String rangeHeader, FileInputStream iStream, byte[] arr, int maxLength) throws IOException {
        //TODO support multiple ranges request, currently one range supported only
        //https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Range
        int offset       = 0;
        int separatorIdx = 0;
        int len          = maxLength;

        if (!rangeHeader.isEmpty()){
            //TODO modify the logic here
            separatorIdx = rangeHeader.lastIndexOf("=");

            //Usually for single range request, the index would typically look like this
            //Index        0         1
            //Value  [start]-    [end]

            if (separatorIdx == -1) return new int[]{-2, 0};

            String[] range = rangeHeader.substring(separatorIdx + 1).split("-");

            if (!range[0].isEmpty())                     offset = Integer.parseInt(range[0]);
            if (range.length > 1 && !range[1].isEmpty()) len    = Integer.parseInt(range[1]);
        }

        iStream.skip(offset);
        return new int[]{offset, iStream.read(arr, 0, len)};
    }

    /**
     * <p>Returns the index page of all the files within that directory. Currently using inline html template</p>
     * <br>
     * <p>Note that if there's an "index.html" file present in the current directory, it will be served instead.</p>
     *
     * @see #sendFile(String)
     *
     * @param file the directory to be indexed and served
     */
    private void serveDirectory(File file) throws IOException {
        //Mimic behavior of Apache Web server

        StringBuilder template = new StringBuilder("<!DOCTYPE html>\n" +
                "<html>\n" +
                " <head>\n" +
                "  <title>Index of " + file.getName() + "</title>\n" +
                " </head>\n" +
                " <body>\n" +
                "<h1>Index of " + file.getName() + "</h1>\n" +
                "<ul>");

        //Retrieve file name from a directory
        File[] files = file.listFiles(pathname -> !pathname.isHidden() && pathname.canRead());

        if (files == null) {
            template.append("<h3> There's no file in this directory </h3>");
        } else {
            //Making use of the for loop to search for the "index.html" file as well
            for (File item : files) {
                if (item.getName().equalsIgnoreCase("index.html")){
                    readAndSendFile(item);
                    return;
                }

                template.append("<li>").append(buildAnchorLink(
                        item.getName(),
                        item.isDirectory() ? item.getName() + "/" : item.getName()
                )).append("</li>");
            }
        }

        //Close the tags
        template.append("</ul></body></html>");

        send(template.toString().getBytes(), -1,  null, "text/html", HttpCode.OK);
    }

    private String processSemanticPath(){
        if (req == null){
            if (status == HttpCode.REQUEST_TIMEOUT || status == HttpCode.GATEWAY_TIMEOUT)
                return "[Timeout]";

            if (status == HttpCode.INTERNAL_SERVER_ERROR)
                return "[Server error]";

            if (status == HttpCode.BAD_REQUEST)
                return "[Bad request]";

            return "[Path unknown]";
        }

        return req.getMethod() + " " + req.getPath().getPath();
    }

    private String buildAnchorLink(String href, String name) {
        // Returns the following format:
        // <a href="favicon.ico"> favicon.ico</a>

        return "<a href=" + "\"" + href + "\"" + ">" + name + "</a>";
    }

    /**
     * <p>Check the "Accept" header, and find out the if the given MIME-type is supported by client. </p>
     *
     * <p>The requested MIME-type is ordered using a priority queue, with the priority value being the given <a href="https://developer.mozilla.org/en-US/docs/Glossary/Quality_values"> quality values</a> (see more at {@link AcceptHeader})</p>
     *     * <p>Note that client doesn't specify the "Accept" header (which negotiate the accept content), the server will generally assumes that the client can accept any type of content."
     * @param mimeType the MIME-type to be checked
     * @return if the
     */
    private boolean checkClientSupport(String mimeType){
        int compareVal = -1;

        AcceptHeader acceptHeader = ((AcceptHeader) req.getHeaders().findHeader("Accept"));

        if (acceptHeader == null)
            return true; //Accept any

        //Going through the encoding list (usually separated with the comma delimiter)
        var encodingTypes = acceptHeader.getRepresentationAndQuality();

        while (!encodingTypes.isEmpty() && compareVal < 2){
            AcceptHeader.PriorityString type = Objects.requireNonNull(encodingTypes.poll());

            // From docs of compareMimeType():
            // return 1 if the first part matched only, 2 if both the main and subpart matched, or -1 if none matched or invalid MIME-type
            compareVal = FileAttributeRetriever.compareMimeType(type.value(), mimeType);
        }

        return compareVal == 2;
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

    /**
     * Sets the status.
     *
     * @param status the new status to set
     */
    public void setStatus(short status) {
        this.status = status;
    }

    /**
     * Gets the headers.
     *
     * @return the headers
     */
    public Headers getHeaders() {
        return headers;
    }

    /**
     * Sets whether to discard the body.
     *
     * @param discardBody true to discard the body, false otherwise
     */
    public void setDiscardBody(boolean discardBody) {
        this.discardBody = discardBody;
    }

    /**
     * Gets the output stream.
     *
     * @return the output stream
     */
    public OutputStream getOutputStream() {
        return oStream;
    }

    /**
     * Checks if the body should be discarded.
     *
     * @return true if the body should be discarded, false otherwise
     */
    public boolean isDiscardBody() {
        return discardBody;
    }

    /**
     * Sets whether the header has been sent.
     *
     * @param headerSent true if the header has been sent, false otherwise
     */
    public void setHeaderSent(boolean headerSent) {
        isHeaderSent = headerSent;
    }

    public Charset getCharset(){
        return charset;
    }

    public Locale getLanguage() {
        return language;
    }

    public void setLanguage(Locale language) {
        this.language = language;
    }

    public void setCharset(Charset charset) {
        this.charset = charset;
    }
}
