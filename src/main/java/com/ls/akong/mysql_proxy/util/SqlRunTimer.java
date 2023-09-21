package com.ls.akong.mysql_proxy.util;

import com.intellij.openapi.project.Project;
import com.ls.akong.mysql_proxy.model.SqlLogModel;

public class SqlRunTimer {
    private final Project project;
    private long id;
    private long startTime;

    public SqlRunTimer(Project project) {

        this.project = project;
    }

    /**
     * 获取 uuid
     *
     * @return
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * 重置
     */
    public void reset() {
        id = 0;
        startTime = 0;
    }


    /**
     * 开始计时
     */
    public void startTimer() {
        startTime = System.currentTimeMillis();
    }

    /**
     * 停止计时
     */
    public void stopTimer() {
        if (startTime == 0) {
            return;
        }

        long endTime = System.currentTimeMillis();
        long elapsedTime = endTime - startTime;
        // 保存数据
        SqlLogModel.updateExecutionTime(project, id, elapsedTime);
        // 重置
        this.reset();
    }
}
