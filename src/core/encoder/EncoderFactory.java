package core.encoder;

import java.io.IOException;

public class EncoderFactory {
    public static boolean isImplemented(String encodingType){
        return switch (encodingType) {
            case "gzip" -> true;
            case "deflate" -> true;
            default -> false;
        };
    }

    public static Encoder getEncoder(String encodingType) throws IOException {
        return switch (encodingType) {
            case "gzip" -> new GZIPEncoder();
            case "deflate" -> new DeflateEncoder();
            default -> null;
        };
    }
}
