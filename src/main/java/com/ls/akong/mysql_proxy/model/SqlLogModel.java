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
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class SqlLogModel {
    private static final Logger logger = Logger.getInstance(SqlLogModel.class);
    private static final int INSERT_INTERVAL_MS = 100;

    private static final int MAX_PAGE_SIZE = 100000;

    private static Map<String, BlockingQueue<String>> projectLogQueues = new HashMap<>();
    private static Map<String, Thread> logInsertThreads = new HashMap<>();

    /**
     * 使用队列的方式保存 sql
     *
     * @param project
     * @param sql
     */
    public static void insertLogByQueue(Project project, String sql) {
        // Get the project name
        String projectName = project.getName();

        // Check if a queue for this project already exists
        BlockingQueue<String> logQueue = projectLogQueues.get(projectName);

        if (logQueue == null) {
            // Create a new queue for this project
            logQueue = new LinkedBlockingQueue<>();
            projectLogQueues.put(projectName, logQueue);

            // Start a new thread for this project
            BlockingQueue<String> finalLogQueue = logQueue;
            Thread logInsertThread = new Thread(() -> {
                while (true) {
                    try {
                        // Get the log message from the queue and insert it into the database
                        String logMessage = finalLogQueue.poll(INSERT_INTERVAL_MS, TimeUnit.MILLISECONDS);
                        if (logMessage != null) {
                            insertLogIntoDatabase(project, logMessage);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
            logInsertThread.start();

            // Store the thread in the map for future use
            logInsertThreads.put(projectName, logInsertThread);
        }

        // Add the log message to the project's queue
        logQueue.offer(sql);
    }

    private static void insertLogIntoDatabase(Project project, String sql) {
        String insertSQL = "INSERT INTO " + SqlLog.getTableName() + " (sql, created_at) VALUES (?, ?)";
        DatabaseManagerService databaseManager = project.getService(DatabaseManagerService.class);

        try {
            PreparedStatement preparedStatement = databaseManager.getConnection().prepareStatement(insertSQL);
            preparedStatement.setString(1, sql);
            preparedStatement.setLong(2, System.currentTimeMillis());
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static List<SqlLog> queryLogs(Project project, String searchText, String selectedTimeRange, String sqlType, int maxLimitId, int minLimitId, int pageSize) {
        List<SqlLog> logEntries = new ArrayList<>();
        String querySQL = "SELECT * FROM " + SqlLog.getTableName() + " WHERE 1 ";
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

        if (!Objects.equals(sqlType, "All")) {
            switch (sqlType) {
                case "Select":
                    querySQL += " AND (`sql` LIKE '%SELECT%') ";
                    break;
                case "Update":
                    querySQL += " AND (`sql` LIKE '%UPDATE%') ";
                    break;
                case "Delete":
                    querySQL += " AND (`sql` LIKE '%DELETE%') ";
                    break;
                case "Other":
                    querySQL += " AND (`sql` NOT LIKE '%SELECT%' AND `sql` NOT LIKE '%UPDATE%' AND `sql` NOT LIKE '%DELETE%') ";
                    break;
            }
        }

        querySQL += " AND sql NOT IN (SELECT sql FROM " + SqlLogFilter.getTableName() + ") ORDER BY id DESC";

        // 非前增、时间搜索的，才用分页。但是不做分页，也要限制最大量，防止卡死
        if (minLimitId > 0 || !selectedTimeRange.equals("No Limit")) {
            pageSize = MAX_PAGE_SIZE;
        }
        querySQL += " LIMIT 0," + pageSize;
        logger.info("sql: " + querySQL);

        DatabaseManagerService databaseManager = project.getService(DatabaseManagerService.class);
        try (PreparedStatement preparedStatement = databaseManager.getConnection().prepareStatement(querySQL);
             ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String sql = resultSet.getString("sql");
                long createdAt = resultSet.getLong("created_at");
                long executionTime = resultSet.getLong("execution_time");
                logEntries.add(new SqlLog(id, sql, createdAt, executionTime));
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

                    return new SqlLog(recordId, sql, createdAt, executionTime); // 创建并返回 SqlLog 对象
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
        return "CREATE TABLE IF NOT EXISTS " + SqlLog.getTableName() + " (id INT AUTO_INCREMENT PRIMARY KEY, sql CLOB,execution_time BIGINT, created_at BIGINT)";
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

    public static int insertLog(Project project, String sql) {
        String insertSQL = "INSERT INTO " + SqlLog.getTableName() + " (sql, created_at) VALUES (?, ?)";
        DatabaseManagerService databaseManager = project.getService(DatabaseManagerService.class);

        try {
            PreparedStatement preparedStatement = databaseManager.getConnection().prepareStatement(insertSQL, Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setString(1, sql);
            preparedStatement.setLong(2, System.currentTimeMillis());
            preparedStatement.executeUpdate();

            // 获取生成的键
            ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
            if (generatedKeys.next()) {
                int generatedId = generatedKeys.getInt(1); // 假设生成的键是一个整型
                return generatedId;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return -1;
    }

    /**
     * 更新执行时间
     *
     * @param project
     * @param id
     * @param executionTime
     * @return
     */
    public static boolean updateExecutionTime(Project project, long id, long executionTime) {
        // SQL UPDATE语句
        String updateSQL = "UPDATE " + SqlLog.getTableName() + " SET execution_time = ? WHERE id = ?";
        DatabaseManagerService databaseManager = project.getService(DatabaseManagerService.class);

        try {
            PreparedStatement preparedStatement = databaseManager.getConnection().prepareStatement(updateSQL);
            // 设置参数
            preparedStatement.setLong(1, executionTime);
            preparedStatement.setLong(2, id);

            // 执行UPDATE语句
            int rowsUpdated = preparedStatement.executeUpdate();

            if (rowsUpdated == 0) {
                return false;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return true;
    }
}
