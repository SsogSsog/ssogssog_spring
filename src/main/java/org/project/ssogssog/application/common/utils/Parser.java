package org.project.ssogssog.application.common.utils;

public class Parser {

    public static long parserStringToLong(String text) {
        try{
            if (text.isEmpty() || "-".equals(text)) {
                return 0;
            }
            return Long.parseLong(text);
        }catch (NumberFormatException e){
            return 0;
        }
    }

    public static double parserStringToDouble(String text) {
        try{
            if (text.isEmpty() || "-".equals(text)) {
                return 0;
            }
            return Double.parseDouble(text);
        }catch (NumberFormatException e){
            return 0;
        }
    }

    public static int parserStringToInt(String text) {
        try{
            if (text.isEmpty() || "-".equals(text)) {
                return 0;
            }
            return Integer.parseInt(text);
        }catch (NumberFormatException e){
            return 0;
        }
    }

}
