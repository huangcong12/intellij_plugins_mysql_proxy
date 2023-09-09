package com.ls.akong.mysql_proxy.ui.action;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.ls.akong.mysql_proxy.services.MySQLProxyServerService;
import com.ls.akong.mysql_proxy.services.MysqlProxyServiceStateListener;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * 运行、停止 sql proxy 服务
 */
public class RunOrStopServerAction extends AnAction implements MysqlProxyServiceStateListener {
    private static final Logger logger = Logger.getInstance(RunOrStopServerAction.class);

    private Project project;

    private AnActionEvent anActionEvent;

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        project = e.getProject();
        anActionEvent = e;
        if (project == null) {
            logger.error("get project failed,return null");
            return;
        }

        MySQLProxyServerService service = project.getService(MySQLProxyServerService.class);
        if (service.isServiceRunning()) {
            service.stopService();
        } else {
            service.startService();
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        project = e.getProject();
        anActionEvent = e;

        if (project == null) {
            logger.error("get project failed,return null");
            return;
        }

        MySQLProxyServerService proxyServer = project.getService(MySQLProxyServerService.class);
        proxyServer.addListener(this);  // 增加订阅状态变化

        super.update(e);
    }

    @Override
    public void onServiceStateChanged(boolean isRunning) {
        // 使用 SwingUtilities.invokeLater() 方法
        SwingUtilities.invokeLater(() -> {
            // 在此处执行与界面相关的操作
            if (isRunning) {
                anActionEvent.getPresentation().setIcon(AllIcons.Actions.Suspend);  // 运行中，展示停止图标
                anActionEvent.getPresentation().setText("Stop 'Mysql Proxy Server'");
            } else {
                anActionEvent.getPresentation().setIcon(AllIcons.Debugger.ThreadRunning); // 停止中，展示启动图标
                anActionEvent.getPresentation().setText("Start 'Mysql Proxy Server'");
            }
        });
    }
}
