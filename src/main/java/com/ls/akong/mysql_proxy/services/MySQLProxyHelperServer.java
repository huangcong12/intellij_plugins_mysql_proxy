package com.ls.akong.mysql_proxy.services;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.ls.akong.mysql_proxy.ui.MysqlProxyConfigurable;
import com.ls.akong.mysql_proxy.util.MySQLProxy;
import io.vertx.core.Vertx;

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service(Service.Level.PROJECT)
public final class MySQLProxyHelperServer {
    private static final Logger logger = Logger.getInstance(MySQLProxyHelperServer.class);

    private final List<MysqlProxyServiceStateListener> listeners = new ArrayList<>();
    private Vertx vertx;
    private Boolean isRunning = false;

    private Project project;

    private MySQLProxyHelperServer(Project project) {
        this.project = project;
    }

    /**
     * 启动服务
     */
    public void startServer() {
        MysqlProxySettings settings = MysqlProxySettings.getInstance(project);

        // Mysql Server Ip 校验
        if (Objects.equals(settings.getOriginalMysqlIp(), "")) {
            Messages.showErrorDialog("Run parameter exception. Please go to the Configuration Management page, fill in the 'Remote MySQL Server IP Address' field, and try again.", "Invalid Run Configuration");
            ShowSettingsUtil.getInstance().showSettingsDialog(project, MysqlProxyConfigurable.class);
            return;
        }
        if (Objects.equals(settings.getOriginalMysqlPort(), "")) {
            Messages.showErrorDialog("Run parameter exception. Please go to the Configuration Management page, fill in the 'Remote MySQL Server Port' field, and try again.", "Invalid Run Configuration");
            ShowSettingsUtil.getInstance().showSettingsDialog(project, MysqlProxyConfigurable.class);
            return;
        }

        // 监听的端口校验
        int proxyPort = Objects.equals(settings.getListeningPort(), "") ? 0 : Integer.parseInt(settings.getListeningPort());
        if (proxyPort == 0) {
            Messages.showErrorDialog("Run parameter exception. Please go to the Configuration Management page, fill in the 'Listening port' field, and try again.", "Invalid Run Configuration");
            ShowSettingsUtil.getInstance().showSettingsDialog(project, MysqlProxyConfigurable.class);
            return;
        }

        //  Java 7 的 try-with-resources 语法，它确保在代码块结束时自动关闭资源，无需手动关闭ServerSocket。这样可以避免资源泄漏。
        try (ServerSocket ignored = new ServerSocket(proxyPort)) {
            // If binding succeeds, the port is available
        } catch (BindException bindException) {     // 端口占用
            String errorMessage = "Run Failed, Error Message:\n" + bindException.getMessage() + "\n\n" + "Possible Reasons for the Failure:\n" + "1. Port " + proxyPort + " already in use. Please modify to a different port and retry.\n" + "2. Insufficient port binding permissions. If you're using a Linux-based system, consider using a port between 1024 and 65535.";
            String errorTitle = "Proxy Listener Port Conflict Error";
            logger.error(bindException.getMessage(), bindException);
            Messages.showErrorDialog(errorMessage, errorTitle);
            return;
        } catch (IOException | RuntimeException ioException) {
            String errorMessage = "An error occurred while trying to bind to port " + proxyPort + ". Please check your network settings and try again." + ioException.getMessage();
            String errorTitle = "Connection Error";
            logger.error(ioException.getMessage(), ioException);
            Messages.showErrorDialog(errorMessage, errorTitle);
            return;
        }

        vertx = Vertx.vertx();
        vertx.deployVerticle(new MySQLProxy(project, settings.getOriginalMysqlIp(), Integer.parseInt(settings.getOriginalMysqlPort()), proxyPort));

        isRunning = true;
        this.notifyListeners();
    }

    /**
     * 停止服务
     */
    public void stopServer() {
        // 关闭Vert.x实例
        vertx.close(completionHandler -> {
            if (completionHandler.succeeded()) {
                // Vert.x实例成功关闭，可以执行重启服务的操作
                isRunning = false;
                this.notifyListeners();
            } else {
                // 关闭Vert.x实例失败，处理错误情况
                Throwable cause = completionHandler.cause();
                cause.printStackTrace();
            }
        });
    }

    public Boolean isRunning() {
        return isRunning;
    }

    /**
     * 添加状态订阅者
     */
    public void addListener(MysqlProxyServiceStateListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);

            // 马上通知一遍，因为跟随编辑器启动的逻辑运行比较早，那时候还没有订阅，因此需要马上通知一遍，更改图标
            notifyListeners();
        }
    }

    /**
     * 通知状态订阅者
     */
    private void notifyListeners() {
        for (MysqlProxyServiceStateListener listener : listeners) {
            listener.onServiceStateChanged(isRunning);
        }
    }
}
