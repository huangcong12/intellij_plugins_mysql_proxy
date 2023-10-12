package com.ls.akong.mysql_proxy.entity;

/**
 * 过滤表
 */
public class SqlLogFilter {
    private static String tableName = "sql_log_filter1";
    private final int id;
    private final String sqlFinger;
    private final Long createdAt;
    private String signature;

    public SqlLogFilter(int id, String sql, Long createdAt) {
        this.id = id;
        this.sqlFinger = sql;
        this.createdAt = createdAt;
    }

    public static String getTableName() {
        return tableName;
    }

    public int getId() {
        return id;
    }

    public String getSqlFinger() {
        return sqlFinger;
    }

}
