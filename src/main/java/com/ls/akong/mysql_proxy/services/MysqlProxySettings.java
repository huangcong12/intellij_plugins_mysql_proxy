package com.ls.akong.mysql_proxy.services;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

/**
 * 状态持久化
 */
@State(name = "RecordingSwitchState", storages = {@Storage("myPluginSettings.xml")})
@Service(Service.Level.PROJECT)
public final class MysqlProxySettings implements PersistentStateComponent<MysqlProxySettings.State> {
    private State myState = new State();

    public static MysqlProxySettings getInstance(Project project) {
        return project.getService(MysqlProxySettings.class);
    }

    @Nullable
    @Override
    public State getState() {
        return myState;
    }

    @Override
    public void loadState(@NotNull State state) {
        myState = state;
    }

    /**
     * 是否开启监听 sql 日志
     */
    public boolean isMonitorEnabled() {
        return myState.isMonitorEnabled;
    }

    public void setMonitorEnabled(boolean monitorEnabled) {
        myState.isMonitorEnabled = monitorEnabled;
    }

    /**
     * 切换是否监听 sql 日志
     */
    public void toggleMonitorEnabled() {
        myState.isMonitorEnabled = !myState.isMonitorEnabled;
    }

    /**
     * 是否跟随编辑器启动
     */
    public boolean isStartWithEditor() {
        return myState.startWithEditor;
    }

    public void setStartWithEditor(boolean startWithEditor) {
        myState.startWithEditor = startWithEditor;
    }

    public String getOriginalMysqlIp() {
        return myState.originalMysqlIp;
    }

    public void setOriginalMysqlIp(String originalMysqlIp) {
        myState.originalMysqlIp = originalMysqlIp;
    }

    public String getOriginalMysqlPort() {
        return myState.originalMysqlPort;
    }

    public void setOriginalMysqlPort(String originalMysqlPort) {
        myState.originalMysqlPort = originalMysqlPort;
    }

    public String getDatabase() {
        return myState.database;
    }

    public void setDatabase(String database) {
        myState.database = database;
    }

    public String getUsername() {
        return myState.username;
    }

    public void setUsername(String username) {
        myState.username = username;
    }

    public String getListeningPort() {
        return myState.listeningPort;
    }

    public void setListeningPort(String listeningPort) {
        myState.listeningPort = listeningPort;
    }

    public static class State {
        // 是否开启监听 sql 日志
        public boolean isMonitorEnabled = true;
        // 远程 mysql ip
        public String originalMysqlIp = "";
        // 远程 mysql port
        public String originalMysqlPort = "";
        // 数据库
        public String database = "";
        // 账号
        public String username = "";
        // 本插件监听的端口
        public String listeningPort = "";
        // 是否跟随编辑器启动
        public boolean startWithEditor = false;
    }
}
