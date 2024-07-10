package com.github.magic.core.encoder;

import java.io.IOException;

public abstract class Encoder {
    /**
     * Encodes the given byte array into a new byte array using a specific encoding scheme.
     *
     * <p>This method takes an input byte array and applies an encoding transformation,
     * returning the resulting encoded byte array. The specific encoding algorithm
     * used should be defined by the implementing class. This method is abstract and
     * must be implemented by subclasses to provide the specific encoding logic.</p>
     *
     * @param text The byte array to be encoded
     * @param length The length of the array
     * @return The encoded byte array
     * @throws IOException if an I/O error occurs during encoding
     */
    public abstract byte[] encode(byte[] byteArr, int length) throws IOException;
}
