package com.ls.akong.mysql_proxy.services;

import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;

@Service(Service.Level.PROJECT)
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
