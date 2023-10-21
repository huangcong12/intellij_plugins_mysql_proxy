package com.ls.akong.mysql_proxy.ui.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.IconLoader;
import com.ls.akong.mysql_proxy.services.MysqlProxySettings;

import javax.swing.*;
import java.util.Objects;

/**
 * 监听、关闭监听 sql log 按钮点击调用
 */
public class RecordingSwitchAction extends AnAction {
    private static final Logger logger = Logger.getInstance(RecordingSwitchAction.class);

    /**
     * 按钮点击调用
     */
    @Override
    public void actionPerformed(AnActionEvent e) {
        MysqlProxySettings recordingSwitch = MysqlProxySettings.getInstance(Objects.requireNonNull(e.getProject()));
        recordingSwitch.toggleMonitorEnabled();
    }

    @Override
    public void update(AnActionEvent e) {
        super.update(e);
        updateIcon(e);
    }

    private void updateIcon(AnActionEvent e) {
        // 使用 SwingUtilities.invokeLater() 方法
        SwingUtilities.invokeLater(() -> {
            // 在此处执行与界面相关的操作
            MysqlProxySettings recordingSwitch = MysqlProxySettings.getInstance(Objects.requireNonNull(e.getProject()));
            if (recordingSwitch.isMonitorEnabled()) {
                Icon listeningIcon = IconLoader.getIcon("/icons/listening.svg", RecordingSwitchAction.class);
                e.getPresentation().setIcon(listeningIcon); // 监听中，展示停止图标
                e.getPresentation().setText("Recording Synchronized SQL In Progress");
            } else {
                Icon stopListeningIcon = IconLoader.getIcon("/icons/stop_listening.svg", RecordingSwitchAction.class);
                e.getPresentation().setIcon(stopListeningIcon); // 停止中，展示启动监听图标
                e.getPresentation().setText("Stop Recording SQL");
            }
        });
    }
}
