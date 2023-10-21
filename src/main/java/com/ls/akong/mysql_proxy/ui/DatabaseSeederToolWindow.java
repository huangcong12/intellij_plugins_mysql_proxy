package com.ls.akong.mysql_proxy.ui;

import com.intellij.openapi.project.Project;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.ls.akong.mysql_proxy.util.CheckBoxNodeEditor;
import com.ls.akong.mysql_proxy.util.CheckBoxNodeRenderer;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.util.ArrayList;

public class DatabaseSeederToolWindow {
    private final Project project;

    public DatabaseSeederToolWindow(Project project) {
        this.project = project;
    }

    public Content getContent() {
        // 创建左侧面板，用于显示数据库和表的树形结构
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BorderLayout());

        // 创建树形结构
        String databaseName = "yyladmin";
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
        // 设置 tree 里的表
        setTableTree(rootNode, null, treeModel);

        // 增加搜索表的功能
        JTextField searchField = new JTextField();
        searchField.addActionListener(e -> {
            String searchText = searchField.getText();

            setTableTree(rootNode, searchText, treeModel);
        });

        JScrollPane treeScrollPane = new JScrollPane(tree);
        leftPanel.add(treeScrollPane, BorderLayout.CENTER);
        leftPanel.add(searchField, BorderLayout.NORTH);  // 添加搜索框到面板

        // 创建右侧面板，用于显示填充的历史记录
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BorderLayout());
        // 添加填充历史记录的UI组件到rightPanel

        // 创建一个顶层容器，使用JSplitPane实现左右分栏布局
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        SwingUtilities.invokeLater(() -> splitPane.setDividerLocation(0.20));   // 设置分割线位置

        // 创建内容
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        return contentFactory.createContent(splitPane, "Database Seeder", false);
    }

    private void setTableTree(DefaultMutableTreeNode rootNode, String searchText, DefaultTreeModel treeModel) {
        ArrayList<String> children = new ArrayList<>();
        children.add("users");
        children.add("order");
        children.add("order_ext");
        children.add("ya_file");

        rootNode.removeAllChildren();

        for (String child : children) {
            if (searchText != null && !child.contains(searchText)) {
                continue;
            }

            rootNode.add(new DefaultMutableTreeNode(child));
        }

        // 通知模型数据已经改变
        treeModel.reload(rootNode);

        // 添加表节点
//        DefaultMutableTreeNode tableNode2 = new DefaultMutableTreeNode();
//        DefaultMutableTreeNode tableNode3 = new DefaultMutableTreeNode();
//        DefaultMutableTreeNode tableNode4 = new DefaultMutableTreeNode();
//
//        rootNode.add(tableNode2);
//        rootNode.add(tableNode3);
//        rootNode.add(tableNode4);
    }

    // 添加匹配的节点到新的根节点
//    private void addNodesMatchingText(DefaultMutableTreeNode oldNode, String searchText, DefaultMutableTreeNode newNode) {
//        if (oldNode.getUserObject().toString().contains(searchText)) {
//            newNode.add(new DefaultMutableTreeNode(oldNode.getUserObject()));
//        }
//
//        Enumeration<TreeNode> children = oldNode.children();
//        while (children.hasMoreElements()) {
//            DefaultMutableTreeNode oldChild = (DefaultMutableTreeNode) children.nextElement();
//            DefaultMutableTreeNode newChild = new DefaultMutableTreeNode(oldChild.getUserObject());
//            addNodesMatchingText(oldChild, searchText, newChild);
//            if (!newChild.isLeaf()) {
//                newNode.add(newChild);
//            }
//        }
//    }

//    private void expandAndSelectNodesMatchingText(DefaultMutableTreeNode node, String searchText, JTree tree) {
//        if (node.getUserObject().toString().contains(searchText)) {
//            TreeNode[] pathToRoot = node.getPath();
//            TreePath path = new TreePath(pathToRoot);
//            tree.expandPath(path);
//            tree.setSelectionPath(path);
//        }
//
//        Enumeration children = node.children();
//        while (children.hasMoreElements()) {
//            DefaultMutableTreeNode child = (DefaultMutableTreeNode) children.nextElement();
//            expandAndSelectNodesMatchingText(child, searchText, tree);
//        }
//    }
}
