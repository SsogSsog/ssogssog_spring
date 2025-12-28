package org.project.ssogssog.application.utils;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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

    // --- 유틸리티 메소드 ---

    public static String unzip(byte[] zipBytes) throws Exception {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry zipEntry = zis.getNextEntry(); // 파일이 하나만 들어있음
            if (zipEntry == null) return "";
            return new String(zis.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    public static String getTagValue(String tag, Element element) {
        try {
            NodeList nodeList = element.getElementsByTagName(tag);
            if (nodeList.getLength() > 0) {
                Node node = nodeList.item(0).getChildNodes().item(0);
                return node != null ? node.getNodeValue() : "";
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }

    // 금액 파싱 (콤마 제거, 공백 처리)
    public static Long parseAmount(String amountStr) {
        try {
            if (amountStr == null || amountStr.isEmpty() || amountStr.equals("-")) return 0L;
            return Long.parseLong(amountStr.replace(",", "").trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    // 보고서 코드를 "1Q", "4Q" 등으로 변환
    public static String convertReportCodeToQuarter(String reportCode) {
        return switch (reportCode) {
            case "11013" -> "1Q";
            case "11012" -> "2Q"; // 반기
            case "11014" -> "3Q";
            case "11011" -> "4Q"; // 사업보고서 (연간)
            default -> "Etc";
        };
    }

}
