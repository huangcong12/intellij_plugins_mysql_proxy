package com.ls.akong.mysql_proxy.services;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.ls.akong.mysql_proxy.model.SqlLogFilterModel;
import com.ls.akong.mysql_proxy.model.SqlLogModel;
import org.jetbrains.annotations.NotNull;
import org.h2.jdbcx.JdbcDataSource;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@Service(Service.Level.PROJECT)
public final class DatabaseManagerService implements Disposable {
    private static final Logger logger = Logger.getInstance(DatabaseManagerService.class);

    private Connection connection;

    public DatabaseManagerService(@NotNull Project project) {

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

            String dbFilePath = pluginDataDirPath + File.separator + "h2database";
            logger.info("H2 database file: " + dbFilePath);

            // 使用 H2 数据库的 JDBC DataSource 进行连接
            JdbcDataSource dataSource = new JdbcDataSource();
            dataSource.setURL("jdbc:h2:" + dbFilePath);
            connection = dataSource.getConnection();
            createTableIfNotExists();   // 创建表（如果不存在）
        } catch (SQLException e) {
            logger.error("H2 database initialization fail " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public void close() {
        try {
            if (connection != null) {
                connection.close();
                logger.info("close H2 database connection success");
            }
        } catch (SQLException e) {
            logger.error("close H2 database connection fail " + e.getMessage());
            e.printStackTrace();
        }
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

    /**
     * shutdown hook 关闭数据库连接
     */
    @Override
    public void dispose() {
        logger.info("closing H2 database connection");
        close();
    }
}
