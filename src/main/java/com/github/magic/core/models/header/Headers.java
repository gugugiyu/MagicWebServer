package com.github.magic.core.models.header;

import com.github.magic.core.consts.Misc;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;

public class Headers implements Iterable<Header> {
    ArrayList<Header> headers = new ArrayList<>();


    public void add(Header newHeader) {
        if (!headers.contains(newHeader)) {
            headers.add(newHeader);
        } else {
            //We can overwrite it
            set(newHeader.key, newHeader.value);
        }
    }

    public void set(String key, String newValue) {
        //Trim out the key
        if (key == null || key.isEmpty()) {
            return;
        }

        //Iterate through the header list and remove the record with that key
        int headerLength = headers.size();

        for (int i = 0; i < headerLength; i++)
            if (headers.get(i).key.equals(key)) {
                headers.set(i, new Header(key, newValue));
            }
    }

    /**
     * Find and return the header's value based on the key
     *
     * @param key the key to look for
     * @return the {@code value} of the header if found, empty string {@code ""} if not found
     */
    public String find(String key) {
        for (Header header : headers) {
            if (header.key.equalsIgnoreCase(key))
                return header.value;
        }

        return "";
    }

    /**
     * Write all fields of the current header list into the "key: value" format, CRLF end-of-line behavior
     * 
     * @param out the output stream to be write to
     * @throws IOException
     */
    public void write(OutputStream out) throws IOException {
        for (Header header : headers) {
            out.write((header.getKey() + ": " + header.getValue()).getBytes());
            out.write(Misc.CRLF);
        }

        out.write(Misc.CRLF); // ends header block
    }

    public boolean remove(String key) {
        //Trim out the key
        if (key == null || key.isEmpty()) {
            return false;
        }

        //Iterate through the header list and remove the record with that key
        int headerLength = headers.size();

        for (int i = 0; i < headerLength; i++)
            if (headers.get(i).key.equals(key)) {
                headers.remove(i);
                return true;
            }

        return false;
    }

    /**
     * Return the array list of all keys from the header list
     * @return null if no header is presented, or array list of type String if convertible
     */
    public ArrayList<String> getKeyList(){
        if (headers == null || headers.isEmpty())
            return null;

        ArrayList<String> retList = new ArrayList<>();

        for (Header header : headers){
            retList.add(header.getKey());
        }

        return retList;
    }

    /**
     * Return the array list of all values from the header list
     * @return null if no header is presented, or array list of type String if convertible
     */
    public ArrayList<String> getValueList(){
        if (headers == null || headers.isEmpty())
            return null;

        ArrayList<String> retList = new ArrayList<>();

        for (Header header : headers){
            retList.add(header.getValue());
        }

        return retList;
    }

    /**
     * Clear the current header list. If called on an empty list, this method has no effect
     */
    public void clear(){
        headers.clear();
    }

    @Override
    public Iterator<Header> iterator() {
        return headers.iterator();
    }

    public ArrayList<Header> getHeaders() {
        return headers;
    }
}
