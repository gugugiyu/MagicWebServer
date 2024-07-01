package core.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class GetFormattedLengthTest {
    @Test
    void test_validLength() {
        assertEquals("500 KB", getFormattedLength(500000));
    }

    @Test
    void test_negativeLength() {
        assertNull(getFormattedLength(Integer.MIN_VALUE));
    }

    @Test
    void test_0Length() {
        assertEquals("0 B", getFormattedLength(0));
    }

    @Test
    void test_maxLength() {
        // 2147483647 bytes is roughly 2 GB

        assertEquals("2 GB", getFormattedLength(Integer.MAX_VALUE));
    }


    // Helper method
    private String getFormattedLength(int length) {
        return Formatter.getFormatedLength(length);
    }
}
