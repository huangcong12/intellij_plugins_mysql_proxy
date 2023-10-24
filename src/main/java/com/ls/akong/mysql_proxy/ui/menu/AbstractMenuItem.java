package com.ls.akong.mysql_proxy.ui.menu;

import com.intellij.openapi.project.Project;

import javax.swing.*;

public abstract class AbstractMenuItem {
    private String tableName;

    public Project getProject() {
        return project;
    }

    private Project project;

    public AbstractMenuItem(Project project) {
        this.project = project;
    }

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
