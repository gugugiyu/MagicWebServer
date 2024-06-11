package core.encoder;

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
        //System.out.println("Before encode: " + length);
        deflater.setInput(text);

        //Let it executes
        deflater.finish();

        final int BUFFER_SIZE = 1 << 11; // 2048

        ByteArrayOutputStream streamBuffer = new ByteArrayOutputStream();
        byte[] buffer = new byte[BUFFER_SIZE]; //Buffering compressed segments

        while (!deflater.finished()) {
            int compressedSize = deflater.deflate(buffer, 0, buffer.length);
            streamBuffer.write(buffer, 0, compressedSize);
        }

        return streamBuffer.toByteArray();
    }
}
