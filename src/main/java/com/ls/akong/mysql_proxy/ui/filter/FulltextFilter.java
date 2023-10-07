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

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.ui.SearchFieldAction;
import com.ls.akong.mysql_proxy.services.MyTableView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Thomas Forrer
 */
public class FulltextFilter extends AbstractChangesFilter {

    private String value = "";


    @Override
    public AnAction getAction(final Project project) {
        return new SearchFieldAction("Filter: ") {
            @Override
            public void actionPerformed(AnActionEvent event) {
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                String newValue = getText().trim();
                if (!isNewValue(newValue)) {
                    return;
                }
                value = newValue;

                // 条件搜索
                MyTableView tableView = MyTableView.getInstance(project);
                MyTableView.MyTableModel myTableModel = tableView.getTableModel();
                myTableModel.setSearchText(value);
                tableView.refreshData();
            }

            private boolean isNewValue(String newValue) {
                return !newValue.equals(value);
            }
        };
    }

    @Override
    @Nullable
    public String getSearchQueryPart() {
        return null;
    }
}
