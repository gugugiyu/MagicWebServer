package core.path_handler;

import core.models.Request;
import core.models.Response;
import core.consts.HttpCode;
import core.utils.FileAttributeRetriever;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class StaticFileHandler implements Handler {
    //The base directory of the current request
    private final FileAttributeRetriever baseDir;

    private final String rawPath;

    private static final String STATIC_DIR = "\\src\\data";

    public StaticFileHandler(String path){
        baseDir = new FileAttributeRetriever(new File(getAbsStaticPath(path)));
        rawPath = path;
    }

    private String getAbsStaticPath(String requestPath){
        String absPath = new File(".").getAbsolutePath();

        //Substring to remove the "." current dir symbol
        absPath = absPath.substring(0, absPath.length() - 2);

        //And route it to the static dir
        absPath += STATIC_DIR + requestPath;

        return absPath;
    }

    @Override
    public void handle(Request req, Response res) throws IOException {
        String time = getCurrentServerTime();

        //Set the common header
        res.setHeader("Server", "Magic/1.1");
        res.setHeader("Date", time);

        try{
            if (baseDir.getFile().isDirectory()){
                //Redirect URL for directories requests that don't start with "/"
                if (!rawPath.endsWith("/")){
                    res.redirect(rawPath + "/", true);
                }else{
                    //If the path points to the directory, 200 OK
                    serveDirectory(res);
                }
            } else if (!baseDir.getFile().exists()
                || baseDir.getFile().isHidden()
                || baseDir.getFile().getName().startsWith(".")){

                //Not Found
                res.sendError(HttpCode.NOT_FOUND);
            }else if(!baseDir.getFile().canRead()){

                //Can't read the file
                //403 Forbidden
                res.sendError(HttpCode.FORBIDDEN);
            }else{

                //200 OK
                readAndSendFile(res);
            }
        } catch (IOException e){
            //500 Internal Server Error
            System.out.println("[-] Failed to serve " + baseDir.getFile().getPath());

            res.sendError(HttpCode.INTERNAL_SERVER_ERROR);
        }
    }

    private String getCurrentServerTime(){
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return dateFormat.format(calendar.getTime());
    }

    private void readAndSendFile(Response res) throws IOException {
        int bufferSize = 1 << 18;
        byte[] arr = new byte[bufferSize];

        FileInputStream iStream = new FileInputStream(baseDir.getFile());

        int byteRead = iStream.read(arr);

        res.send(arr, byteRead, baseDir.getMimeType(), HttpCode.OK);
    }

    private void serveDirectory(Response res) throws IOException {
        //Mimic behavior of Apache Web server

        String template = "<!DOCTYPE html>\n" +
                "<html>\n" +
                " <head>\n" +
                "  <title>Index of " + rawPath + "</title>\n" +
                " </head>\n" +
                " <body>\n" +
                "<h1>Index of "  + rawPath + "</h1>\n" +
                "<ul>";

        //Retrieve file name from a directory
        File[] files = baseDir.getFile().listFiles(pathname -> !pathname.isHidden() && pathname.canRead());

        for (File file : files){
            template += "<li>" + buildAnchorLink(
                    rawPath + file.getName(),
                    file.isDirectory() ? file.getName() + "/" : file.getName()
                    ) + "</li>";
        }

        //Close the tags
        template += "</ul></body></html>";

        res.send(template.getBytes(), -1, "text/html" ,HttpCode.OK);
    }

    private String buildAnchorLink(String href, String name){
        // Returns the following format:
        // <a href="favicon.ico"> favicon.ico</a>

        return "<a href="+ "\"" + href + "\"" + ">" + name + "</a>";
    }
}
