package core.utils;


import com.github.magic.core.utils.Formatter;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class GetFormattedLengthTest {
    @Test
    public void test_validLength() {
        assertEquals("500 KB", getFormattedLength(500000));
    }

    @Test
    public void test_negativeLength() {
        assertNull(getFormattedLength(Integer.MIN_VALUE));
    }

    @Test
    public void test_0Length() {
        assertEquals("0 B", getFormattedLength(0));
    }

    @Test
    public void test_maxLength() {
        // 2147483647 bytes is roughly 2 GB

        assertEquals("2 GB", getFormattedLength(Integer.MAX_VALUE));
    }


    // Helper method
    private String getFormattedLength(int length) {
        return Formatter.getFormatedLength(length);
    }
}
