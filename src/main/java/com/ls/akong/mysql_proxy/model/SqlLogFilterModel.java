package com.ls.akong.mysql_proxy.model;

import com.intellij.openapi.project.Project;
import com.ls.akong.mysql_proxy.entity.SqlLogFilter;
import com.ls.akong.mysql_proxy.services.DatabaseManagerService;
import com.ls.akong.mysql_proxy.util.SQLFingerprintGenerator;

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
     * @param signature
     * @return
     */
    public static boolean isSqlLogExistsBySignature(Project project, String signature) {
        String checkSQL = "SELECT COUNT(*) FROM " + SqlLogFilter.getTableName() + " WHERE signature = ?";

        DatabaseManagerService databaseManager = project.getService(DatabaseManagerService.class);
        try (PreparedStatement preparedStatement = databaseManager.getConnection().prepareStatement(checkSQL)) {
            preparedStatement.setString(1, signature);
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

    /**
     * 新增过滤 sql
     *
     * @param project
     * @param sql
     */
    public static void insertLogFilter(Project project, String sql) {
        String sqlFinger = SQLFingerprintGenerator.generateFingerprint(sql);
        String signature = SQLFingerprintGenerator.getSignature(sqlFinger);
        // 检查是否已存在这条 sql，如果已经存在，则不操作
        if (isSqlLogExistsBySignature(project, signature)) {
            return;
        }

        // 不存在，则新增
        String insertSQL = "INSERT INTO " + SqlLogFilter.getTableName() + " (sql_finger, signature, created_at) VALUES (?,?,?)";

        DatabaseManagerService databaseManager = project.getService(DatabaseManagerService.class);
        try (PreparedStatement preparedStatement = databaseManager.getConnection().prepareStatement(insertSQL)) {
            preparedStatement.setString(1, sqlFinger);
            preparedStatement.setString(2, signature);
            preparedStatement.setLong(3, System.currentTimeMillis());
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 过滤 sql 列表
     *
     * @param project
     * @return
     */
    public static List<SqlLogFilter> querySqlLogFilter(Project project) {
        List<SqlLogFilter> logFilterList = new ArrayList<>();
        String querySQL = "SELECT * FROM " + SqlLogFilter.getTableName() + " ORDER BY id DESC";

        DatabaseManagerService databaseManager = project.getService(DatabaseManagerService.class);
        try (PreparedStatement preparedStatement = databaseManager.getConnection().prepareStatement(querySQL);
             ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String sqlFinger = resultSet.getString("sql_finger");
                long createdAt = resultSet.getLong("created_at");
                logFilterList.add(new SqlLogFilter(id, sqlFinger, createdAt));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return logFilterList;
    }

    /**
     * 删除制定过滤 sql
     *
     * @param project
     * @param id
     */
    public static void deleteDataById(Project project, int id) {
        String sql = "DELETE FROM " + SqlLogFilter.getTableName() + " WHERE id = ?";

        DatabaseManagerService databaseManager = project.getService(DatabaseManagerService.class);
        try (PreparedStatement preparedStatement = databaseManager.getConnection().prepareStatement(sql)) {
            preparedStatement.setInt(1, id);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 更新制定过滤 sql 的值
     *
     * @param project
     * @param id
     * @param newSql
     */
    public static void updateDataById(Project project, int id, String newSql) {
        String sql = "UPDATE " + SqlLogFilter.getTableName() + " SET sql_finger = ?,signature=? WHERE id = ?";

        DatabaseManagerService databaseManager = project.getService(DatabaseManagerService.class);
        try (PreparedStatement preparedStatement = databaseManager.getConnection().prepareStatement(sql)) {
            String sqlFinger = SQLFingerprintGenerator.generateFingerprint(newSql);
            preparedStatement.setString(1, sqlFinger);
            preparedStatement.setString(2, SQLFingerprintGenerator.getSignature(sqlFinger));
            preparedStatement.setInt(3, id);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 建表 SQL
     */
    public static String getCreateTableSql() {
        return "CREATE TABLE IF NOT EXISTS " + SqlLogFilter.getTableName() + " (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "sql_finger CLOB, " +
                "signature char(16), " +
                "created_at BIGINT)";
    }
}
