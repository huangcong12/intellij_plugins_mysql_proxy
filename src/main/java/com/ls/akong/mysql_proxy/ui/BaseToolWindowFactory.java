package com.ls.akong.mysql_proxy.ui;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.ls.akong.mysql_proxy.services.MySQLProxyHelperServer;
import com.ls.akong.mysql_proxy.services.MysqlProxyServiceStateListener;
import com.ls.akong.mysql_proxy.ui.action.RecordingSwitchAction;

import javax.swing.*;

/**
 * BaseToolWindowFactory 是必须要实现的
 * DumbAware 在 "Dumb Mode" 下，即在一些特定的后台任务运行期间，组件是否应该继续工作，实现了即表示要工作
 */
public class BaseToolWindowFactory implements ToolWindowFactory, DumbAware, MysqlProxyServiceStateListener {
    private ToolWindow toolWindow;


    /**
     * 启动一个新 tool windows，编辑器启动完成后，自动调用。只运行一遍，ToolWindowFactory 规定必须要实现
     */
    @Override
    public void createToolWindowContent(Project project, ToolWindow toolWindow) {
        this.toolWindow = toolWindow;
        ContentManager contentManager = toolWindow.getContentManager();

        // 1、SQL 日志的标签页
        SQLHistoryToolWindow history = new SQLHistoryToolWindow(project);
        Content historyContent = history.getContent();
        contentManager.addContent(historyContent);
//        contentManager.setSelectedContent(historyContent);     // 默认选中

        // 2、数据填充页
        DatabaseSeederToolWindow seeder = new DatabaseSeederToolWindow(project);
        Content seederContent = seeder.getContent();
        contentManager.addContent(seederContent);
        contentManager.setSelectedContent(seederContent);     // 默认选中

        // 监听代理服务器状态
        MySQLProxyHelperServer proxyServer = project.getService(MySQLProxyHelperServer.class);
        proxyServer.addListener(this);  // 增加订阅状态变化
    }

    /**
     * mysql proxy server 状态变化订阅。修改 toolwindow 的图标
     */
    @Override
    public void onServiceStateChanged(boolean isRunning) {
        // 使用 SwingUtilities.invokeLater() 方法
        SwingUtilities.invokeLater(() -> {
            if (isRunning) {
                Icon runningIcon = IconLoader.getIcon("/icons/logo_running.svg", RecordingSwitchAction.class);
                toolWindow.setIcon(runningIcon);
            } else {
                Icon originalIcon = IconLoader.getIcon("/icons/logo.svg", RecordingSwitchAction.class);
                toolWindow.setIcon(originalIcon);
            }
        });
    }
}
