package com.ls.akong.mysql_proxy.ui.filter;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.util.Consumer;
import com.ls.akong.mysql_proxy.services.MyTableView;
import com.ls.akong.mysql_proxy.ui.BasePopupAction;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class TimeRangeFilter extends AbstractChangesFilter {

    private ImmutableList<TimeRange> TimeRanges;
    private Optional<TimeRange> value = Optional.absent();

    private int timeRangeValue = 0;

    @Override
    public AnAction getAction(final Project project) {
        TimeRanges = ImmutableList.of(
                new TimeRange("All", 0),
                new TimeRange("Within 10s", 10000),
                new TimeRange("Within 1m", 60000),
                new TimeRange("Within 5m", 300000),
                new TimeRange("Within 10m", 600000)
        );
        value = Optional.of(TimeRanges.get(0));

        return new TimeRangeFilterPopupAction(project, "Recent Data");
    }


    @Override
    @Nullable
    public String getSearchQueryPart() {
        return null;
    }

    private static final class TimeRange {
        String label;
        int value;

        private TimeRange(String label, int value) {
            this.label = label;
            this.value = value;
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
            for (final TimeRange timeRange : TimeRanges) {
                actionConsumer.consume(new DumbAwareAction(timeRange.label) {
                    @Override
                    public void actionPerformed(AnActionEvent e) {
                        change(timeRange);
                    }
                });
            }

        }

        private void change(TimeRange timeRanges) {
            value = Optional.of(timeRanges);
            updateFilterValueLabel(timeRanges.label);
            setChanged();
            notifyObservers(project);

            // 判断是否有变动
            if (Objects.equals(timeRangeValue, timeRanges.value)) {
                return;
            }
            timeRangeValue = timeRanges.value;

            // 更新 TableView
            MyTableView tableView = MyTableView.getInstance(project);
            MyTableView.MyTableModel myTableModel = tableView.getTableModel();
            myTableModel.setSelectedTimeRange(timeRangeValue);
            tableView.refreshData();
        }
    }
}
