package com.ls.akong.mysql_proxy.services;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;

import javax.annotation.Nullable;
import java.util.Objects;

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
     */
    public boolean isMonitorEnabled() {
        return myState.isMonitorEnabled;
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

    /**
     * 增加跳过版本号
     *
     * @param version
     */
    public void addSkippedVersion(String version) {
        myState.skippedVersions = version;
    }

    /**
     * 判断是否是跳过的版本号
     *
     * @param version
     * @return
     */
    public boolean isVersionSkipped(String version) {
        return Objects.equals(myState.skippedVersions, version);
    }

    public static class State {
        // 跳过的最新版本号
        public String skippedVersions = "";
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
