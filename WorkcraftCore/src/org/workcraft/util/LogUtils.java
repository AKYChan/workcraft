package org.workcraft.util;

import org.workcraft.dom.references.ReferenceHelper;
import org.workcraft.dom.visual.SizeHelper;

import java.util.Collection;

public class LogUtils {
    private static final String PREFIX_INFO = "[INFO] ";
    private static final String PREFIX_WARNING = "[WARNING] ";
    private static final String PREFIX_ERROR = "[ERROR] ";
    private static final String PREFIX_STDOUT = "[STDOUT] ";
    private static final String PREFIX_STDERR = "[STDERR] ";

    public static void logMessage(String msg) {
        System.out.println(msg);
    }

    public static void logInfo(String msg) {
        System.out.println(PREFIX_INFO + msg);
    }

    public static boolean isInfoText(String text) {
        return (text != null) && text.startsWith(PREFIX_INFO);
    }

    public static void logWarning(String msg) {
        System.out.println(PREFIX_WARNING + msg);
    }

    public static boolean isWarningText(String text) {
        return (text != null) && text.startsWith(PREFIX_WARNING);
    }

    public static void logError(String msg) {
        System.out.println(PREFIX_ERROR + msg);
    }

    public static boolean isErrorText(String text) {
        return (text != null) && text.startsWith(PREFIX_ERROR);
    }

    public static void logStdout(String msg) {
        System.out.println(PREFIX_STDOUT + msg);
    }

    public static boolean isStdoutText(String text) {
        return (text != null) && text.startsWith(PREFIX_STDOUT);
    }

    public static void logStderr(String msg) {
        System.out.println(PREFIX_STDERR + msg);
    }

    public static boolean isStderrText(String text) {
        return (text != null) && text.startsWith(PREFIX_STDERR);
    }

    public static String getPrefix(String text) {
        if (isInfoText(text)) {
            return PREFIX_INFO;
        }
        if (isWarningText(text)) {
            return PREFIX_WARNING;
        }
        if (isErrorText(text)) {
            return PREFIX_ERROR;
        }
        if (isStdoutText(text)) {
            return PREFIX_STDOUT;
        }
        if (isStderrText(text)) {
            return PREFIX_STDERR;
        }
        return "";
    }

    public static String getTextWithoutPrefix(String text) {
        if (text != null) {
            String prefix = getPrefix(text);
            return text.substring(prefix.length());
        }
        return text;
    }

    public static String getTextWithRefs(String msg, Collection<String> refs) {
        return getTextWithRefs(msg, refs, SizeHelper.getWrapLength());
    }

    public static String getTextWithRefs(String msg, Collection<String> refs, int len) {
        if (refs.size() == 1) {
            msg += " '" + refs.iterator().next() + "'.";
        } else {
            msg = makePlural(msg) + ":";
            String str = String.join(", ", refs);
            if (msg.length() + str.length() > len) {
                msg += "\n";
            } else {
                msg += " ";
            }
            msg += ReferenceHelper.getReferencesAsString(refs, len);
        }
        return msg;
    }

    private static String makePlural(String s) {
        if (s.endsWith("y")) {
            s = s.substring(0, s.length() - 1) + "ie";
        }
        if (s.endsWith("s") || s.endsWith("x") || s.endsWith("z") || s.endsWith("ch") || s.endsWith("sh")) {
            s += "es";
        } else {
            s += "s";
        }
        return s;
    }

}
