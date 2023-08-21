package com.ls.akong.mysql_proxy.ui.action;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.ls.akong.mysql_proxy.services.MySQLProxyServerService;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * 运行、停止 sql proxy 服务
 */
public class RunOrStopServerAction extends AnAction {
    private static final Logger logger = Logger.getInstance(RunOrStopServerAction.class);

    private Project project;

    /**
     * @param e
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        project = e.getProject();
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

    /**
     * @param e
     */
    @Override
    public void update(@NotNull AnActionEvent e) {
        project = e.getProject();
        if (project == null) {
            logger.error("get project failed,return null");
            return;
        }

        super.update(e);
        updateIcon(e);
    }

    /**
     * 从 IntelliJ Platform 2022.3 开始，AnAction.getActionUpdateThread() 需要被插件开发者实现，以指定 AnAction.update()
     * 方法应当在哪个线程中运行，是后台线程（Background Thread，简称 BGT）还是事件分发线程（Event Dispatch Thread，简称 EDT）
     * plugins.jetbrains.com。当你在 IntelliJ 插件开发中创建一个新的 Action 时，你需要继承 AnAction 类，并重写 update()
     * 和 actionPerformed() 方法。其中，update() 方法负责更新 Action 的状态，如是否可用、是否可见等，actionPerformed()
     * 方法则负责实现 Action 的具体行为plugins.jetbrains.com。 AnAction.getActionUpdateThread() 方法的作用是指定 update()
     * 方法在哪个线程中运行。你可以选择在后台线程（BGT）或事件分发线程（EDT）中运行 update() 方法。如果你选择在后台线程运行 update()
     * 方法，那么你可以保证在执行 update() 方法时可以读取 PSI、虚拟文件系统（VFS）或项目模型。但是，你不能直接访问 Swing 组件。
     * 如果你选择在事件分发线程运行 update() 方法，那么你不能访问 PSI、VFS 或项目数据，但是可以访问 Swing 组件和其他 UI 模型 plugins.jetbrains.com。
     *
     * @return
     */
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }


    private void updateIcon(AnActionEvent e) {
        // 使用 SwingUtilities.invokeLater() 方法
        SwingUtilities.invokeLater(() -> {
            // 在此处执行与界面相关的操作
            MySQLProxyServerService mySQLProxyServerService = project.getService(MySQLProxyServerService.class);

            if (mySQLProxyServerService.isServiceRunning()) {
                e.getPresentation().setIcon(AllIcons.Actions.Suspend);  // 运行中，展示停止图标
                e.getPresentation().setText("Stop 'Mysql Proxy Server'");
            } else {
                e.getPresentation().setIcon(AllIcons.Debugger.ThreadRunning); // 停止中，展示启动图标
                e.getPresentation().setText("Start 'Mysql Proxy Server'");
            }
        });
    }
}
