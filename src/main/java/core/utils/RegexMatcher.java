package core.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class RegexMatcher {
    /**
     * Method to find the first matched group in a string using a regex pattern.
     *
     * @param input The input string to be searched.
     * @param regex The regex pattern to search with.
     * @return The first matched group, or null if no match is found.
     */
    public String check(String input, String regex) throws PatternSyntaxException {
/*        System.out.println("Input: " + input);
        System.out.println("Regex: " + regex);*/

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);

        if (matcher.find()) {
            return matcher.group(); // Return the first matched group
        }

        return null; // Return null if no match is found
    }
}