package org.project.ssogssog.application.utils;

public class NormalizeUtils {

    public static String normalizeNumber(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;
        return t.replace(",", "");
    }

}
