package com.ls.akong.mysql_proxy.util;

/**
 * 填充数据左边 Table CheckBox 用
 */
public class CheckBoxNode {
    String text;
    boolean selected;

    public CheckBoxNode(String text, boolean selected) {
        this.text = text;
        this.selected = selected;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean newValue) {
        selected = newValue;
    }

    public String getText() {
        return text;
    }

    public void setText(String newValue) {
        text = newValue;
    }

    public String toString() {
        return getClass().getName() + "[" + text + "/" + selected + "]";
    }
}

