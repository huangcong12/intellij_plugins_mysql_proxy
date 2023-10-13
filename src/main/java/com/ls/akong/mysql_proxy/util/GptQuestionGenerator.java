package com.ls.akong.mysql_proxy.util;

import com.github.mnadeem.TableNameParser;
import com.intellij.openapi.project.Project;
import com.ls.akong.mysql_proxy.services.MysqlProxySettings;
import com.ls.akong.mysql_proxy.services.NotificationsService;
import com.ls.akong.mysql_proxy.services.PersistingSensitiveData;

import java.sql.*;
import java.util.Collection;
import java.util.Locale;
import java.util.Objects;

/**
 * 生成 GPT 的问题
 */
public class GptQuestionGenerator {
    private final String sql;

    private Project project;

    private MysqlProxySettings.State state;

    public GptQuestionGenerator(Project project, String sql) {
        this.project = project;
        this.sql = sql;
        this.state = MysqlProxySettings.getInstance(project).getState();
    }

    public Collection<String> getTableNames(String sql) {
        return new TableNameParser(sql).tables();
    }

    /**
     * 生成问题
     *
     * @return
     */
    public String getQuestion() {
        String question = "I am a developer, and you are an experienced DBA. Can you assist me in improving the performance of this SQL? Here's the SQL: \n```\n" + SQLFingerprintGenerator.generateFingerprint(sql) + "\n```\n";

        try {
            Statement statement = getStatement();
            // 1、获取 explain 信息
            String explainSql = "explain format=json " + sql;
            ResultSet explainResultSet = statement.executeQuery(explainSql);
            String explainFormatJson = "";
            while (explainResultSet.next()) {
                explainFormatJson = StringHelper.mergedIntoOneLine(explainResultSet.getString("EXPLAIN"));
            }
            // 把 Explain format json 信息放进去
            question += "The EXPLAIN FORMAT JSON information for this SQL query:\n```\n" + explainFormatJson + "\n```\n";

            // 2、获取 sql 涉及到的表
            Collection<String> tableNames = getTableNames(sql);
            StringBuilder showCreateTableInfo = new StringBuilder();
            for (String tableName : tableNames) {
                ResultSet tableResultSet = statement.executeQuery("SHOW CREATE TABLE " + tableName);
                while (tableResultSet.next()) {
                    if (!showCreateTableInfo.toString().equals("")) {
                        showCreateTableInfo.append("\n");
                    }
                    showCreateTableInfo.append(StringHelper.mergedIntoOneLine(tableResultSet.getString("Create Table")));
                }
            }

            // 把表 DDL 信息放进去
            question += ("The DDL information related to this SQL query for the table:\n```\n" + showCreateTableInfo + "\n```\n");

        } catch (SQLException | ClassNotFoundException | RuntimeException e) {
            // 发送通知
            NotificationsService.notifyError(project, e.getMessage());
        }

        // 3、获取默认的系统区域设置，让 GPT 按照区域的母语回答
        Locale defaultLocale = Locale.getDefault();
        String systemLanguage = defaultLocale.getLanguage();
        if (!systemLanguage.equals("")) {
            question += "Please answer me in " + systemLanguage;
        }

        return question;
    }

    /**
     * 获取数据库连接
     *
     * @return
     * @throws SQLException
     */
    private Statement getStatement() throws SQLException, ClassNotFoundException {
        String database = state.database;

        String url = "jdbc:mysql://" + state.originalMysqlIp + ":" + state.originalMysqlPort + "/" + database;
        String username = state.username;
        String password = PersistingSensitiveData.getPassword();
        if (Objects.equals(database, "") || Objects.equals(username, "")) {
            throw new RuntimeException("database is empty or username is empty");
        }

        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection connection = DriverManager.getConnection(url, username, password);

        return connection.createStatement();
    }
}
