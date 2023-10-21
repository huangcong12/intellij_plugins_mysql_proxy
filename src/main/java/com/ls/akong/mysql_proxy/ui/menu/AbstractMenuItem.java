package com.ls.akong.mysql_proxy.ui.menu;

import javax.swing.*;

public abstract class AbstractMenuItem {
    private String tableName;

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public JMenuItem getMenuItem() {
        return null;
    }
}
