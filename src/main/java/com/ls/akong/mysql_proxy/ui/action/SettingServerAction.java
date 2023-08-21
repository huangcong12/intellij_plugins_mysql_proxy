package com.ls.akong.mysql_proxy.ui.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.ls.akong.mysql_proxy.ui.MysqlProxyConfigurable;
import org.jetbrains.annotations.NotNull;

public class SettingServerAction extends AnAction {
    /**
     * @param e
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        ShowSettingsUtil.getInstance().showSettingsDialog(e.getProject(), MysqlProxyConfigurable.class);
    }
}
