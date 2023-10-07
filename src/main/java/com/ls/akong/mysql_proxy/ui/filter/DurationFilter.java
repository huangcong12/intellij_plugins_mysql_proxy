package com.ls.akong.mysql_proxy.ui.filter;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonShortcuts;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.*;
import com.intellij.openapi.util.Comparing;
import com.intellij.util.Consumer;
import com.ls.akong.mysql_proxy.services.MyTableView;
import com.ls.akong.mysql_proxy.ui.BasePopupAction;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Objects;

public class DurationFilter extends AbstractChangesFilter {
    private static final String POPUP_TEXT = String.format("Enter a number and press %s to search",
            KeymapUtil.getShortcutsText(CommonShortcuts.CTRL_ENTER.getShortcuts()));
    private ImmutableList<DurationFilterType> durationFilterTypes;
    private final JBPopupFactory jbPopupFactory;
    private JBPopup popup;
    private AnAction selectOkAction;
    private JTextArea selectFilterTypeTextArea;
    private Optional<DurationFilterType> value = Optional.absent();

    private String operation; // 新增操作类型字段

    private String durationFilterValue = "";

    public DurationFilter() {
        this.jbPopupFactory = JBPopupFactory.getInstance();
    }


    @Override
    public AnAction getAction(final Project project) {
        durationFilterTypes = ImmutableList.of(
                new DurationFilterType("No Limit", "No Limit"),
                new DurationFilterType("<=", ""),
                new DurationFilterType(">=", ""),
                new DurationFilterType("=", "")
        );
        value = Optional.of(durationFilterTypes.get(0));

        return new DurationFilterPopupAction(project, "Duration Filter");
    }

    @Override
    @Nullable
    public String getSearchQueryPart() {
        return null;
    }

    private boolean isNotLimit(DurationFilterType type) {
        return type.label == "No Limit";
    }

    private static final class DurationFilterType {
        String label;
        String type;

        private DurationFilterType(String label, String type) {
            this.label = label;
            this.type = type;
        }
    }

    public final class DurationFilterPopupAction extends BasePopupAction {
        private final Project project;

        public DurationFilterPopupAction(Project project, String labelText) {
            super(labelText);
            this.project = project;
            updateFilterValueLabel(value.get().label);
        }

        @Override
        protected void createActions(Consumer<AnAction> actionConsumer) {
            selectFilterTypeTextArea = new JTextArea();
            // 限制用户只能输入数字
            selectFilterTypeTextArea.addKeyListener(new KeyListener() {
                @Override
                public void keyTyped(KeyEvent e) {
                    char c = e.getKeyChar();
                    if (!(Character.isDigit(c) || c == KeyEvent.VK_BACK_SPACE || c == KeyEvent.VK_DELETE)) {
                        e.consume(); // 阻止输入非数字字符
                    }
                }

                @Override
                public void keyPressed(KeyEvent e) {
                    // 不需要处理
                }

                @Override
                public void keyReleased(KeyEvent e) {
                    // 不需要处理
                }
            });

            selectOkAction = buildOkAction();

            for (final DurationFilterType filter : durationFilterTypes) {
                actionConsumer.consume(new DumbAwareAction(filter.label) {
                    @Override
                    public void actionPerformed(AnActionEvent e) {
                        operation = filter.label;

                        // 如果是第一个 No Limit 不用处理后面的
                        if (isNotLimit(filter)) {
                            change(filter);
                            return;
                        }

                        popup = buildBalloon(selectFilterTypeTextArea);
                        Point point = new Point(0, 35);
                        SwingUtilities.convertPointToScreen(point, getFilterValueLabel());
                        popup.showInScreenCoordinates(getFilterValueLabel(), point);
                        final JComponent content = popup.getContent();
                        selectOkAction.registerCustomShortcutSet(CommonShortcuts.CTRL_ENTER, content);
                        popup.addListener(new JBPopupListener() {
                            @Override
                            public void beforeShown(LightweightWindowEvent lightweightWindowEvent) {
                            }

                            @Override
                            public void onClosed(LightweightWindowEvent event) {
                                selectOkAction.unregisterCustomShortcutSet(content);
                            }
                        });
                    }
                });
            }

        }

        private void change(DurationFilterType type) {
            value = Optional.of(type);
            String labelValue = operation + (!isNotLimit(type) ? " " + type.label + " ms" : "");
            updateFilterValueLabel(labelValue);
            setChanged();
            notifyObservers(project);

            // 判断是否有变化，如果有则更新
            if (Objects.equals(durationFilterValue, labelValue)) {
                return;
            }
            durationFilterValue = labelValue;

            // 更新操作
            MyTableView tableView = MyTableView.getInstance(project);
            MyTableView.MyTableModel myTableModel = tableView.getTableModel();
            myTableModel.setDurationFilter(isNotLimit(type) ? "" : operation + type.label);
            tableView.refreshData();
        }

        private AnAction buildOkAction() {
            return new AnAction() {
                public void actionPerformed(AnActionEvent e) {
                    popup.closeOk(e.getInputEvent());
                    String newText = selectFilterTypeTextArea.getText().trim();
                    if (newText.isEmpty()) {
                        return;
                    }
                    if (!Comparing.equal(newText, getFilterValueLabel().getText(), true)) {
                        DurationFilterType type = new DurationFilterType(newText, newText);
                        change(type);
                    }
                }
            };
        }

        private JBPopup buildBalloon(JTextArea textArea) {
            ComponentPopupBuilder builder = jbPopupFactory.createComponentPopupBuilder(textArea, textArea);
            builder.setAdText(POPUP_TEXT);
            builder.setResizable(true);
            builder.setMovable(true);
            builder.setRequestFocus(true);
            return builder.createPopup();
        }
    }
}
