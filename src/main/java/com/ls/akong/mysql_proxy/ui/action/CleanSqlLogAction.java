package com.ls.akong.mysql_proxy.ui.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.ls.akong.mysql_proxy.model.SqlLogModel;
import com.ls.akong.mysql_proxy.services.MyTableView;

/**
 * 点击 “CleanSqlLog” 按钮后调用
 */
public class CleanSqlLogAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        // 1、重置表
        SqlLogModel.truncateSqlLog(project);

        // 2、重新加载数据
        MyTableView myTableView = MyTableView.getInstance(project);
        myTableView.refreshData();

    }
}
