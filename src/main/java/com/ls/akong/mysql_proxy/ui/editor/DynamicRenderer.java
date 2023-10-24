package com.ls.akong.mysql_proxy.ui.editor;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

public class DynamicRenderer extends DefaultTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (value != null) {
            String[] values = value.toString().split(":");
            String rule = values[0];
            JPanel panel = new JPanel();
            JComboBox<String> comboBox = new JComboBox<>(new String[]{"Rule1", "Rule2", "Rule3"});
            comboBox.setSelectedItem(rule);
            panel.add(comboBox);
            if ("Rule1".equals(rule)) {
                JTextField textField1 = new JTextField();
                textField1.setText(values[1]);
                panel.add(textField1);
            } else if ("Rule2".equals(rule) || "Rule3".equals(rule)) {
                String[] textValues = values[1].split("-");
                JTextField textField1 = new JTextField();
                textField1.setText(textValues[0]);
                JTextField textField2 = new JTextField();
                textField2.setText(textValues[1]);
                panel.add(textField1);
                panel.add(textField2);
            }
            return panel;
        } else {
            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
    }
}

