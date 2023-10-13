package com.ls.akong.mysql_proxy.util;

import com.intellij.openapi.project.Project;
import com.ls.akong.mysql_proxy.model.SqlLogModel;
import com.ls.akong.mysql_proxy.services.MyTableView;
import com.ls.akong.mysql_proxy.services.MysqlProxySettings;

import java.util.HashMap;
import java.util.Map;

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

    public SqlExecutionStatisticsCollector(Project project) {
        this.project = project;
        this.preparedStatements = new HashMap<>();
        this.preparingStatement = null;
        this.startTime = 0;
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
            SqlLogModel.insertLog(project, sql, executionTime, signature);

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
