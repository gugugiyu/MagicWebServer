package com.github.magic.core.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public record FileAttributeRetriever() {
    /**
     * Retrieves the MIME-type of a given file. And if it's none determinable, return the type of byte stream (or {@code application/octet-stream}) instead
     *
     * @param file The given file to check for MIME-type
     * @return A string of the MIME-type, or "application/octet-stream" if the value can't be retrieved
     */
    public static String getMimeType(File file) {
        try {
            return Files.probeContentType(file.toPath());
        } catch (IOException e) {
            // In case of unknown error, return byte streams
            return "application/octet-stream";
        }
    }

    /**
     * <p>Compare the two MIME-type together. This function does take in consideration for wildcard symbol "*" in both the main and sub-part of the expected MIME-type</p>
     * <p>Usually, the {@code expected} parameter should be passed in as the client's request MIME-type</p>
     *
     * @param expected The expected MIME-Type
     * @param actual The actual MIME-Type
     * @return {@code 1} if the first part matched only, {@code 2} if both the main and subpart matched, or {@code -1} if none matched or invalid MIME-type
     */
    public static int compareMimeType(String expected, String actual){
        if (expected == null || expected.isEmpty()
                || actual == null || actual.isEmpty())
            return -1;

        int expectedDelimiterIdx = expected.indexOf("/");
        int actualDelimiterIdx = actual.indexOf("/");

        //A MIME-type with no sub-part is invalid
        if (expectedDelimiterIdx == -1 || actualDelimiterIdx == -1)
            return -1;

        if ( expected.substring(0, expectedDelimiterIdx).trim().contains("*")
                || expected.substring(0 , expectedDelimiterIdx).equals(actual.substring(0, actualDelimiterIdx))){
            //Check the sub-part
            if (expected.substring(expectedDelimiterIdx + 1).trim().contains("*")
                    || expected.substring(expectedDelimiterIdx + 1).equals(actual.substring(actualDelimiterIdx + 1)))
                return 2;
            else
                return 1;
        }

        return -1; //None matched
    }
}
