package com.ls.akong.mysql_proxy.util;

import com.intellij.openapi.project.Project;
import com.ls.akong.mysql_proxy.model.SqlDatabasesModel;
import com.ls.akong.mysql_proxy.model.SqlLogModel;
import com.ls.akong.mysql_proxy.services.MyTableView;
import com.ls.akong.mysql_proxy.services.MysqlProxySettings;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 用于收集 mysql 的 COM_QUERY 里的 sql 或者 COM_STMT_PREPARE、COM_STMT_EXECUTE 组合的 sql，和统计这条 sql 的执行时间
 */
public class SqlExecutionStatisticsCollector {
    private final Map<Integer, String> preparedStatements;
    private final Project project;
    private String preparingStatement;
    private long startTime;
    private long executionTime;
    private String sql;
    private int databaseId = 0;

    public SqlExecutionStatisticsCollector(Project project) {
        this.project = project;
        this.preparedStatements = new HashMap<>();
        this.preparingStatement = null;
        this.startTime = 0;
    }

    /**
     * 设置数据库名字。可能有这些场景：
     * 1、直接 database name
     * 2、use xx; use `xx`;
     * 3、use "xx";
     * 4、use 'xx';
     * 5、USE xx;
     * 6、USE `xx`;
     * 7、USE "xx";
     * 8、USE 'xxx';
     * 9、/星 ApplicationName=DataGrip 2023.2 星/ use test
     *
     * @param databaseName
     */
    public void setDatabaseName(String databaseName) {
        this.databaseId = SqlDatabasesModel.getDatabasesIdByName(project, getDatabaseName(databaseName));
    }

    /**
     * 获取 database name
     *
     * @param sqlOrDbName
     * @return
     */
    public String getDatabaseName(String sqlOrDbName) {
        // If the input is a single word, return it directly as the database name
        if (!sqlOrDbName.contains(" ")) {
            return sqlOrDbName;
        }

        // Pattern to match: use xx; use `xx`; use "xx"; use 'xx'; USE xx; USE `xx`; USE "xx"; USE 'xxx'; /* ApplicationName=DataGrip 2023.2 */ use test
        String pattern = "\\s*(/\\*.*\\*/)?\\s*(?i)use\\s+(`([^`]+)`|\"([^\"]+)\"|'([^']+)'|(\\w+));?\\s*";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(sqlOrDbName.trim());

        if (m.find()) {
            for (int i = 2; i <= m.groupCount(); i++) {
                if (m.group(i) != null) {
                    String dbName = m.group(i);
                    // Remove backticks, double quotes, and single quotes from the database name
                    dbName = dbName.replaceAll("`", "").replaceAll("\"", "").replaceAll("'", "");
                    return dbName;
                }
            }
        }

        return null;
    }

    /**
     * 用来判断是否已设置数据库，如果已设置，不需要再走一遍逻辑。因为这里会产生多余的耗时
     *
     * @return
     */
    public boolean isDatabaseNameIsEmpty() {
        return this.databaseId == 0;
    }

    /**
     * 获取执行耗时
     *
     * @return
     */
    public long getExecutionTime() {
        return executionTime;
    }

    /**
     * COM_STMT_EXECUTE 包根据 statementId 获取并删除预处理 sql
     *
     * @param key
     * @return
     */
    public String removePreparedStatement(int key) {
        return preparedStatements.remove(key);
    }

    /**
     * COM_STMT_PREPARE 包保存预处理 sql 和 statementId 关系
     *
     * @param key
     * @param value
     */
    public void putToPreparedStatements(int key, String value) {
        preparedStatements.put(key, value);
    }


    /**
     * 获取 COM_STMT_PREPARE 暂存的预处理 sql
     *
     * @return
     */
    public String getPreparingStatement() {
        return preparingStatement;
    }

    /**
     * COM_STMT_PREPARE 包暂存预处理 sql
     *
     * @param preparingStatement
     */
    public void setPreparingStatement(String preparingStatement) {
        this.preparingStatement = preparingStatement;
    }

    /**
     * 记录开始执行时间
     */
    public void startTiming() {
        if (startTime != 0) {
            return;
        }

        this.startTime = System.currentTimeMillis();
    }

    /**
     * 停止计时，并获取总的执行时间
     */
    public void stopTiming() {
        if (sql == null) {
            // 如果没有处理得到一条完整的 sql ，则不停止计时（预处理 sql 场景）
            return;
        }

        long endTime = System.currentTimeMillis();
        executionTime = endTime - startTime;

        // 检查是否开启记录 sql
        MysqlProxySettings recordingSwitch = MysqlProxySettings.getInstance(project);
        if (recordingSwitch.isMonitorEnabled()) {
            String signature = SQLFingerprintGenerator.getSignatureBySql(sql);

            // 记录 sql 到表里
            SqlLogModel.insertLog(project, sql, executionTime, signature, databaseId);

            MyTableView myTableView = MyTableView.getInstance(project);
            myTableView.updateData();
        }

        // 重置
        startTime = 0;
        this.sql = null;
    }

    /**
     * 设置 sql
     *
     * @param sql
     */
    public void setSql(String sql) {
        if (this.sql != null) {
            return;
        }

        this.sql = StringHelper.mergedIntoOneLine(sql);
    }
}
