package core.utils;

import com.github.magic.core.utils.Formatter;
import org.junit.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;

public class ConvertTimeTest {
    @Test
    public void test_nullString() {
        Date testDate = null;

        //NULL value should be resolved to get current time
        assertEquals(convertTime(new Date()), convertTime(testDate));
    }

    @Test
    public void test_validTime() {
        Date testTime = new Date(1000); //1 seconds after the epoch
        assertEquals(
                "Thu, 01 Jan 1970 00:00:01 UTC",
                convertTime(testTime));
    }

    // Helper method
    private String convertTime(Date date) {
        return Formatter.convertTime(date);
    }
}
