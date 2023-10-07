package com.ls.akong.mysql_proxy.ui.filter;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.util.Consumer;
import com.ls.akong.mysql_proxy.ui.BasePopupAction;
import org.jetbrains.annotations.Nullable;

public class TimeRangeFilter extends AbstractChangesFilter {

    private ImmutableList<TimeRange> TimeRanges;
    private Optional<TimeRange> value = Optional.absent();


    @Override
    public AnAction getAction(final Project project) {
        TimeRanges = ImmutableList.of(
                new TimeRange("No Limit", "0"),
                new TimeRange("Within 10s", "10"),
                new TimeRange("Within 1m", "60"),
                new TimeRange("Within 5m", "300"),
                new TimeRange("Within 10m", "600")
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
        String type;

        private TimeRange(String label, String type) {
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
        }
    }
}
