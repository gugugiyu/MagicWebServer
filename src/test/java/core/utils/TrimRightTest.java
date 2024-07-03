package core.utils;


import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TrimRightTest {

    @Test
    public void testTrimRight_NoTrimmingNeeded() {
        assertEquals("hello", trimRight("hello", 'a'));
    }

    @Test
    public void testTrimRight_SingleCharacterTrim() {
        assertEquals("hell", trimRight("hello", 'o'));
    }

    @Test
    public void testTrimRight_MultipleCharactersTrim() {
        assertEquals("hee", trimRight("heeoooo", 'o'));
    }

    @Test
    public void testTrimRight_OnlyTrimCharacters() {
        assertEquals("", trimRight("ooo", 'o'));
    }

    @Test
    public void testTrimRight_EmptyString() {
        assertEquals("", trimRight("", 'o'));
    }

    @Test
    public void testTrimRight_NoMatchingCharacters() {
        assertEquals("hello", trimRight("hello", 'z'));
    }

    @Test
    public void testTrimRight_WhitespaceCharacters() {
        assertEquals("hello", trimRight("hello   ", ' '));
    }

    @Test
    public void testTrimRight_SpecialCharacters() {
        assertEquals("##hello", trimRight("##hello$$", '$'));
    }

    @Test
    public void testTrimRight_UnicodeCharacters() {
        assertEquals("世界字漢字", trimRight("世界字漢字漢漢", '漢'));
    }

    // Helper method to call the static method from the class where it's defined
    private String trimRight(String s, char c) {
        return Formatter.trimRight(s, c); // Adjust the class name if it's different
    }
}