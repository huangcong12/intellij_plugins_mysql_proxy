package com.ls.akong.mysql_proxy.services;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;

/**
 * 提示类，参考：https://plugins.jetbrains.com/docs/intellij/notifications.html
 */
@Service(Service.Level.PROJECT)
public class NotificationsService {

    private static final String groupId = "MySQLProxy Notification Group";


    /**
     * 提示错误信息
     *
     * @param project
     * @param content
     */
    public static void notifyError(Project project, String content) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup(groupId)
                .createNotification(content, NotificationType.ERROR)
                .notify(project);
    }

    /**
     * 提示用户有新版本可用
     *
     * @param project
     */
    public static void notifyUpdateAvailable(Project project, String latestVersion) {
        // Mysql Proxy 有新版本可用，请在插件设置的'Installed'标签页中查看
        String content = "Please check the 'Installed' tab in the plugin settings.";
        String title = "New version available for MySQL Proxy"; // 新版本可用
        Notification notification = NotificationGroupManager.getInstance()
                .getNotificationGroup(groupId)
                .createNotification(title, content, String.valueOf(NotificationType.INFORMATION));

        notification.addAction(new AnAction("Check for Updates") {  // 查看更新
            @Override
            public void actionPerformed(AnActionEvent e) {
                // 在这里打开插件设置页面
                ShowSettingsUtil.getInstance().showSettingsDialog(project, "Plugins");
                notification.expire();
            }
        });

        notification.addAction(new AnAction("Skip This Version") {  // 跳过这个版本
            @Override
            public void actionPerformed(AnActionEvent e) {
                // 用户选择跳过这个版本的操作
                MysqlProxySettings.getInstance(project).addSkippedVersion(latestVersion);
                notification.expire();
            }
        });

        notification.addAction(new AnAction("Remind Me Later") {    // 稍后提醒
            @Override
            public void actionPerformed(AnActionEvent e) {
                notification.expire();
            }
        });

        notification.notify(project);
    }

}
