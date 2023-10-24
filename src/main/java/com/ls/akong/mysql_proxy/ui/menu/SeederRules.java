package com.ls.akong.mysql_proxy.ui.menu;

import com.intellij.openapi.project.Project;
import com.ls.akong.mysql_proxy.ui.editor.CustomTableEditor;

import javax.swing.*;

public class SeederRules extends AbstractMenuItem {
    public SeederRules(Project project) {
        super(project);
    }

    public JMenuItem getMenuItem() {
        JMenuItem seederRules = new JMenuItem("Seeder Rules");
        seederRules.addActionListener(e -> {
            System.out.println("Seeder Rules 选中了" + getTableName());
            CustomTableEditor.show(getProject(), getTableName());
        });

        return seederRules;
    }

}
