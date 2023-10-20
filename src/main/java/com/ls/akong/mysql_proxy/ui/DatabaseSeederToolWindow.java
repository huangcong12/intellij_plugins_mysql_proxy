package com.ls.akong.mysql_proxy.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;

public class DatabaseSeederToolWindow {
    private final Project project;

    public DatabaseSeederToolWindow(Project project) {
        this.project = project;
    }

    public Content getContent() {
        // 再来一个新的 panel
        SimpleToolWindowPanel panel2 = new SimpleToolWindowPanel(false, true); // 注意设置第一个参数为 false，表示在左侧添加工具栏

        return ContentFactory.SERVICE.getInstance().createContent(panel2, "Database Seeder", false);
    }
}
