package com.ls.akong.mysql_proxy.ui.editor;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.ls.akong.mysql_proxy.model.DatabaseInfoModel;
import com.ls.akong.mysql_proxy.model.TableColumnInfo;
import com.ls.akong.mysql_proxy.services.MysqlProxySettings;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;

public class CustomTableEditor {
    public static void show(Project project, String tableName) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Seeder rules for the " + tableName);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

            JTable table = null;
            try {
                table = createCustomTable(project, tableName);
            } catch (SQLException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
            JScrollPane scrollPane = new JScrollPane(table);

            frame.getContentPane().add(scrollPane, BorderLayout.CENTER);
            frame.setSize(1200, 800);

            // Locate the frame relative to the active editor window
            Component activeEditor = FileEditorManager.getInstance(project).getSelectedTextEditor().getComponent();
            frame.setLocationRelativeTo(activeEditor);

            frame.setVisible(true);
        });
    }

    private static JTable createCustomTable(Project project, String tableName) throws SQLException, ClassNotFoundException {
        String[] columnNames = {"Field Name", "Field Type", "Fill Rule"};

        // 获取表的字段
        DatabaseInfoModel databaseInfoModel = new DatabaseInfoModel(project);
        String databaseName = MysqlProxySettings.getInstance(project).getDatabase();
        ArrayList<TableColumnInfo> columnInfoList = databaseInfoModel.getTableMetaData(databaseName, tableName);

        // 将 columnInfoList 转换为 data 数组
        Object[][] data = new Object[columnInfoList.size()][3];

        for (int i = 0; i < columnInfoList.size(); i++) {
            TableColumnInfo columnInfo = columnInfoList.get(i);
            data[i][0] = columnInfo.getColumnName();
            data[i][1] = columnInfo.getTypeName();
            data[i][2] = "";
        }

        JTable table = new JTable(data, columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // 禁止所有单元格编辑
            }
        };
        table.setPreferredScrollableViewportSize(new Dimension(1200, 800));
        table.setFillsViewportHeight(true);

        // 设置每列的宽度
        setColumnWidth(table, 0, 150);
        setColumnWidth(table, 1, 150);
        table.getColumnModel().getColumn(2).setMinWidth(0); // 设置第三列最小宽度为0像素
        table.getColumnModel().getColumn(2).setMaxWidth(Integer.MAX_VALUE); // 设置第三列最大宽度为最大整数值

        table.setRowHeight(30); // 设置行高为30像素，你可以根据需要调整这个值

        return table;
    }

    private static void setColumnWidth(JTable table, int column, int width) {
        table.getColumnModel().getColumn(column).setPreferredWidth(width);
        table.getColumnModel().getColumn(column).setMinWidth(width);
        table.getColumnModel().getColumn(column).setMaxWidth(width);
    }
}
