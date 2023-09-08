package com.ls.akong.mysql_proxy.ui;

import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.Project;
import com.ls.akong.mysql_proxy.services.MySQLProxyServerService;
import com.ls.akong.mysql_proxy.services.MysqlProxySettings;

public class MySQLToolWindowListener implements ProjectComponent {

    private final Project project;

    public MySQLToolWindowListener(Project project) {
        this.project = project;
    }

    public void projectOpened() {
        // 跟随编辑器启动
        MySQLProxyServerService proxyServer = project.getService(MySQLProxyServerService.class);
        if (MysqlProxySettings.getInstance(project).isStartWithEditor()) {
            proxyServer.startService();
        }
    }
}
