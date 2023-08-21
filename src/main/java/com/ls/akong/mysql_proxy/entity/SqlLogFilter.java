package com.ls.akong.mysql_proxy.entity;

/**
 * 过滤表
 */
public class SqlLogFilter {
    private final int id;
    private final String sql;
    private final Long createdAt;

    public SqlLogFilter(int id, String sql, Long createdAt) {
        this.id = id;
        this.sql = sql;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public String getSql() {
        return sql;
    }

    public Long getCreatedAt() {
        return createdAt;
    }
}
