package com.ls.akong.mysql_proxy.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.ls.akong.mysql_proxy.entity.SqlLogFilter;
import com.ls.akong.mysql_proxy.model.SqlLogFilterModel;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.Socket;
import java.util.List;
import java.util.Vector;

public class ConfigurableForm {
    private JPanel panel;
    private JTextField targetMysqlIpTextField;
    private JTextField targetMysqlPortTextField;
    private JButton testConnectionButton;
    private JPanel mysqlPanel;
    private JTextField ListeningPortTextField;
    private JPanel FieldSqlPanel;
    private JCheckBox proxyServerStartWithCheckBox;
    private JPanel title;
    private JTable table1;
    private JButton newButton;
    private JButton removeButton;
    private JButton editButton;

    private Project project;

    private DefaultTableModel tableModel;

    public ConfigurableForm(Project project) {
        this.project = project;

        // 创建表格模型
        tableModel = new DefaultTableModel();

        initTestConnectonButton();

        // 新增 sql filter 按钮
        initSqlFilterButton(newButton);
        // 删除 sql filter 按钮
        initSqlFilterButton(removeButton);
        // 编辑 sql filter 按钮
        initSqlFilterButton(editButton);

        // 新增 sql filter 按钮点击事件
        newButton.addActionListener(e -> {
            FilterSqlDialog dialog = new FilterSqlDialog(project);
            if (dialog.showAndGet()) {
                String sql = dialog.getSqlText();
                SqlLogFilterModel.insertLogFilter(project, sql);

                // 重新加载数据
                populateTableData(project);
            }
        });
        // 删除 sql filter 按钮点击事件
        removeButton.addActionListener(e -> {
            int row = table1.getSelectedRow();
            if (row < 0) {
                Messages.showErrorDialog("Please select the data you want to delete before clicking the delete button.", "Data Selection Required");
                return;
            }
            Object id = tableModel.getValueAt(row, 0);
            SqlLogFilterModel.deleteDataById(project, (Integer) id);

            // 重新加载数据
            populateTableData(project);
        });
        // 编辑 sql filter 按钮点击事件
        editButton.addActionListener(e -> {
            int row = table1.getSelectedRow();
            if (row < 0) {
                Messages.showErrorDialog("Please select the data you want to edit before clicking the edit button.", "Data Selection Required");
                return;
            }
            Object id = tableModel.getValueAt(row, 0);
            Object beforeSql = tableModel.getValueAt(row, 1);
            FilterSqlDialog dialog = new FilterSqlDialog(project);
            dialog.setSqlText((String) beforeSql);

            if (dialog.showAndGet()) {
                String sql = dialog.getSqlText();
                SqlLogFilterModel.updateDataById(project, (Integer) id, sql);

                // 重新加载数据
                populateTableData(project);
            }
        });

        // JTable 填充数据
        populateTableData(project);
    }

    /**
     * Helper method to set column width
     *
     * @param table
     * @param columnIndex
     * @param width
     */
    private static void setColumnWidth(JTable table, int columnIndex, int width) {
        TableColumn column = table.getColumnModel().getColumn(columnIndex);
        column.setPreferredWidth(width);
        column.setMinWidth(width);
        column.setMaxWidth(width);
    }

    /**
     * 填充数据
     *
     * @param project
     */
    private void populateTableData(Project project) {
        // 清空数据
        tableModel.setRowCount(0);
        tableModel.setColumnCount(0);

        // 添加表格列名
        tableModel.addColumn("ID");
        tableModel.addColumn("SQL in this section will be filtered and not displayed.");

        // 添加表格数据
        List<SqlLogFilter> sqlLogFilterList = SqlLogFilterModel.querySqlLogFilter(project);

        for (int i = 0; i < sqlLogFilterList.size(); i++) {
            Vector<Object> rowData = new Vector<>();
            rowData.add(sqlLogFilterList.get(i).getId());
            rowData.add(sqlLogFilterList.get(i).getSql());

            tableModel.addRow(rowData);
        }

        // 设置数据模型
        table1.setRowHeight(40);
        table1.setModel(tableModel);

        setColumnWidth(table1, 0, 0); // Set width of Column 1 to 0 pixels, for hide column
    }

    private void initSqlFilterButton(JButton newButton) {
        // 新增 sql 按钮
        newButton.setPreferredSize(new Dimension(47, 43));
        newButton.setMinimumSize(new Dimension(47, 43));
        newButton.setMaximumSize(new Dimension(47, 43));
        newButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                newButton.setBackground(new Color(76, 80, 82));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                newButton.setBackground(UIManager.getColor("Button.background"));
            }
        });
    }

    private void initTestConnectonButton() {
        // "Test Connection" button 监听
        testConnectionButton.addActionListener(e -> {
            // 处理按钮点击事件的逻辑
            try {
                Socket socket = new Socket(getTargetMysqlIpTextField(), Integer.parseInt(getTargetMysqlPortTextField()));
                socket.close();
                Messages.showInfoMessage("The MySQL connection and port are valid. You can proceed with the configuration.", "Connection Successful");
            } catch (Exception exception) {
                Messages.showErrorDialog("There was an issue with the MySQL connection or port. Please double-check your MySQL server address and port number, and try again.", "Connection Error");
            }
        });
    }

    public String getTargetMysqlIpTextField() {
        return targetMysqlIpTextField.getText();
    }

    public void setTargetMysqlIpTextField(String targetMysqlIpText) {
        this.targetMysqlIpTextField.setText(targetMysqlIpText);
    }

    public String getTargetMysqlPortTextField() {
        return targetMysqlPortTextField.getText();
    }

    public void setTargetMysqlPortTextField(String targetMysqlPort) {
        this.targetMysqlPortTextField.setText(targetMysqlPort);
    }

    public JButton getTestConnectionButton() {
        return testConnectionButton;
    }

    public void setTestConnectionButton(JButton testConnectionButton) {
        this.testConnectionButton = testConnectionButton;
    }

    public JPanel getMysqlPanel() {
        return mysqlPanel;
    }

    public void setMysqlPanel(JPanel mysqlPanel) {
        this.mysqlPanel = mysqlPanel;
    }

    public String getListeningPortTextField() {
        return ListeningPortTextField.getText();
    }

    public void setListeningPortTextField(String listeningPort) {
        ListeningPortTextField.setText(listeningPort);
    }

    public JPanel getFieldSqlPanel() {
        return FieldSqlPanel;
    }

    public void setFieldSqlPanel(JPanel fieldSqlPanel) {
        FieldSqlPanel = fieldSqlPanel;
    }

    public boolean getProxyServerStartWithCheckBox() {
        return proxyServerStartWithCheckBox.isSelected();
    }

    public void setProxyServerStartWithCheckBox(boolean check) {
        this.proxyServerStartWithCheckBox.setSelected(check);
    }

    public JComponent getPanel() {
        return panel;
    }
}
