package org.workcraft.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Identifier {
    private static final Pattern identifierPattern = Pattern.compile("[_A-Za-z][_A-Za-z0-9]*");

    public static boolean isValid(String s) {
        final Matcher matcher = identifierPattern.matcher(s);
        return matcher.find() && matcher.start() == 0 && matcher.end() == s.length();
    }

    private static final Pattern numberPattern = Pattern.compile("[0-9]*");
    public static boolean isNumber(String s) {
        if (s == null) return false;
        final Matcher matcher = numberPattern.matcher(s);
        return matcher.find() && matcher.start() == 0 && matcher.end() == s.length();
    }
}
