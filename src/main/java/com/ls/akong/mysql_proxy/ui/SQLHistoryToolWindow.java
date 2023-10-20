package com.ls.akong.mysql_proxy.ui;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.ls.akong.mysql_proxy.services.MyTableView;
import com.ls.akong.mysql_proxy.ui.filter.FilterModule;

public class SQLHistoryToolWindow {

    private final Project project;

    public SQLHistoryToolWindow(Project project) {
        this.project = project;
    }

    public Content getContent() {
        // 新建一个 panel
        SimpleToolWindowPanel panel = new SimpleToolWindowPanel(false, true); // 注意设置第一个参数为 false，表示在左侧添加工具栏

        // 1、左边的按钮
        DefaultActionGroup leftToolbarGroupButtons = (DefaultActionGroup) ActionManager.getInstance().getAction("MysqlProxy.LeftToolbar");
        ActionToolbar leftToolbar = ActionManager.getInstance().createActionToolbar("MysqlProxy.LeftToolbar", leftToolbarGroupButtons, false);
        // 使用 BorderLayout 设置上方和左侧的工具栏
        leftToolbar.setTargetComponent(panel);
        panel.setToolbar(leftToolbar.getComponent());

        // 2、List 数据
        // 创建一个新的 JPanel，包含 ActionToolbar 和 SearchTextField
        JBSplitter sqlListSplitter = new JBSplitter(true, 0.06f);

        // 顶部条件搜索按钮组
        sqlListSplitter.setFirstComponent(FilterModule.createToolbar(project));
        // 下部 table view
        MyTableView myTableView = MyTableView.getInstance(project);
        sqlListSplitter.setSecondComponent(myTableView);

        // 总的 splitter
        JBSplitter panelSplitter = new JBSplitter(false, 0.8f);
        panelSplitter.setSplitterProportionKey("MysqlProxy.SqlPanelSplitter.Proportion");
        panelSplitter.setFirstComponent(sqlListSplitter);

        panel.setContent(panelSplitter);

        return ContentFactory.SERVICE.getInstance().createContent(panel, "SQL History", false);
    }
}
