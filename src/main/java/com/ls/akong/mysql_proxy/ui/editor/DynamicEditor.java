package com.ls.akong.mysql_proxy.ui.editor;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;

public class DynamicEditor extends AbstractCellEditor implements TableCellEditor {
    private JPanel panel;
    private JComboBox<String> comboBox;
    private JTextField textField1;
    private JTextField textField2;

    public DynamicEditor() {
        panel = new JPanel();
        comboBox = new JComboBox<>(new String[]{"Rule1", "Rule2", "Rule3"});
        textField1 = new JTextField();
        textField2 = new JTextField();

        // 监听下拉框的选择事件
        comboBox.addActionListener(e -> {
            String rule = (String) comboBox.getSelectedItem();
            panel.removeAll();
            panel.add(comboBox);
            if ("Rule1".equals(rule)) {
                panel.add(textField1);
            } else if ("Rule2".equals(rule) || "Rule3".equals(rule)) {
                panel.add(textField1);
                panel.add(textField2);
            }
            panel.revalidate();
            panel.repaint();
        });

        panel.add(comboBox);
    }

    @Override
    public Object getCellEditorValue() {
        String rule = (String) comboBox.getSelectedItem();
        if ("Rule1".equals(rule)) {
            return rule + ":" + textField1.getText();
        } else if ("Rule2".equals(rule) || "Rule3".equals(rule)) {
            return rule + ":" + textField1.getText() + "-" + textField2.getText();
        }
        return null;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        return panel;
    }
}

