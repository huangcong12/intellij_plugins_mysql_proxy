package com.ls.akong.mysql_proxy.entity;

/**
 * 过滤表
 */
public class SqlLogFilter {
    private static String tableName = "sql_log_filter";
    private final int id;
    private final String sql;
    private final Long createdAt;

    public SqlLogFilter(int id, String sql, Long createdAt) {
        this.id = id;
        this.sql = sql;
        this.createdAt = createdAt;
    }

    public static String getTableName() {
        return tableName;
    }

    public int getId() {
        return id;
    }

    public String getSql() {
        return sql;
    }

}
