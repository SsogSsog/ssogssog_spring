package org.project.ssogssog.application.common.utils;

public class ParserUtils {

    public static long parseStringToLong(String text) {
        try{
            if (text == null || text.isEmpty() || "-".equals(text)) {
                return 0;
            }
            return Long.parseLong(text);
        }catch (NumberFormatException e){
            return 0;
        }
    }

    public static double parseStringToDouble(String text) {
        try{
            if (text == null || text.isEmpty() || "-".equals(text)) {
                return 0;
            }
            return Double.parseDouble(text);
        }catch (NumberFormatException e){
            return 0;
        }
    }

    public static int parseStringToInt(String text) {
        try{
            if (text == null || text.isEmpty() || "-".equals(text)) {
                return 0;
            }
            return Integer.parseInt(text);
        }catch (NumberFormatException e){
            return 0;
        }
    }

    public static Integer parseIntOrNull(String t){
        if(t == null){
            return null;
        }
        try{
            return Integer.parseInt(t);
        }catch (NumberFormatException e){
            return null;
        }
    }

    public static Long parseLongOrNull(String t){
        if(t == null){
            return null;
        }
        try{
            return Long.parseLong(t);
        }catch (NumberFormatException e){
            return null;
        }

    }

}
