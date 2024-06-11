package core.models.header;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;

import static core.models.Server.CRLF;

public class Headers implements Iterable<Header>{
    ArrayList<Header> headers = new ArrayList<>();


    public void add(Header newHeader){
        if (!headers.contains(newHeader)){
            headers.add(newHeader);
        }else{
            //We can overwrite it
            set(newHeader.key, newHeader.value);
        }
    }

    public boolean set(String key, String newValue){
        //Trim out the key
        if (key == null || key.isEmpty()){
            return false;
        }

        //Iterate through the header list and remove the record with that key
        int headerLength = headers.size();

        for (int i = 0; i < headerLength; i++)
            if (headers.get(i).key.equals(key)){
                headers.set(i, new Header(key, newValue));
                return true;
            }

        return false;
    }

    public Header find(String key){
        for (Header header : headers){
            if (header.key.equals(key))
                return header;
        }

        return null;
    }

    public void write(OutputStream out) throws IOException {
        for (Header header : headers) {
            out.write((header.getKey() + ": " + header.getValue()).getBytes());
            out.write(CRLF);
        }

        out.write(CRLF); // ends header block
    }

    public boolean remove(String key){
        //Trim out the key
        if (key == null || key.isEmpty()){
            return false;
        }

        //Iterate through the header list and remove the record with that key
        int headerLength = headers.size();

        for (int i = 0; i < headerLength; i++)
            if (headers.get(i).key.equals(key)){
                headers.remove(i);
                return true;
            }

        return false;
    }

    @Override
    public Iterator<Header> iterator() {
        return headers.iterator();
    }
}
