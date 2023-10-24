package com.ls.akong.mysql_proxy.model;

public class TableColumnInfo {
    private String columnName;
    private String typeName;

    public TableColumnInfo(String columnName, String typeName) {
        this.columnName = columnName;
        this.typeName = typeName;
    }

    public String getColumnName() {
        return columnName;
    }

    public String getTypeName() {
        return typeName;
    }
}
