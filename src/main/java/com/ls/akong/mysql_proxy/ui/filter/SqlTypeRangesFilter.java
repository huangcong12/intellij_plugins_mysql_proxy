package com.ls.akong.mysql_proxy.ui.filter;

import com.google.common.collect.ImmutableList;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.util.Consumer;
import com.ls.akong.mysql_proxy.ui.BasePopupAction;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class SqlTypeRangesFilter extends AbstractChangesFilter {

    private ImmutableList<SqlTypeRange> sqlTypeRanges;
    private Optional<SqlTypeRange> value = Optional.empty();

    @Override
    public AnAction getAction(final Project project) {
        sqlTypeRanges = ImmutableList.of(
                new SqlTypeRange("All", "All"),
                new SqlTypeRange("Select", "Select"),
                new SqlTypeRange("Insert", "Insert"),
                new SqlTypeRange("Update", "Update"),
                new SqlTypeRange("Delete", "Delete"),
                new SqlTypeRange("Other", "Other")
        );
        value = Optional.of(sqlTypeRanges.get(0));

        return new TimeRangeFilterPopupAction(project, "Sql Type Ranges");
    }

    @Override
    @Nullable
    public String getSearchQueryPart() {
        return null;
    }

    private static final class SqlTypeRange {
        String label;
        String type;

        private SqlTypeRange(String label, String type) {
            this.label = label;
            this.type = type;
        }
    }

    public final class TimeRangeFilterPopupAction extends BasePopupAction {
        private final Project project;

        public TimeRangeFilterPopupAction(Project project, String labelText) {
            super(labelText);
            this.project = project;
            updateFilterValueLabel(value.get().label);
        }

        @Override
        protected void createActions(Consumer<AnAction> actionConsumer) {
            for (final SqlTypeRange sqlTypeRange : sqlTypeRanges) {
                actionConsumer.consume(new DumbAwareAction(sqlTypeRange.label) {
                    @Override
                    public void actionPerformed(AnActionEvent e) {
                        change(sqlTypeRange);
                    }
                });
            }
        }

        private void change(SqlTypeRange sqlTypeRange) {
            value = Optional.of(sqlTypeRange);
            updateFilterValueLabel(sqlTypeRange.label);
            setChanged();
            notifyObservers(project);
        }
    }
}
