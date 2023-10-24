package com.ls.akong.mysql_proxy.ui.menu;

import com.intellij.openapi.project.Project;

import javax.swing.*;
import java.util.ArrayList;

public class DatabaseSeederPopupMenuModel {

    public static ArrayList<AbstractMenuItem> allPopupMenu(Project project) {
        ArrayList<AbstractMenuItem> popupMenus = new ArrayList<>();
        popupMenus.add(new SeederRules(project));

        return popupMenus;
    }

    public static JPopupMenu createPopupMenu(Project project, String tableName) {
        JPopupMenu popupMenu = new JPopupMenu();

        for (AbstractMenuItem menuItem : allPopupMenu(project)) {
            menuItem.setTableName(tableName);
            popupMenu.add(menuItem.getMenuItem());
        }

        return popupMenu;
    }
}
