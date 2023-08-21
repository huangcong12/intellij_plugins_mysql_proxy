package com.ls.akong.mysql_proxy.ui.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.ls.akong.mysql_proxy.services.MyTableView;
import org.jetbrains.annotations.NotNull;

/**
 * 点击 “ScrollToTop” 后调用
 */
public class ScrollToTopAction extends AnAction {
    @Override
    public void update(@NotNull AnActionEvent e) {
        // 更新Action的状态
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // 获取当前项目
        Project project = e.getProject();

        if (project != null) {
            MyTableView myTableView = MyTableView.getInstance(project);
            myTableView.scrollToTop();
        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
