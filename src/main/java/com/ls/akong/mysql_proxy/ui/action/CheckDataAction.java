package com.ls.akong.mysql_proxy.ui.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.ls.akong.mysql_proxy.entity.SqlLog;
import com.ls.akong.mysql_proxy.model.SqlLogModel;
import com.ls.akong.mysql_proxy.services.MyTableView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class CheckDataAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        MyTableView myTableView = MyTableView.getInstance(project);
        ArrayList<SqlLog> data = myTableView.getTableModel().data();
        // 创建一个集合用于存储已经出现过的id
        Set<Integer> seenIds = new HashSet<>();
        boolean hasDuplicates = false;
        int startId = -1;
        int endId = -1;

        for (SqlLog sqlLog : data) {
            int id = sqlLog.getId();
            if (seenIds.contains(id)) {
                hasDuplicates = true;
                // 发现重复的id，可以进行相应的处理
                System.out.println("重复的id: " + id);
            } else {
                seenIds.add(id);

                // 检查id是否连续，data 是倒序的
                if (startId == -1) {
                    startId = id;
                    endId = id;
                } else if (id == endId - 1) {
                    endId = id;
                } else {
                    // 发现不连续的id
                    System.out.println("不连续的id" + id);

                    startId = -1;
                    endId = -1;
                }
            }

            // 检查 data 里的执行时间是否和表里的一致
            SqlLog tableSqlLog = SqlLogModel.getById(project, id);
            assert tableSqlLog != null;
            if (tableSqlLog.getExecutionTime() != sqlLog.getExecutionTime()) {
                System.out.println("id:" + id + " data 的执行时间： " + sqlLog.getExecutionTime() + " 表里的执行时间：" + tableSqlLog.getExecutionTime());
            }

        }

        if (!hasDuplicates) {
            System.out.println("没有重复的id。");
        }
    }
}
