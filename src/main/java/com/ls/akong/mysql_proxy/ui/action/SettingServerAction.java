package com.ls.akong.mysql_proxy.ui.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.ls.akong.mysql_proxy.ui.MysqlProxyConfigurable;

public class SettingServerAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        ShowSettingsUtil.getInstance().showSettingsDialog(e.getProject(), MysqlProxyConfigurable.class);
    }
}
