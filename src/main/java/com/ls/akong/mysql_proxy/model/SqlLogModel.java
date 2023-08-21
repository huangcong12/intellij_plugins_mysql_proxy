package com.ls.akong.mysql_proxy.model;

import com.intellij.openapi.project.Project;
import com.ls.akong.mysql_proxy.entity.SqlLog;
import com.ls.akong.mysql_proxy.services.DatabaseManagerService;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

public class SqlLogModel {
    private static Timer debounceTimer = new Timer();

    private static List<String> newLog = new ArrayList<>();

    public static void insertLog(Project project, String sql) {
        newLog.add(sql);

        // 取消之前的定时任务
        debounceTimer.cancel();

        // 创建一个新的定时任务，在100ms后执行通知操作
        debounceTimer = new Timer();
        debounceTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                List<String> newLogCopy = new ArrayList<>(newLog);
                newLog.clear();

                String insertSQL = "INSERT INTO sql_log (sql, created_at) VALUES (?, ?)";

                DatabaseManagerService databaseManager = project.getService(DatabaseManagerService.class);
                try {
                    PreparedStatement preparedStatement = databaseManager.getConnection().prepareStatement(insertSQL);
                    for (String sql : newLogCopy) {
                        preparedStatement.setString(1, sql);
                        preparedStatement.setDouble(2, System.currentTimeMillis());
                        preparedStatement.addBatch();
                    }
                    preparedStatement.executeBatch();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }, 100); // 在100ms后执行
    }

//    public void updateLog(int id, String newStatus, int newSpendTime) {
//        String updateSQL = "UPDATE sql_log SET status = ?, spend_time = ? WHERE id = ?";
//
//        try (PreparedStatement preparedStatement = DatabaseManager.getConnection().prepareStatement(updateSQL)) {
//            preparedStatement.setString(1, newStatus);
//            preparedStatement.setInt(2, newSpendTime);
//            preparedStatement.setInt(3, id);
//            preparedStatement.executeUpdate();
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//    }

    public static List<SqlLog> queryLogs(Project project, String searchText, String selectedTimeRange, int maxLimitId, int minLimitId, int pageSize) {
        List<SqlLog> logEntries = new ArrayList<>();
        String querySQL = "SELECT * FROM sql_log WHERE 1 ";
        // 条件搜索
        if (!Objects.equals(searchText, "") && searchText.length() > 0) {
            querySQL += " AND sql LIKE '%" + searchText + "%'";
        }

        // Time range conditions.如果有时间段条件搜索，则查询所有，要不然会有过时间了，还在的 bug
        if (!selectedTimeRange.equals("No Limit")) {
            long timeLimitMillis;
            switch (selectedTimeRange) {
                case "Within 10s":
                    timeLimitMillis = 10000; // 10 seconds in milliseconds
                    break;
                case "Within 1m":
                    timeLimitMillis = 60000; // 1 minute in milliseconds
                    break;
                case "Within 5m":
                    timeLimitMillis = 300000; // 5 minute in milliseconds
                    break;
                case "Within 10m":
                    timeLimitMillis = 600000; // 10 minute in milliseconds
                    break;
                default:
                    timeLimitMillis = 0; // No time limit
                    break;
            }

            // Append the time range condition to the query
            if (timeLimitMillis > 0) {
                long startTimeMillis = System.currentTimeMillis() - timeLimitMillis;
                querySQL += " AND created_at >= " + startTimeMillis;
            }
        } else {
            if (maxLimitId > 0) {           // 分页
                querySQL += " AND id<" + maxLimitId;
            } else if (minLimitId > 0) {    // 前补
                querySQL += " AND id>" + minLimitId;
            }
        }

        querySQL += " AND sql NOT IN (SELECT sql FROM sql_log_filter)";

        querySQL += " ORDER BY id DESC";

        // 不是前增、时间搜索的，才用分页
        if (maxLimitId > 0 || selectedTimeRange.equals("No Limit")) {
            querySQL += " LIMIT 0," + pageSize;
        }


        System.out.println("sql: " + querySQL);

        DatabaseManagerService databaseManger = project.getService(DatabaseManagerService.class);
        try (Statement statement = databaseManger.getConnection().createStatement(); ResultSet resultSet = statement.executeQuery(querySQL)) {
            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String sql = resultSet.getString("sql");
                long createdAt = resultSet.getLong("created_at");
                logEntries.add(new SqlLog(id, sql, createdAt));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return logEntries;
    }

    public static SqlLog getById(Project project, int id, String searchText) {
        String insertSQL = "SELECT * FROM sql_log WHERE id=? ";
        if (!Objects.equals(searchText, "")) {
            insertSQL += " AND sql LIKE '%" + searchText + "%'";
        }
        insertSQL += "AND sql NOT IN (SELECT sql FROM sql_log_filter)";
        System.out.println("getById sql: " + insertSQL);

        DatabaseManagerService databaseManger = project.getService(DatabaseManagerService.class);
        try (PreparedStatement preparedStatement = databaseManger.getConnection().prepareStatement(insertSQL)) {
            preparedStatement.setInt(1, id);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    int recordId = resultSet.getInt("id");
                    String sql = resultSet.getString("sql");
                    long createdAt = resultSet.getLong("created_at");

                    return new SqlLog(recordId, sql, createdAt); // 创建并返回 SqlLog 对象
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null; // 未找到记录，返回 null 或者根据需求进行错误处理
    }

    // 重置 sql log 表，并且 id 从 1 开始
    public static void truncateSqlLog(Project project) {
        String truncateSQL = "DELETE FROM sql_log;UPDATE sqlite_sequence SET seq = 0 WHERE name = 'sql_log'";

        DatabaseManagerService databaseManger = project.getService(DatabaseManagerService.class);
        try (Statement statement = databaseManger.getConnection().createStatement()) {
            statement.executeUpdate(truncateSQL);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 建表 SQL
     */
    public static String getCreateTableSql() {
        return "CREATE TABLE IF NOT EXISTS sql_log (id INTEGER PRIMARY KEY AUTOINCREMENT,sql TEXT,created_at REAL)";
    }

    public static void deleteDataById(Project project, int id) {
        String sql = "DELETE FROM sql_log WHERE id = ?";

        DatabaseManagerService databaseManger = project.getService(DatabaseManagerService.class);
        try (PreparedStatement preparedStatement = databaseManger.getConnection().prepareStatement(sql)) {
            preparedStatement.setInt(1, id);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
