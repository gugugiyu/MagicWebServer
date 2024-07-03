package core.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

//Shamelessly copy from jhttp, as I think it really helpful but I'm incapable to reimplementing it alone
public class StreamTransfer {
    /**
     * Transfers data from an input stream to an output stream.
     *
     * @param in  the input stream to transfer from
     * @param out the output stream to transfer to (or null to discard output)
     * @param len the number of bytes to transfer. If negative, the entire
     *            contents of the input stream are transferred.
     * @throws IOException if an IO error occurs or the input stream ends
     *                     before the requested number of bytes have been read
     */
    public static void transfer(InputStream in, OutputStream out, long len) throws IOException {
        if (len == 0 || out == null && len < 0 && in.read() < 0)
            return; // small optimization - avoid buffer creation
        byte[] buf = new byte[4096];
        while (len != 0) {
            int count = len < 0 || buf.length < len ? buf.length : (int) len;
            count = in.read(buf, 0, count);
            if (count < 0) {
                if (len > 0)
                    throw new IOException("unexpected end of stream");
                break;
            }
            if (out != null)
                out.write(buf, 0, count);
            len -= len > 0 ? count : 0;
        }
    }
}
