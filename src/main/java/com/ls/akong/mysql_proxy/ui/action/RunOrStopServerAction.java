package com.ls.akong.mysql_proxy.ui.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.ls.akong.mysql_proxy.services.MySQLProxyServerService;

import javax.swing.*;

/**
 * 运行、停止 sql proxy 服务
 */
public class RunOrStopServerAction extends AnAction {
    private static final Logger logger = Logger.getInstance(RunOrStopServerAction.class);
    private Project project;

    @Override
    public void actionPerformed(AnActionEvent e) {
        project = e.getProject();

        if (project == null) {
            logger.error("get project failed,return null");
            return;
        }

        MySQLProxyServerService service = project.getService(MySQLProxyServerService.class);
        if (service.isServiceRunning()) {
            service.stopService();
            onServiceStateChanged(false, e);
        } else {
            service.startService();
            onServiceStateChanged(true, e);
        }
    }

    @Override
    public void update(AnActionEvent e) {
        project = e.getProject();

        if (project == null) {
            logger.error("get project failed,return null");
            return;
        }

        MySQLProxyServerService proxyServer = project.getService(MySQLProxyServerService.class);
        onServiceStateChanged(proxyServer.isServiceRunning(), e);

        super.update(e);
    }

    public void onServiceStateChanged(boolean isRunning, AnActionEvent e) {
        // 使用 SwingUtilities.invokeLater() 方法
        SwingUtilities.invokeLater(() -> {
            if (isRunning) {
                Icon suspendIcon = IconLoader.getIcon("/icons/suspend.svg", RunOrStopServerAction.class);
                e.getPresentation().setIcon(suspendIcon);
                e.getPresentation().setText("Stop 'Mysql Proxy Server'");
                logger.info("change icon to /icons/suspend.svg");
            } else {
                Icon threadRunningIcon = IconLoader.getIcon("/icons/threadRunning.svg", RunOrStopServerAction.class);
                e.getPresentation().setIcon(threadRunningIcon);
                e.getPresentation().setText("Start 'Mysql Proxy Server'");
                logger.info("change icon to /icons/threadRunning.svg");
            }
        });
    }
}
