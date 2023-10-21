package com.ls.akong.mysql_proxy.ui.menu;

import javax.swing.*;

public class SeederRules extends AbstractMenuItem {
    public JMenuItem getMenuItem() {
        JMenuItem seederRules = new JMenuItem("Seeder Rules");
        seederRules.addActionListener(e -> {
            System.out.println("Seeder Rules 选中了" + getTableName());
        });

        return seederRules;
    }

}
