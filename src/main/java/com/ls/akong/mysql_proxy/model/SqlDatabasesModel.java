package com.ls.akong.mysql_proxy.model;

import com.intellij.openapi.project.Project;
import com.ls.akong.mysql_proxy.entity.SqlDatabases;
import com.ls.akong.mysql_proxy.services.DatabaseManagerService;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class SqlDatabasesModel {
    /**
     * 通过 database name 查询 id，如果不存在会新增
     *
     * @param project
     * @param databaseName
     * @return
     */
    public static int getDatabasesIdByName(Project project, String databaseName) {
        // 假如已经存在
        SqlDatabases existingDatabase = getByDatabaseName(project, databaseName);
        if (existingDatabase != null) {
            return existingDatabase.getId();
        }

        // 不存在则新增
        return insertSqlDatabase(project, databaseName);
    }

    /**
     * 新增过滤 sql
     *
     * @param project
     * @param databaseName
     */
    public static int insertSqlDatabase(Project project, String databaseName) {
        // 不存在，则新增
        String insertSQL = "INSERT INTO " + SqlDatabases.getTableName() + " (database_name, created_at) VALUES (?,?)";

        DatabaseManagerService databaseManager = project.getService(DatabaseManagerService.class);
        try (PreparedStatement preparedStatement = databaseManager.getConnection().prepareStatement(insertSQL, Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setString(1, databaseName);
            preparedStatement.setLong(2, System.currentTimeMillis());
            preparedStatement.executeUpdate();

            // 获取插入的 ID
            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Inserting record failed; no ID obtained.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return -1; // 或者抛出异常以表示插入失败
    }

    /**
     * 通过名字查询
     *
     * @param project
     * @param databaseName
     * @return
     */
    public static SqlDatabases getByDatabaseName(Project project, String databaseName) {
        String querySQL = "SELECT * FROM " + SqlDatabases.getTableName() + " WHERE database_name=? ";

        DatabaseManagerService databaseManager = project.getService(DatabaseManagerService.class);
        try (PreparedStatement preparedStatement = databaseManager.getConnection().prepareStatement(querySQL)) {
            preparedStatement.setString(1, databaseName);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    int recordId = resultSet.getInt("id");
                    String databaseName2 = resultSet.getString("database_name");
                    long createdAt = resultSet.getLong("created_at");

                    return new SqlDatabases(recordId, databaseName2, createdAt); // 创建并返回对象
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null; // 未找到记录，返回 null 或者根据需求进行错误处理
    }

    /**
     * 建表 SQL
     */
    public static String getCreateTableSql() {
        return "CREATE TABLE IF NOT EXISTS " + SqlDatabases.getTableName()
                + " (id INT AUTO_INCREMENT PRIMARY KEY,database_name VARCHAR(50), created_at BIGINT)";
    }

    public static SqlDatabases getById(Project project, int id) {
        String querySQL = "SELECT * FROM " + SqlDatabases.getTableName() + " WHERE id=? ";

        DatabaseManagerService databaseManager = project.getService(DatabaseManagerService.class);
        try (PreparedStatement preparedStatement = databaseManager.getConnection().prepareStatement(querySQL)) {
            preparedStatement.setInt(1, id);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    int recordId = resultSet.getInt("id");
                    String databaseName = resultSet.getString("database_name");
                    long createdAt = resultSet.getLong("created_at");

                    return new SqlDatabases(recordId, databaseName, createdAt); // 创建并返回对象
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null; // 未找到记录，返回 null 或者根据需求进行错误处理
    }
}
