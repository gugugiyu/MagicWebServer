package com.github.magic.core.encoder;

import java.io.IOException;

public class EncoderFactory {
    /**
     * Returns the encoder based on the given type of encoding
     *
     * @param encodingType the type of the encoder
     * @return {@link Encoder} instance that was implemented, or {@code null} is the type given is invalid
     * @throws IOException when open the output streams
     */
    public static Encoder getEncoder(String encodingType) throws IOException {
        return switch (encodingType) {
            case "gzip" -> new GZIPEncoder();
            case "deflate" -> new DeflateEncoder();
            default -> null;
        };
    }
}
