package com.ls.akong.mysql_proxy.model;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.ls.akong.mysql_proxy.entity.SqlLog;
import com.ls.akong.mysql_proxy.entity.SqlLogFilter;
import com.ls.akong.mysql_proxy.services.DatabaseManagerService;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Objects;

public class SqlLogModel {
    private static final Logger logger = Logger.getInstance(SqlLogModel.class);
    private static final int MAX_PAGE_SIZE = 100000;

    /**
     * 查询 sql log 内容，TableView 展示数据用
     *
     * @param project
     * @param searchText
     * @param selectedTimeRange
     * @param sqlType
     * @param maxLimitId
     * @param minLimitId
     * @param pageSize
     * @return
     */
    public static ArrayList<SqlLog> queryLogs(Project project, String searchText, String durationFilter, int selectedTimeRange, String sqlType, int maxLimitId, int minLimitId, int pageSize) {
        ArrayList<SqlLog> logEntries = new ArrayList<>();
        String querySQL = "SELECT * FROM " + SqlLog.getTableName() + " WHERE 1 ";
        // 条件搜索
        if (!Objects.equals(searchText, "") && searchText.length() > 0) {
            querySQL += " AND sql LIKE '%" + searchText + "%'";
        }
        if (durationFilter != "") {
            querySQL += " AND execution_time" + durationFilter;
        }

        // Time range conditions.如果有时间段条件搜索，则查询所有，要不然会有过时间了，还在的 bug
        if (selectedTimeRange > 0) {
            // Append the time range condition to the query
            long startTimeMillis = System.currentTimeMillis() - selectedTimeRange;
            querySQL += " AND created_at >= " + startTimeMillis;
        } else {
            if (maxLimitId > 0) {           // 分页
                querySQL += " AND id<" + maxLimitId;
            } else if (minLimitId > 0) {    // 前补
                querySQL += " AND id>" + minLimitId;
            }
        }

        if (!Objects.equals(sqlType, "All")) {
            switch (sqlType) {
                case "Other":
                    querySQL += " AND (LOWER(sql) NOT LIKE '%select %' AND LOWER(sql) NOT LIKE '%insert %' AND LOWER(sql) NOT LIKE '%update %' AND LOWER(sql) NOT LIKE '%delete %') ";
                    break;
                default:
                    querySQL += " AND (LOWER(sql) LIKE '%" + sqlType.toLowerCase() + " %') ";
                    break;
            }
        }

        querySQL += " AND signature NOT IN (SELECT signature FROM " + SqlLogFilter.getTableName() + ") ORDER BY id DESC";

        // 非前增、时间搜索的，才用分页。但是不做分页，也要限制最大量，防止卡死
        if (minLimitId > 0 || selectedTimeRange > 0) {
            pageSize = MAX_PAGE_SIZE;
        }
        querySQL += " LIMIT 0," + pageSize;
        logger.info("sql: " + querySQL);

        DatabaseManagerService databaseManager = project.getService(DatabaseManagerService.class);
        try (PreparedStatement preparedStatement = databaseManager.getConnection().prepareStatement(querySQL); ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String sql = resultSet.getString("sql");
                long createdAt = resultSet.getLong("created_at");
                long executionTime = resultSet.getLong("execution_time");
                String signature = resultSet.getString("signature");
                int sqlDatabasesId = resultSet.getInt("sql_databases_id");

                logEntries.add(new SqlLog(id, sql, createdAt, executionTime, signature, sqlDatabasesId));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return logEntries;
    }

    public static SqlLog getById(Project project, int id) {
        String querySQL = "SELECT * FROM " + SqlLog.getTableName() + " WHERE id=? ";

        DatabaseManagerService databaseManager = project.getService(DatabaseManagerService.class);
        try (PreparedStatement preparedStatement = databaseManager.getConnection().prepareStatement(querySQL)) {
            preparedStatement.setInt(1, id);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    int recordId = resultSet.getInt("id");
                    String sql = resultSet.getString("sql");
                    long createdAt = resultSet.getLong("created_at");
                    long executionTime = resultSet.getLong("execution_time");
                    String signature = resultSet.getString("signature");
                    int sqlDatabasesId = resultSet.getInt("sql_databases_id");

                    return new SqlLog(recordId, sql, createdAt, executionTime, signature, sqlDatabasesId); // 创建并返回 SqlLog 对象
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null; // 未找到记录，返回 null 或者根据需求进行错误处理
    }

    // 重置 sql log 表，并且 id 从 1 开始
    public static void truncateSqlLog(Project project) {
        String truncateSQL = "TRUNCATE TABLE " + SqlLog.getTableName() + " RESTART IDENTITY";

        DatabaseManagerService databaseManager = project.getService(DatabaseManagerService.class);
        try (Statement statement = databaseManager.getConnection().createStatement()) {
            statement.executeUpdate(truncateSQL);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 建表 SQL
     */
    public static String getCreateTableSql() {
        return "CREATE TABLE IF NOT EXISTS " + SqlLog.getTableName()
                + " (id INT AUTO_INCREMENT PRIMARY KEY, sql CLOB,execution_time BIGINT,signature char(16),sql_databases_id INT, created_at BIGINT)";
    }

    public static void deleteDataById(Project project, int id) {
        String sql = "DELETE FROM " + SqlLog.getTableName() + " WHERE id = ?";

        DatabaseManagerService databaseManager = project.getService(DatabaseManagerService.class);
        try (PreparedStatement preparedStatement = databaseManager.getConnection().prepareStatement(sql)) {
            preparedStatement.setInt(1, id);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 一条一条新增 sql log 到表里
     *
     * @param project
     * @param sql
     * @param executionTime
     * @return
     */
    public static int insertLog(Project project, String sql, long executionTime, String signature, int sqlDatabaseId) {
        String insertSQL = "INSERT INTO " + SqlLog.getTableName() + " (sql,execution_time,signature,sql_databases_id,created_at) VALUES (?,?,?,?,?)";
        DatabaseManagerService databaseManager = project.getService(DatabaseManagerService.class);

        try {
            PreparedStatement preparedStatement = databaseManager.getConnection().prepareStatement(insertSQL, Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setString(1, sql);
            preparedStatement.setLong(2, executionTime);
            preparedStatement.setString(3, signature);
            preparedStatement.setLong(4, sqlDatabaseId);
            preparedStatement.setLong(5, System.currentTimeMillis());
            preparedStatement.executeUpdate();

            // 获取生成的键
            ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
            if (generatedKeys.next()) {
                return generatedKeys.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return -1;
    }
}
