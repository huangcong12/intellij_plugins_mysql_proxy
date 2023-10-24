package com.ls.akong.mysql_proxy.ui;

import com.intellij.openapi.project.Project;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.ls.akong.mysql_proxy.model.DatabaseInfoModel;
import com.ls.akong.mysql_proxy.services.MysqlProxySettings;
import com.ls.akong.mysql_proxy.ui.menu.DatabaseSeederPopupMenuModel;
import com.ls.akong.mysql_proxy.util.CheckBoxNode;
import com.ls.akong.mysql_proxy.util.CheckBoxNodeEditor;
import com.ls.akong.mysql_proxy.util.CheckBoxNodeRenderer;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.ArrayList;

public class DatabaseSeederToolWindow {
    private final Project project;
    private MysqlProxySettings settings;

    public DatabaseSeederToolWindow(Project project) {
        this.project = project;

        this.settings = MysqlProxySettings.getInstance(project);
    }

    public Content getContent() {
        // 创建右侧面板，用于显示填充的历史记录
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BorderLayout());

        // 创建一个顶层容器，使用JSplitPane实现左右分栏布局
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, getLeftPane(), rightPanel);
        SwingUtilities.invokeLater(() -> splitPane.setDividerLocation(0.20));   // 设置分割线位置

        // 创建内容
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        return contentFactory.createContent(splitPane, "Database Seeder", false);
    }

    /**
     * 左边 Pane
     *
     * @return
     */
    private JPanel getLeftPane() {
        // 创建树形结构
        String databaseName = settings.getDatabase();
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(databaseName);

        // 创建树模型并设置为JTree的模型
        DefaultTreeModel treeModel = new DefaultTreeModel(rootNode);
        JTree tree = new JTree(treeModel);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        // 创建带有复选框的树节点
        CheckBoxNodeRenderer renderer = new CheckBoxNodeRenderer();
        tree.setCellRenderer(renderer);
        tree.setCellEditor(new CheckBoxNodeEditor(tree));
        tree.setEditable(true);

        // 右键菜单
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    // 获取被右键单击的树节点
                    TreePath path = tree.getPathForLocation(e.getX(), e.getY());
                    if (path != null) {
                        // 获取用户对象，这通常是节点的文本标签
                        String tableName = "";
                        Object userObject = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
                        if (userObject instanceof String) {
                            tableName = (String) userObject;
                        } else if (userObject instanceof CheckBoxNode) {
                            tableName = ((CheckBoxNode) userObject).getText();
                        }

                        // 弹出菜单
                        tree.setSelectionPath(path);
                        DatabaseSeederPopupMenuModel.createPopupMenu(project, tableName).show(tree, e.getX(), e.getY());
                    }
                }
            }
        });
        // 设置 tree 里的表
        setTableTree(rootNode, null, treeModel);

        JScrollPane treeScrollPane = new JScrollPane(tree);

        // 创建左侧面板，用于显示数据库和表的树形结构
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BorderLayout());
        leftPanel.add(treeScrollPane, BorderLayout.CENTER);
        leftPanel.add(getLeftPaneSearchField(rootNode, treeModel), BorderLayout.NORTH);  // 添加搜索框到面板

        return leftPanel;
    }


    /**
     * 左边 Table 搜索框
     *
     * @param rootNode
     * @param treeModel
     * @return
     */
    private JTextField getLeftPaneSearchField(DefaultMutableTreeNode rootNode, DefaultTreeModel treeModel) {
        // 增加搜索表的功能
        JTextField searchField = new JTextField();
        searchField.addActionListener(e -> {
            String searchText = searchField.getText();
            setTableTree(rootNode, searchText, treeModel);
        });

        return searchField;
    }

    /**
     * 获取数据库的所有表
     *
     * @param rootNode
     * @param searchText
     * @param treeModel
     */
    private void setTableTree(DefaultMutableTreeNode rootNode, String searchText, DefaultTreeModel treeModel) {
        try {
            DatabaseInfoModel databaseInfoModel = new DatabaseInfoModel(project);
            ArrayList<String> tables = databaseInfoModel.getAllTable(settings.getDatabase());

            rootNode.removeAllChildren();

            for (String table : tables) {
                // 假如条件搜索
                if (searchText != null && !table.contains(searchText)) {
                    continue;
                }
                rootNode.add(new DefaultMutableTreeNode(table));
            }

            // 通知模型数据已经改变
            treeModel.reload(rootNode);

        } catch (SQLException | ClassNotFoundException e) {
            // TODO 抛出错误，提示用户
        }
    }
}
