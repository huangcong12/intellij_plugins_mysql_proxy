package com.ls.akong.mysql_proxy.ui.editor;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.ls.akong.mysql_proxy.model.DatabaseInfoModel;
import com.ls.akong.mysql_proxy.model.TableColumnInfo;
import com.ls.akong.mysql_proxy.services.MysqlProxySettings;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EventObject;

public class CustomTableEditor {
    public static void show(Project project, String tableName) {
        SwingUtilities.invokeLater(() -> {
            try {
                JFrame frame = new JFrame("Seeder rules for the " + tableName);
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                JTable table = createCustomTable(project, tableName);

                JScrollPane scrollPane = new JScrollPane(table);

                frame.getContentPane().add(scrollPane, BorderLayout.CENTER);
                frame.setSize(1200, 800);

                // Locate the frame relative to the active editor window
                Component activeEditor = FileEditorManager.getInstance(project).getSelectedTextEditor().getComponent();
                frame.setLocationRelativeTo(activeEditor);

                frame.setVisible(true);
            } catch (SQLException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static JTable createCustomTable(Project project, String tableName) throws SQLException, ClassNotFoundException {
        String[] columnNames = {"Field Name", "Field Type", "Fill Rule"};

        // 获取表的字段
        DatabaseInfoModel databaseInfoModel = new DatabaseInfoModel(project);
        String databaseName = MysqlProxySettings.getInstance(project).getDatabase();
        ArrayList<TableColumnInfo> columnInfoList = databaseInfoModel.getTableMetaData(databaseName, tableName);

        // 设置列宽
        int fieldWidth = 200;
        int fieldTypeWidth = 200;
        int fillRuleWidth = 300;

        DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return true;
//                return column == 2; // 禁止所有单元格编辑
            }
        };

        for (TableColumnInfo columnInfo : columnInfoList) {
            model.addRow(new Object[]{columnInfo.getColumnName(), columnInfo.getTypeName(), ""});
        }

        JTable table = new JTable(model);
        table.getColumnModel().getColumn(0).setPreferredWidth(fieldWidth);
        table.getColumnModel().getColumn(1).setPreferredWidth(fieldTypeWidth);
        table.getColumnModel().getColumn(2).setPreferredWidth(fillRuleWidth);

        // 添加 JComboBox 渲染器和编辑器
//        JComboBox<String> fillRuleComboBox = new JComboBox<>(new String[]{"Rule1", "Rule2", "Rule3"});
//        table.getColumnModel().getColumn(2).setCellRenderer(new JComboBoxTableCellRenderer(fillRuleComboBox));
//        table.getColumnModel().getColumn(2).setCellEditor(new JComboBoxTableCellEditor(fillRuleComboBox));
        table.getColumnModel().getColumn(2).setCellRenderer(new DynamicRenderer());
        table.getColumnModel().getColumn(2).setCellEditor(new DynamicEditor());

        table.setRowHeight(30); // 设置行高为30像素，你可以根据需要调整这个值

        return table;
    }

    private static void setColumnWidth(JTable table, int column, int width) {
        table.getColumnModel().getColumn(column).setPreferredWidth(width);
        table.getColumnModel().getColumn(column).setMinWidth(width);
        table.getColumnModel().getColumn(column).setMaxWidth(width);
    }

    private static class JComboBoxTableCellRenderer extends DefaultTableCellRenderer {
        private JComboBox<String> comboBox;

        JComboBoxTableCellRenderer(JComboBox<String> comboBox) {
            this.comboBox = comboBox;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (value != null) {
                comboBox.setSelectedItem(value.toString());
            }
            return comboBox;
        }
    }

    private static class JComboBoxTableCellEditor extends DefaultCellEditor implements TableCellEditor {
        private JComboBox<String> comboBox;

        JComboBoxTableCellEditor(JComboBox<String> comboBox) {
            super(comboBox);
            this.comboBox = comboBox;

            // 为 JComboBox 添加事件处理器
            comboBox.addActionListener(e -> {
                if (comboBox.isPopupVisible() && isCellEditable(null)) {
                    stopCellEditing();
                }
            });
        }

        @Override
        public boolean isCellEditable(EventObject e) {
            return true;
        }

        @Override
        public Object getCellEditorValue() {
            return comboBox.getSelectedItem();
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            if (value != null) {
                comboBox.setSelectedItem(value.toString());
            }

            // 设置当前行获得焦点
            table.setRowSelectionInterval(row, row);
            table.setColumnSelectionInterval(column, column);

            if (!comboBox.isPopupVisible()) {
                comboBox.setPopupVisible(true);
            }
            return comboBox;
        }
    }
}
