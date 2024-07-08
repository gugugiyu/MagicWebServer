package com.github.magic.core.encoder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

public class GZIPEncoder extends Encoder {
    private final GZIPOutputStream gzipStream;
    private final ByteArrayOutputStream streamBuffer;


    public GZIPEncoder() throws IOException {
        streamBuffer = new ByteArrayOutputStream();
        gzipStream = new GZIPOutputStream(streamBuffer);
    }

    @Override
    public byte[] encode(byte[] data, int length) throws IOException {
        //Compress and close the gzip stream
        gzipStream.write(data, 0, length);
        gzipStream.close();

        return streamBuffer.toByteArray();
    }

    @Override
    public String toString() {
        return "gzip";
    }
}
