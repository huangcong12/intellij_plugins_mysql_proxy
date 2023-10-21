package com.ls.akong.mysql_proxy.util;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;

/**
 * 填充数据左边 Table CheckBox 用
 */
public class CheckBoxNodeRenderer implements TreeCellRenderer {
    private final JCheckBox leafRenderer = new JCheckBox();
    private DefaultTreeCellRenderer nonLeafRenderer = new DefaultTreeCellRenderer();
    private Color selectionBorderColor, selectionForeground, selectionBackground, textForeground, textBackground;

    protected JCheckBox getLeafRenderer() {
        return leafRenderer;
    }

    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        Component returnValue;
        if (leaf) {

            String stringValue = tree.convertValueToText(value, selected, expanded, leaf, row, false);
            leafRenderer.setText(stringValue);
            leafRenderer.setSelected(false);

            leafRenderer.setEnabled(tree.isEnabled());

            leafRenderer.setFocusPainted(false); // 移除焦点绘制
            leafRenderer.setBorderPainted(false); // 移除边框绘制
            leafRenderer.setBorder(null); // 设置边框为 null

            if (selected) {
                leafRenderer.setForeground(selectionForeground);
                leafRenderer.setBackground(selectionBackground);
            } else {
                leafRenderer.setForeground(textForeground);
                leafRenderer.setBackground(textBackground);
            }

            if ((value != null) && (value instanceof DefaultMutableTreeNode)) {
                Object userObject = ((DefaultMutableTreeNode) value).getUserObject();
                if (userObject instanceof CheckBoxNode) {
                    CheckBoxNode node = (CheckBoxNode) userObject;
                    leafRenderer.setText(node.getText());
                    leafRenderer.setSelected(node.isSelected());
                }
            }
            returnValue = leafRenderer;
        } else {
            returnValue = nonLeafRenderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
        }
        return returnValue;
    }
}


