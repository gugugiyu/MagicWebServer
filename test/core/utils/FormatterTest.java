package core.utils;

import org.junit.jupiter.api.Test;

class FormatterTest {

    @Test
    void getFormatedLength() {
        GetFormattedLengthTest allTest = new GetFormattedLengthTest();

        allTest.test_maxLength();

        allTest.test_validLength();

        allTest.test_0Length();

        allTest.test_negativeLength();
    }

    @Test
    void convertTime() {
        ConvertTimeTest allTest = new ConvertTimeTest();

        allTest.test_nullString();

        allTest.test_validTime();
    }

    @Test
    void trimLeft() {
        TrimLeftTest allTest = new TrimLeftTest();

        allTest.testTrimLeft_EmptyString();

        allTest.testTrimLeft_NoTrimmingNeeded();

        allTest.testTrimLeft_MultipleCharactersTrim();

        allTest.testTrimLeft_OnlyTrimCharacters();

        allTest.testTrimLeft_NoMatchingCharacters();

        allTest.testTrimLeft_SpecialCharacters();

        allTest.testTrimLeft_WhitespaceCharacters();

        allTest.testTrimLeft_UnicodeCharacters();
    }

    @Test
    void trimRight() {
        TrimRightTest allTest = new TrimRightTest();

        allTest.testTrimRight_EmptyString();

        allTest.testTrimRight_NoTrimmingNeeded();

        allTest.testTrimRight_MultipleCharactersTrim();

        allTest.testTrimRight_OnlyTrimCharacters();

        allTest.testTrimRight_NoMatchingCharacters();

        allTest.testTrimRight_SpecialCharacters();

        allTest.testTrimRight_WhitespaceCharacters();

        allTest.testTrimRight_UnicodeCharacters();
    }
}