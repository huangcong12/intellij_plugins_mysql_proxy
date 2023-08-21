package com.ls.akong.mysql_proxy.ui;

import com.intellij.openapi.project.Project;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class FilterSqlDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField sql;

    private Project project;

    private boolean confirmed = false;

    public FilterSqlDialog(Project project) {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
               public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void onOK() {
        confirmed = true;
        // add your code here
        dispose();
    }

    private void onCancel() {
        // add your code here if necessary
        confirmed = false;
        dispose();
    }

    public boolean showAndGet() {
        setTitle("Add New Filtering SQL");
        setMinimumSize(new Dimension(800, 100));
        pack(); // 调整对话框的大小
        setLocationRelativeTo(null); // 居中显示
        setModal(true); // 设置为模态对话框
        setVisible(true); // 显示对话框
        return confirmed;
    }

    public String getSqlText() {
        return sql.getText();
    }

    public void setSqlText(String sql) {
        this.sql.setText(sql);
    }
}
