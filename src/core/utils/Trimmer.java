package core.utils;

public class Trimmer {
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
}
