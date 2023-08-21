package com.ls.akong.mysql_proxy.services;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;

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
    public void loadState(State state) {
        myState = state;
    }

    /**
     * 是否开启监听 sql 日志
     *
     * @return
     */
    public boolean isMonitorEnabled() {
        return myState.isMonitorEnabled;
    }

    public void setMonitorEnabled(boolean enabled) {
        myState.isMonitorEnabled = enabled;
    }

    /**
     * 切换是否监听 sql 日志
     */
    public void toggleMonitorEnabled() {
        myState.isMonitorEnabled = !myState.isMonitorEnabled;
    }

    /**
     * 是否跟随编辑器启动
     *
     * @return
     */
    public boolean isStartWithEditor() {
        return myState.startWithEditor;
    }

    public static class State {
        // 是否开启监听 sql 日志
        public boolean isMonitorEnabled = true;
        // 远程 mysql ip
        public String originalMysqlIp = "";
        // 远程 mysql port
        public String originalMysqlPort = "";
        // 本插件监听的端口
        public String listeningPort = "";
        // 是否跟随编辑器启动
        public boolean startWithEditor = false;
    }
}
