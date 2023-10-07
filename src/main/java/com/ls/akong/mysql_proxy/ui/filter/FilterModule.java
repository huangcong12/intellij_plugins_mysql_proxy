/*
 * Copyright 2013 Urs Wolfer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ls.akong.mysql_proxy.ui.filter;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.project.Project;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

/**
 * @author Thomas Forrer
 */
public class FilterModule {
    public static ArrayList<AbstractChangesFilter> allFilter() {
        ArrayList<AbstractChangesFilter> filterList = new ArrayList<>();
        filterList.add(new FulltextFilter());
        filterList.add(new DurationFilter());
        filterList.add(new TimeRangeFilter());
        filterList.add(new SqlTypeRangesFilter());

        return filterList;
    }

    public static JPanel createToolbar(final Project project) {
        // 顶部按钮
        JPanel topToolbarPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));

        DefaultActionGroup filterGroup = new DefaultActionGroup();
        for (AbstractChangesFilter filter : FilterModule.allFilter()) {
            filterGroup.add(filter.getAction(project));
            filterGroup.add(new Separator());
        }

        // 将 filterGroup 转换为工具栏组件
        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("TopToolbar", filterGroup, true);
        toolbar.setTargetComponent(topToolbarPanel);

        // 将 filterGroup 中的动作添加到 topToolbarPanel
        topToolbarPanel.add(toolbar.getComponent());

        return topToolbarPanel;
    }
}
