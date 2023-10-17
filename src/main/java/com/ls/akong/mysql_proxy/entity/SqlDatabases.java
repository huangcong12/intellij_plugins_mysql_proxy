package com.ls.akong.mysql_proxy.entity;

public class SqlDatabases {
    private static String tableName = "sql_databases";
    private final int id;
    private final String databaseName;
    private final Long createdAt;

    public SqlDatabases(int id, String databaseName, Long createdAt) {
        this.id = id;
        this.databaseName = databaseName;
        this.createdAt = createdAt;
    }

    public static String getTableName() {
        return tableName;
    }

    public int getId() {
        return id;
    }
}
