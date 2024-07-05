package com.github.magic.core.encoder;

import com.github.magic.core.config.Config;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.Deflater;

public class DeflateEncoder extends Encoder {
    Deflater deflater;

    public DeflateEncoder() {
        deflater = new Deflater(Deflater.BEST_COMPRESSION);
    }

    @Override
    public byte[] encode(byte[] text, int length) throws IOException {
        ByteArrayOutputStream streamBuffer = new ByteArrayOutputStream();
        byte[] buffer = new byte[Config.ENCODER_BUFFER_SIZE]; //Buffering compressed segments

        //System.out.println("Before encode: " + length);
        deflater.setInput(text);

        //Let it executes
        deflater.finish();

        while (!deflater.finished()) {
            int compressedSize = deflater.deflate(buffer, 0, buffer.length);
            streamBuffer.write(buffer, 0, compressedSize);
        }

        deflater.end();
        return streamBuffer.toByteArray();
    }

    @Override
    public String toString() {
        return "deflate";
    }
}
