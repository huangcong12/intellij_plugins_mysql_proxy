package com.ls.akong.mysql_proxy.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SQLFingerprintGenerator {
    public static String generateFingerprint(String sql) {
        // 截取前 1500 个字符。避免类型 WordPress 这种项目超级长 SQL，导致下面的 replaceValueLists 抛出：Method threw 'java.lang.StackOverflowError' exception. Cannot evaluate java.util.regex.Pattern.toString()
        sql = sql.length() > 1500 ? sql.substring(0, 1500) : sql;
        sql = removeComments(sql);
        sql = replaceNumericLiterals(sql);
        sql = replaceStringLiterals(sql);
        sql = replaceValueLists(sql);
        sql = convertToLowercase(sql);
        sql = removeExtraSpaces(sql);

        return sql;
    }

    private static String removeComments(String sql) {
        sql = sql.replaceAll("/\\*.*?\\*/", ""); // Remove multi-line comments
        sql = sql.replaceAll("--.*?\\n", "");   // Remove single-line comments
        sql = sql.replaceAll("#.*?\\n", "");    // Remove single-line comments starting with #

        return sql;
    }

    private static String replaceNumericLiterals(String sql) {
        sql = sql.replaceAll("-?\\d+(\\.\\d+)?", "?");

        return sql;
    }

    private static String replaceStringLiterals(String sql) {
        Pattern pattern = Pattern.compile("('(?:''|[^'])*')");
        Matcher matcher = pattern.matcher(sql);

        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String literal = matcher.group();
            literal = literal.replaceAll("'", "''"); // Escape single quotes
            matcher.appendReplacement(result, "?");
        }
        matcher.appendTail(result);

        return result.toString();
    }

    private static String replaceValueLists(String sql) {
        Pattern pattern = Pattern.compile("\\([^()]*\\)");
        Matcher matcher = pattern.matcher(sql);

        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(result, "?");
        }
        matcher.appendTail(result);

        return result.toString();
    }

    private static String convertToLowercase(String sql) {
        return sql.toLowerCase();
    }

    private static String removeExtraSpaces(String sql) {
        sql = sql.replaceAll("\\s+", " ").trim();

        return sql;
    }

    /**
     * md5 sql 然后取右 16 位，参考 soar 的，soar 给的解释是：
     * Id returns the right-most 16 characters of the MD5 checksum of fingerprint.Query IDs are the shortest way to uniquely identify queries.
     *
     * @param fingerprint
     * @return
     */
    public static String getSignature(String fingerprint) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(fingerprint.getBytes());
            byte[] digest = md.digest();

            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                String hex = Integer.toHexString(0xFF & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            String hash = hexString.toString().toUpperCase();
            return hash.substring(16, 32);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null; // Handle the exception appropriately
        }
    }

    public static String getSignatureBySql(String sql) {
        return getSignature(generateFingerprint(sql));
    }
}
