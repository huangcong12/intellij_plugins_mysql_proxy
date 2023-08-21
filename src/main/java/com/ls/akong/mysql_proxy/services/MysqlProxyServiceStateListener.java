package com.ls.akong.mysql_proxy.services;

public interface MysqlProxyServiceStateListener {
    void onServiceStateChanged(boolean isRunning);
}
