package com.ls.akong.mysql_proxy.model;

import com.intellij.openapi.project.Project;
import com.ls.akong.mysql_proxy.entity.SqlLogFilter;
import com.ls.akong.mysql_proxy.services.DatabaseManagerService;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SqlLogFilterModel {

    /**
     * 检查 sql 是否已存在
     *
     * @param project
     * @param sql
     * @return
     */
    public static boolean isSqlLogExists(Project project, String sql) {
        String checkSQL = "SELECT COUNT(*) FROM sql_log_filter WHERE sql = ?";

        DatabaseManagerService databaseManager = project.getService(DatabaseManagerService.class);
        try (PreparedStatement preparedStatement = databaseManager.getConnection().prepareStatement(checkSQL)) {
            preparedStatement.setString(1, sql);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                int count = resultSet.getInt(1);
                return count > 0; // 如果 count 大于 0，表示已存在相同的 SQL
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false; // 查询出错或者没有匹配的记录
    }

    public static void insertLogFilter(Project project, String sql) {
        // 检查是否已存在这条 sql，如果已经存在，则不操作
        if (isSqlLogExists(project, sql)) {
            return;
        }

        // 不存在，则新增
        String insertSQL = "INSERT INTO sql_log_filter (sql, created_at) VALUES (?, ?)";

        DatabaseManagerService databaseManager = project.getService(DatabaseManagerService.class);
        try (PreparedStatement preparedStatement = databaseManager.getConnection().prepareStatement(insertSQL)) {
            preparedStatement.setString(1, sql);
            preparedStatement.setLong(2, System.currentTimeMillis());
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static List<SqlLogFilter> querySqlLogFilter(Project project) {
        List<SqlLogFilter> logEntries = new ArrayList<>();
        String querySQL = "SELECT * FROM sql_log_filter ORDER BY id DESC";

        DatabaseManagerService databaseManager = project.getService(DatabaseManagerService.class);
        try (PreparedStatement preparedStatement = databaseManager.getConnection().prepareStatement(querySQL);
             ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String sql = resultSet.getString("sql");
                long createdAt = resultSet.getLong("created_at");
                logEntries.add(new SqlLogFilter(id, sql, createdAt));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return logEntries;
    }

    public static void deleteDataById(Project project, int id) {
        String sql = "DELETE FROM sql_log_filter WHERE id = ?";

        DatabaseManagerService databaseManager = project.getService(DatabaseManagerService.class);
        try (PreparedStatement preparedStatement = databaseManager.getConnection().prepareStatement(sql)) {
            preparedStatement.setInt(1, id);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void updateDataById(Project project, int id, String newSql) {
        String sql = "UPDATE sql_log_filter SET sql = ? WHERE id = ?";

        DatabaseManagerService databaseManager = project.getService(DatabaseManagerService.class);
        try (PreparedStatement preparedStatement = databaseManager.getConnection().prepareStatement(sql)) {
            preparedStatement.setString(1, newSql);
            preparedStatement.setInt(2, id);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 建表 SQL
     */
    public static String getCreateTableSql() {
        return "CREATE TABLE IF NOT EXISTS sql_log_filter (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "sql CLOB, " +
                "created_at BIGINT)";
    }
}
