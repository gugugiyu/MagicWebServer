package core.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class FileAttributeRetriever {
    private File file;

    public FileAttributeRetriever(File file){
        this.file = file;
    }

    public String getFormatedLength(){
        //Returns the size of the file in converted form with unit
        final String[] sizeTable = new String[]{"B", "KB", "MB", "GB", "TB", "PB"};

        long fileLength = file.length();

        int unitIndex = 0;

        while (fileLength > 1024){
            fileLength /= 1024;

            //Go to next unit
            unitIndex++;
        }

        return (fileLength + sizeTable[unitIndex]);
    }

    public String getMimeType(){
        try{
            return Files.probeContentType(file.toPath());
        } catch (IOException e){
            // In case of unknown error, return byte streams
            return "application/octet-stream";
        }
    }


    public File getFile() {
        return file;
    }
}
