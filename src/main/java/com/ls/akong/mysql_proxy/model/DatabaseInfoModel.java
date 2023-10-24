package com.ls.akong.mysql_proxy.model;

import com.intellij.openapi.project.Project;
import com.ls.akong.mysql_proxy.services.MysqlProxySettings;
import com.ls.akong.mysql_proxy.services.PersistingSensitiveDataService;
import com.ls.akong.mysql_proxy.util.StringHelper;

import java.sql.*;
import java.util.ArrayList;
import java.util.Objects;

public class DatabaseInfoModel {
    private final Project project;
    private MysqlProxySettings settings;

    private Connection connection;

    public DatabaseInfoModel(Project project) {
        this.project = project;
        this.settings = MysqlProxySettings.getInstance(project);
    }

    /**
     * 获取 sql 的 explain 信息
     *
     * @param sql
     * @return
     * @throws SQLException
     */
    public String getExplainJsonInfoBySql(String databaseName, String sql) throws SQLException, ClassNotFoundException {
        String explainSql = "explain format=json " + sql;
        ResultSet resultSet = getStatement(databaseName).executeQuery(explainSql);

        String explainFormatJson = "";
        while (resultSet.next()) {
            explainFormatJson = StringHelper.mergedIntoOneLine(resultSet.getString("EXPLAIN"));
        }

        return explainFormatJson;
    }

    /**
     * 获取一个表的所有字段
     *
     * @param databaseName
     * @param tableName
     * @return
     * @throws SQLException
     */
    public ArrayList<TableColumnInfo> getTableMetaData(String databaseName, String tableName) throws SQLException, ClassNotFoundException {
        DatabaseMetaData metaData = getConnection(databaseName).getMetaData();
        ResultSet resultSet = metaData.getColumns(null, null, tableName, null);

        ArrayList<TableColumnInfo> columnInfoList = new ArrayList<TableColumnInfo>();
        while (resultSet.next()) {
            String columnName = resultSet.getString("COLUMN_NAME");
            String dataType = resultSet.getString("TYPE_NAME");
            TableColumnInfo columnInfo = new TableColumnInfo(columnName, dataType);
            columnInfoList.add(columnInfo);
        }

        return columnInfoList;
    }

    /**
     * 获取表的 DDL 信息
     *
     * @param tableName
     * @return
     * @throws SQLException
     */
    public String getTableDDL(String databaseName, String tableName) throws SQLException, ClassNotFoundException {
        String sql = "SHOW CREATE TABLE " + tableName;
        ResultSet tableResultSet = getStatement(databaseName).executeQuery(sql);

        String tableDDL = "";
        while (tableResultSet.next()) {
            tableDDL = StringHelper.mergedIntoOneLine(tableResultSet.getString("Create Table"));
        }

        return tableDDL;
    }

    /**
     * 获取所有的 table
     *
     * @param databaseName
     * @return
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    public ArrayList<String> getAllTable(String databaseName) throws SQLException, ClassNotFoundException {
        String sql = "SHOW TABLES";
        ResultSet tableResultSet = getStatement(databaseName).executeQuery(sql);

        ArrayList<String> tables = new ArrayList<String>();
        while (tableResultSet.next()) {
            tables.add(tableResultSet.getString("Tables_in_" + databaseName));
        }

        return tables;
    }

    /**
     * 关闭数据库资源
     */
    public void close() {
        try {
            // 关闭数据库连接
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            // 处理关闭连接时可能出现的异常
            e.printStackTrace();
        }
    }

    /**
     * 获取数据库连接
     *
     * @return
     * @throws SQLException
     */
    private Statement getStatement(String database) throws SQLException, ClassNotFoundException {
        return getConnection(database).createStatement();
    }

    /**
     * 获取数据库连接
     *
     * @param database
     * @return
     * @throws SQLException
     */
    private Connection getConnection(String database) throws SQLException, ClassNotFoundException {
        String url = "jdbc:mysql://" + settings.getOriginalMysqlIp() + ":" + settings.getOriginalMysqlPort() + "/" + database;
        String username = settings.getUsername();
        String password = PersistingSensitiveDataService.getPassword();
        if (Objects.equals(database, "") || Objects.equals(username, "")) {
            throw new RuntimeException("database is empty or username is empty");
        }

        Class.forName("com.mysql.cj.jdbc.Driver");
        connection = DriverManager.getConnection(url, username, password);

        return connection;
    }
}
