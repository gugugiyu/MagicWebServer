package com.github.magic.core.utils;

import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.*;

public class Formatter {
    public static String getFormatedLength(int length) {
        if (length < 0)
            return null;

        //Returns the size of the file in converted form with unit
        final String[] sizeTable = new String[]{"B", "KB", "MB", "GB", "TB", "PB"};

        int unitIndex = 0;

        // Using the SI (International System of Units) for megabyte when displaying size
        // 1 megabyte = 1000 kilobytes
        while (length >= 1000) {
            length /= 1000;

            //Go to next unit
            unitIndex++;
        }

        return length + " " + sizeTable[unitIndex];
    }

    public static String convertTime(Date date) {
        if (date == null)
            date = new Date();

        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        return dateFormat.format(date);
    }

    public static String trimLeft(String s, char c) {
        int i = 0;
        int len = s.length();

        while (i < len && s.charAt(i) == c)
            i++;

        return s.substring(i);
    }

    public static String trimRight(String s, char c) {
        int i = s.length() - 1;

        while (i >= 0 && s.charAt(i) == c)
            i--;

        return s.substring(0, i + 1);
    }

    public static String[] replaceEmptyWithRoot(String[] strings){
        ArrayList<String> list = new ArrayList<>(Arrays.asList(strings));

        if (list.get(0).isEmpty())
            list.set(0, "/");

        return list.toArray(new String[0]);
    }
}
