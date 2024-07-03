package core.models;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static core.consts.Misc.CRLF;

public class ChunkedOutputStream extends FilterOutputStream {
    //Tells whether the writing mode for this current stream should be normal again (after sending the termination chunk)
    private boolean isTerminated;

    public ChunkedOutputStream(OutputStream out) {
        super(out);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        //From https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Transfer-Encoding

        // At the beginning of each chunk you need to add the length of the current chunk in hexadecimal format.
        // Followed by '\r\n' and then the chunk itself, followed by another '\r\n'.
        if (!isTerminated){
            out.write(Integer.toHexString(len).getBytes());
            out.write(CRLF);
        }

        out.write(b);
        out.write(CRLF);
    }

    public void writeTerminateChunk() throws IOException {
        isTerminated = true;

        // The terminating chunk is a regular chunk, with the exception that its length is zero.
        out.write(new byte[]{0x00});
        out.write(CRLF);
    }

    @Override
    public void close() {
    } // keep underlying connection stream open

}
