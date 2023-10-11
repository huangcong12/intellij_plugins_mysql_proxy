package com.ls.akong.mysql_proxy.util;

import com.intellij.ui.JBColor;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

public class CustomTableCellRenderer extends DefaultTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        // 设置某一行的背景色
        boolean isChangeBackgroundColor = false;
        if (isSelected) {
            component.setBackground(Color.decode("#2675bf")); // 设置选中行的背景色为默认的选中行背景色
            component.setForeground(JBColor.WHITE); // 设置选中行的文字颜色为默认的选中行文字颜色
            isChangeBackgroundColor = true;
        } else if (column == 2) {
            long spendTime = convertTimeStringToMillis(value.toString());
            if (spendTime > 999) {      // 大于 1s 是慢查询
                isChangeBackgroundColor = true;
                component.setBackground(JBColor.YELLOW);
                component.setForeground(JBColor.BLACK); // 设置文字颜色为默认的文字颜色
            }
        }

        // 默认的白底黑字
        if (!isChangeBackgroundColor) {
            component.setBackground(table.getBackground()); // 设置其他行的背景色为默认的表格背景色
            component.setForeground(table.getForeground()); // 设置其他行的文字颜色为默认的文字颜色

        }

        return component;
    }

    private long convertTimeStringToMillis(String timeString) {
        // 分割字符串，以空格为分隔符
        String[] parts = timeString.split(" ");

        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid time format");
        }

        // 提取时间值和单位
        double timeValue = Double.parseDouble(parts[0]);
        String unit = parts[1].trim();

        // 根据单位将时间值转换为毫秒
        if (unit.equals("ms")) {
            // 毫秒不需要转换
            return (long) timeValue;
        } else if (unit.equals("s")) {
            // 秒转换为毫秒
            return (long) (timeValue * 1000);
        } else {
            throw new IllegalArgumentException("Unsupported time unit: " + unit);
        }
    }
}
