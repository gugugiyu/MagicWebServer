package core.utils;

import java.text.SimpleDateFormat;
import java.util.*;

public class Formatter {
    public static String getFormatedLength(int length) {
        //Returns the size of the file in converted form with unit
        final String[] sizeTable = new String[]{" B", "KB", "MB", "GB", "TB", "PB"};

        int unitIndex = 0;

        while (length >= 1024) {
            length /= 1024;

            //Go to next unit
            unitIndex++;
        }

        return length + " " + sizeTable[unitIndex];
    }

    public static String convertTime(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
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

        if (list.get(0).equals(""))
            list.set(0, "/");

        return list.toArray(new String[0]);
    }
}
