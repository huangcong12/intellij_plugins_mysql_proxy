package com.ls.akong.mysql_proxy.util;

public class StringHelper {
    /**
     * 合并成一行，长空格缩成一个
     *
     * @param str
     * @return
     */
    public static final String mergedIntoOneLine(String str) {
        return str.replaceAll("\n", " ").replaceAll("\r", " ").replaceAll("\t", " ").replaceAll(" +", " ").trim();
    }
}
