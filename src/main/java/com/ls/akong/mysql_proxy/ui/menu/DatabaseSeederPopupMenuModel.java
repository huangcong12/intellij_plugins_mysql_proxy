package com.ls.akong.mysql_proxy.ui.menu;

import javax.swing.*;
import java.util.ArrayList;

public class DatabaseSeederPopupMenuModel {

    public static ArrayList<AbstractMenuItem> allPopupMenu() {
        ArrayList<AbstractMenuItem> popupMenus = new ArrayList<>();
        popupMenus.add(new SeederRules());

        return popupMenus;
    }

    public static JPopupMenu createPopupMenu(String tableName) {
        JPopupMenu popupMenu = new JPopupMenu();

        for (AbstractMenuItem menuItem : allPopupMenu()) {
            menuItem.setTableName(tableName);
            popupMenu.add(menuItem.getMenuItem());
        }

        return popupMenu;
    }
}
