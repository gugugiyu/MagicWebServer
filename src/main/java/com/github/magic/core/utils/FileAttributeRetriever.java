package com.github.magic.core.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public record FileAttributeRetriever() {
    public static String getMimeType(File file) {
        try {
            return Files.probeContentType(file.toPath());
        } catch (IOException e) {
            // In case of unknown error, return byte streams
            return "application/octet-stream";
        }
    }
}
