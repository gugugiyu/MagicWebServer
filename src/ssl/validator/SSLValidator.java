package ssl.validator;

import core.config.Config;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;

public class SSLValidator {
    private static String[] defaultPath = new String[4];

    static{
        defaultPath[0] = Config.ROOT_DIR;
        defaultPath[1] = Config.ROOT_DIR + "cert_data\\";
        defaultPath[2] = Config.ROOT_DIR + "src\\ssl\\";
        defaultPath[3] = Config.ROOT_DIR + "data\\";
    }

    /**
     * The method will validate the ssl components of exported key (.p12 file), and trust store (if any)
     * <br>
     * If the path specified can't be located, this function will try the following path in order:
     * <br>
     * - %ROOT_DIR%/
     * - %ROOT_DIR%/<b>cert_data</b> <br>
     * - %ROOT_DIR%/<b>src/ssl/</b> <br>
     * - %ROOT_DIR%/<b>data/</b> <br>
     *
     * @param path The path to the ssl component
     * @return String return path from param if valid, or any default path if found, else return empty string
     */
    public static String checkPath(String path){
        if (path == null)
            return "";

        String keyName = new File(path).getName();
        path = checkDefault(path, keyName);

        return path;
    }


    private static String checkDefault(String path, String fileName){
        int idx = -1;

        idx = performDefaultSearch(fileName);

        if (idx == -1)
            return ""; //Failed to search the default path also

        return defaultPath[idx] + fileName;
    }

    /**
     * Traverse and return the index of any default path specified from the {@link #checkPath(String)} matched (both existed and readable)
     *
     * @param filename the name of the file to check
     * @return -1 if not found, a range of 1 to the length of defaultPath for the index that matches
     */
    private static int performDefaultSearch(String filename){
        for (int i = 0; i < defaultPath.length; i++){
            if (Files.exists(Path.of(defaultPath[i] + filename))
                    && Files.isReadable(Path.of(defaultPath[i] + filename))){
                return i;
            }
        }

        return -1;
    }
}
