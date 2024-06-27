package core.models;


import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

//Copy pasted from jhttp.

/**
 * The {@code ResponseOutputStream} encompasses a single response over a connection,
 * and does not close the underlying stream so that it can be used by subsequent responses.
 */
public class ResponseOutputStream extends FilterOutputStream {

    /**
     * Constructs a ResponseOutputStream.
     *
     * @param out the underlying output stream
     */
    public ResponseOutputStream(OutputStream out) {
        super(out);
    }

    @Override
    public void close() {
    } // keep underlying connection stream open

    @Override // override the very inefficient default implementation
    public void write(byte[] b, int off, int len){
        if (b == null || len <= 0 || off < 0)
            return;

        try{
            out.write(b, off, len);
        } catch (IOException ignored){}
        //When the stream is closed, but we don't need to care about this because here we're at the end of this transaction already
    }
}