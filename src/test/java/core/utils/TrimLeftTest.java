package core.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TrimLeftTest {

    @Test
    public void testTrimLeft_NoTrimmingNeeded() {
        assertEquals("hello", trimLeft("hello", 'a'));
    }

    @Test
    public void testTrimLeft_SingleCharacterTrim() {
        assertEquals("ello", trimLeft("hello", 'h'));
    }

    @Test
    public void testTrimLeft_MultipleCharactersTrim() {
        assertEquals("ello", trimLeft("hhhello", 'h'));
    }

    @Test
    public void testTrimLeft_OnlyTrimCharacters() {
        assertEquals("", trimLeft("hhh", 'h'));
    }

    @Test
    public void testTrimLeft_EmptyString() {
        assertEquals("", trimLeft("", 'h'));
    }

    @Test
    public void testTrimLeft_NoMatchingCharacters() {
        assertEquals("hello", trimLeft("hello", 'z'));
    }

    @Test
    public void testTrimLeft_WhitespaceCharacters() {
        assertEquals("world", trimLeft("   world", ' '));
    }

    @Test
    public void testTrimLeft_SpecialCharacters() {
        assertEquals("##hello", trimLeft("$$##hello", '$'));
    }

    @Test
    public void testTrimLeft_UnicodeCharacters() {
        assertEquals("字漢字世界", trimLeft("漢字漢字世界", '漢'));
    }

    // Helper method
    private String trimLeft(String s, char c) {
        return Formatter.trimLeft(s, c);
    }
}