package com.ls.akong.mysql_proxy.ui;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.SearchTextField;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.ls.akong.mysql_proxy.services.MySQLProxyServer;
import com.ls.akong.mysql_proxy.services.MyTableView;
import com.ls.akong.mysql_proxy.services.MysqlProxyServiceStateListener;
import com.ls.akong.mysql_proxy.ui.action.RecordingSwitchAction;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

/**
 * ToolWindowFactory 是必须要实现的
 * DumbAware 在 "Dumb Mode" 下，即在一些特定的后台任务运行期间，组件是否应该继续工作，实现了即表示要工作
 */
public class SqlToolWindowFactory implements ToolWindowFactory, DumbAware, MysqlProxyServiceStateListener {
    private ToolWindow toolWindow;

    /**
     * 启动一个新 tool windows，编辑器启动完成后，自动调用。只运行一遍，ToolWindowFactory 规定必须要实现
     */
    @Override
    public void createToolWindowContent(Project project, ToolWindow toolWindow) {
        this.toolWindow = toolWindow;

        // 新建一个 panel
        SimpleToolWindowPanel panel = new SimpleToolWindowPanel(false, true); // 注意设置第一个参数为 false，表示在左侧添加工具栏

        // 1、左边的按钮
        DefaultActionGroup leftToolbarGroupButtons = (DefaultActionGroup) ActionManager.getInstance().getAction("MysqlProxy.LeftToolbar");
        ActionToolbar leftToolbar = ActionManager.getInstance().createActionToolbar("MysqlProxy.LeftToolbar", leftToolbarGroupButtons, false);
        // 使用 BorderLayout 设置上方和左侧的工具栏
        leftToolbar.setTargetComponent(panel);
        panel.setToolbar(leftToolbar.getComponent());

        // 2、List 数据
        // 顶部 创建 SearchTextField
        SearchTextField searchField = new SearchTextField();
        // 创建一个 JComboBox 包含时间段选项
        String[] timeRanges = {"No Limit", "Within 10s", "Within 1m", "Within 5m", "Within 10m"};
        JComboBox<String> timeRangeComboBox = new com.intellij.openapi.ui.ComboBox<>(timeRanges);

        // 输入框监听
        searchField.addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                reloadData();
            }

            private void reloadData() {
                // 当插入文本时被调用
                MyTableView tableView = MyTableView.getInstance(project);
                MyTableView.MyTableModel myTableModel = tableView.getTableModel();
                myTableModel.setSearchText(searchField.getText());
                myTableModel.setSelectedTimeRange((String) timeRangeComboBox.getSelectedItem());
                tableView.refreshData();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                reloadData();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                reloadData();
            }
        });

        // 添加 ActionListener 来监听选择事件
        timeRangeComboBox.addActionListener(e -> {
            // 在这里可以根据选择的时间段进行处理
            MyTableView tableView = MyTableView.getInstance(project);
            MyTableView.MyTableModel myTableModel = tableView.getTableModel();
            myTableModel.setSearchText(searchField.getText());
            myTableModel.setSelectedTimeRange((String) timeRangeComboBox.getSelectedItem());
            tableView.refreshData();
        });

        // 创建一个新的 JPanel，包含 ActionToolbar 和 SearchTextField
        JPanel topToolbarPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        topToolbarPanel.add(new JLabel("Filter:"));
        topToolbarPanel.add(searchField);
        topToolbarPanel.add(new JLabel("Time Range:"));
        topToolbarPanel.add(timeRangeComboBox);

        JBSplitter sqlListSplitter = new JBSplitter(true, 0.01f);
        // 顶部按钮
        sqlListSplitter.setFirstComponent(topToolbarPanel);
        // 下部 table view
        MyTableView myTableView = MyTableView.getInstance(project);
        sqlListSplitter.setSecondComponent(myTableView);

        // 总的 splitter
        JBSplitter panelSplitter = new JBSplitter(false, 0.8f);
        panelSplitter.setSplitterProportionKey("MysqlProxy.SqlPanelSplitter.Proportion");
        panelSplitter.setFirstComponent(sqlListSplitter);

        panel.setContent(panelSplitter);

        // 把 panel 放到 toolWindow 里
        ContentManager contentManager = toolWindow.getContentManager();
        Content content = ContentFactory.SERVICE.getInstance().createContent(panel, "", false);
        contentManager.addContent(content);
        contentManager.setSelectedContent(content);

        // 监听代理服务器状态
        MySQLProxyServer proxyServer = project.getService(MySQLProxyServer.class);
        proxyServer.addListener(this);  // 增加订阅状态变化
    }

    /**
     * mysql proxy server 状态变化订阅。修改 toolwindow 的图标
     */
    @Override
    public void onServiceStateChanged(boolean isRunning) {
        // 使用 SwingUtilities.invokeLater() 方法
        SwingUtilities.invokeLater(() -> {
            if (isRunning) {
                Icon runningIcon = IconLoader.getIcon("/icons/logo_running.svg", RecordingSwitchAction.class);
                toolWindow.setIcon(runningIcon);
            } else {
                Icon originalIcon = IconLoader.getIcon("/icons/logo.svg", RecordingSwitchAction.class);
                toolWindow.setIcon(originalIcon);
            }
        });
    }
}
