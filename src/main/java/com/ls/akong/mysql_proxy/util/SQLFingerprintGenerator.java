package com.ls.akong.mysql_proxy.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SQLFingerprintGenerator {
    public static String generateFingerprint(String sql) {
        // Remove comments (both single-line and multi-line)
        sql = sql.replaceAll("/\\*.*?\\*/", ""); // Remove multi-line comments
        sql = sql.replaceAll("--.*?\\n", "");   // Remove single-line comments

        // Replace numeric literals with '?'
        sql = sql.replaceAll("-?\\d+(\\.\\d+)?", "?");

        // Replace string literals with '?'
        sql = replaceStringLiterals(sql);

        // Replace value lists (e.g., IN clause) with '?'
        sql = replaceValueLists(sql);

        // Convert the SQL query to lowercase
        sql = sql.toLowerCase();

        // Remove extra spaces and trim
        sql = sql.replaceAll("\\s+", " ").trim();

        return sql;
    }

    private static String replaceStringLiterals(String sql) {
        Pattern pattern = Pattern.compile("('(?:''|[^'])*')");
        Matcher matcher = pattern.matcher(sql);

        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(result, "?");
        }
        matcher.appendTail(result);

        return result.toString();
    }

    private static String replaceValueLists(String sql) {
        Pattern pattern = Pattern.compile("\\([^)]*\\)");
        Matcher matcher = pattern.matcher(sql);

        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
//            String valueList = matcher.group();
            String replacement = "?"; // Replace the entire value list with a single '?'
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);

        return result.toString();
    }

}
