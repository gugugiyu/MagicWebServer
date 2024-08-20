package com.github.magic.cache;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class ETagGenerator {
    /**
     * <p>A simple algorithm based to give the uniqueness attribute to any ETag</p>
     *
     * <p>The <b>MIME-type</b> field helps specify between different file type, as a folder could contain multiple file name with different type</p>
     * <p>The <b>Modified date</b> is very helpful in finding any modification such as length, end of line,...</p>
     * <p>The last two fields are exclusively in the context of this webserver, refer to {@link Cache} docs to see why that field is included here</p>
     * @param length
     * @param mimeType
     * @param language
     * @param encoding
     * @param modifiedSince
     * @return
     */
    public static String generateEtagForStaticResponse(long length, long modifiedSince, String mimeType, String language, String encoding) {
        return new BigInteger("" + Math.abs(
                (length + (modifiedSince / 1000)) << 2
                        | (mimeType + language + encoding).hashCode()
        )).toString(36) + "-s";
    }

    /**
     * This method use the MD5 hash function to generate the Strong Etag of the whole content. The reason why MD5 was chosen was because of its efficiency and robustness for any generic hashing usage
     * @param data The whole content to be hashed
     * @return The Etag
     */
    public static String generateEtagForDynamicResponse(byte[] data) {
        try{
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(data);
            return new BigInteger(1, md.digest()).toString(32) + "-d";
        } catch (NoSuchAlgorithmException ignored){}

        return null;
    }
}
