package com.ls.akong.mysql_proxy.ui.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.ls.akong.mysql_proxy.services.MySQLProxyHelperServer;

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

        MySQLProxyHelperServer proxyServer = project.getService(MySQLProxyHelperServer.class);
        if (proxyServer.isRunning()) {
            proxyServer.stopServer();
            onServiceStateChanged(false, e);
        } else {
            proxyServer.startServer();
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

        MySQLProxyHelperServer proxyServer = project.getService(MySQLProxyHelperServer.class);
        onServiceStateChanged(proxyServer.isRunning(), e);

        super.update(e);
    }


    /**
     * 更改图标，本来是用订阅模式的，但是高版本的 idea 一直不行，因此使用这种一直调用的办法。不想浪费时间耗在这里了
     *
     * @param isRunning
     * @param e
     */
    public void onServiceStateChanged(boolean isRunning, AnActionEvent e) {
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
