package com.ls.akong.mysql_proxy.services;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.ls.akong.mysql_proxy.model.SqlLogFilterModel;
import com.ls.akong.mysql_proxy.model.SqlLogModel;
import org.h2.jdbcx.JdbcConnectionPool;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@Service(Service.Level.PROJECT)
public final class DatabaseManagerService {
    private static final Logger logger = Logger.getInstance(DatabaseManagerService.class);

    // 如果表结构不能先后兼容，可以改变这个值，重新建一个数据库、表
    private final int version = 1;
    private Connection connection;

    public DatabaseManagerService(Project project) {
        try {
            String pluginDataDirPath = PathManager.getPluginsPath() + File.separator + "sql_proxy" + File.separator + project.getName() + File.separator;
            File pluginDataDir = new File(pluginDataDirPath);
            if (!pluginDataDir.exists()) {
                logger.info("create H2 database dir: " + pluginDataDirPath);
                if (!pluginDataDir.mkdirs()) {
                    logger.error("Failed to create folder: " + pluginDataDirPath);
                    Messages.showErrorDialog("Failed to create folder: " + pluginDataDirPath, "Error Creating");
                }
            }

            String dbFilePath = pluginDataDirPath + version + "_h2database";
            logger.info("H2 database file: " + dbFilePath);

            // 创建H2数据库的内置连接池
            JdbcConnectionPool connectionPool = JdbcConnectionPool.create("jdbc:h2:" + dbFilePath, "", "");

            // 从连接池获取连接
            connection = connectionPool.getConnection();
            createTableIfNotExists(); // 创建表（如果不存在）
        } catch (SQLException e) {
            logger.error("H2 database initialization fail " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Connection getConnection() {
        return connection;
    }

    // 首次执行的时候，如果不存在表，则创建表
    private void createTableIfNotExists() {
        String createSqlLogTable = SqlLogModel.getCreateTableSql();
        String createSqlLogFilterTable = SqlLogFilterModel.getCreateTableSql();

        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(createSqlLogTable);
            statement.executeUpdate(createSqlLogFilterTable);
            logger.info("exec create table SQL success");
        } catch (SQLException e) {
            logger.error("exec create table SQL fail " + e.getMessage());
            e.printStackTrace();
        }
    }
}
