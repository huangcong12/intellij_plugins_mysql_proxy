package com.ls.akong.mysql_proxy.ui.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.ls.akong.mysql_proxy.services.MyTableView;
import org.jetbrains.annotations.NotNull;

/**
 * 点击 “RefreshTable” 按钮后调用
 */
public class RefreshTableViewAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project != null) {
            MyTableView myTableView = MyTableView.getInstance(project);
            myTableView.refreshData();
        }
    }
}
