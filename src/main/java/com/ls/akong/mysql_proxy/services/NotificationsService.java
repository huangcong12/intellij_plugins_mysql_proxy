package com.ls.akong.mysql_proxy.services;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.components.Service;
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
}
