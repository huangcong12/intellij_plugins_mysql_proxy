package com.ls.akong.mysql_proxy.services;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.ls.akong.mysql_proxy.model.SqlLogFilterModel;
import com.ls.akong.mysql_proxy.model.SqlLogModel;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

@Service(Service.Level.PROJECT)
public final class DatabaseManagerService implements Disposable {
    private static final Logger logger = Logger.getInstance(DatabaseManagerService.class);

    private Project project;
    private Connection connection;

    public DatabaseManagerService(@NotNull Project project) {
        this.project = project;

        try {
            // TODO 开发过程中，换成非插件的目录
//            String pluginDataDirPath = PathManager.getPluginsPath() + File.separator + "sql_proxy";
            String pluginDataDirPath = "sql_proxy" + File.separator + project.getName() + File.separator;
            File pluginDataDir = new File(pluginDataDirPath);
            if (!pluginDataDir.exists()) {
                logger.info("create sqlite dir: " + pluginDataDirPath);
                if (!pluginDataDir.mkdirs()) {
                    logger.error("Failed to create folder: " + pluginDataDirPath);
                    Messages.showErrorDialog("Failed to create folder: " + pluginDataDirPath, "Error Creating");
                }
            }

            String dbFilePath = pluginDataDirPath + File.separator + "database.db";
            logger.info("db file: " + dbFilePath);

            // 加载 SQLite 驱动
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFilePath);
            createTableIfNotExists();   // 创建表（如果不存在）
        } catch (ClassNotFoundException | SQLException e) {
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
                logger.info("close sqlite connection success");
            }
        } catch (SQLException e) {
            logger.error("close sqlite connection fail " + e.getMessage());
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
            logger.info("exec create table sql success");
        } catch (SQLException e) {
            logger.error("exec create table sql fail " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * shutdown hook 关闭数据库连接
     */
    @Override
    public void dispose() {
        logger.info("closing sqlite connection");
        close();
    }
}
