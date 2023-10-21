package com.ls.akong.mysql_proxy.util;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.tree.TreeCellEditor;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.util.EventObject;

/**
 * 填充数据左边 Table CheckBox 用
 */
public class CheckBoxNodeEditor extends AbstractCellEditor implements TreeCellEditor {
    CheckBoxNodeRenderer renderer = new CheckBoxNodeRenderer();
    ChangeEvent changeEvent = null;
    JTree tree;

    public CheckBoxNodeEditor(JTree tree) {
        this.tree = tree;
    }

    public Object getCellEditorValue() {
        JCheckBox checkbox = renderer.getLeafRenderer();
        CheckBoxNode checkBoxNode = new CheckBoxNode(checkbox.getText(), checkbox.isSelected());
        return checkBoxNode;
    }

    public boolean isCellEditable(EventObject event) {
        boolean returnValue = false;
        if (event instanceof MouseEvent) {
            MouseEvent mouseEvent = (MouseEvent) event;
            Component source = mouseEvent.getComponent();
            // 假如调整了表的渲染组件，需要调整这里
            if (source instanceof JTree) {
                returnValue = true;
            }
        }
        return returnValue;
    }

    public Component getTreeCellEditorComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row) {
        Component editor = renderer.getTreeCellRendererComponent(tree, value, true, expanded, leaf, row, true);
        // editor always selected / focused
        ItemListener itemListener = new ItemListener() {
            public void itemStateChanged(ItemEvent itemEvent) {
                if (stopCellEditing()) {
                    fireEditingStopped();
                }
            }
        };
        if (editor instanceof JCheckBox) {
            ((JCheckBox) editor).addItemListener(itemListener);
        }
        return editor;
    }
}
