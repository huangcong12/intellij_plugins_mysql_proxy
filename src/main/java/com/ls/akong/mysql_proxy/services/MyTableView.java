package com.ls.akong.mysql_proxy.services;

import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.ls.akong.mysql_proxy.entity.SqlLog;
import com.ls.akong.mysql_proxy.model.SqlLogFilterModel;
import com.ls.akong.mysql_proxy.model.SqlLogModel;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Timer;
import java.util.*;


@Service(Service.Level.PROJECT)
public final class MyTableView extends JPanel {
    private final MyTableModel tableModel;

    private final JBTable table;

    private Timer debounceTimer = new Timer();

    private MyTableView(Project project) {
        tableModel = new MyTableModel(project);
        table = new JBTable(tableModel);

        // 增加右键菜单
        JPopupMenu popupMenu = new JPopupMenu();
        // 复制
        JMenuItem copyItem = new JMenuItem("Copy Sql", AllIcons.Actions.Copy);
        copyItem.addActionListener(e -> {
            // 复制当前选中的单元格的数据
            int row = table.getSelectedRow();
            Object id = table.getValueAt(row, 0);

            StringSelection stringSelection = new StringSelection(tableModel.getSqlById((Integer) id));
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(stringSelection, null);
        });
        // 删除
        JMenuItem deleteItem = new JMenuItem("Delete", AllIcons.Actions.DeleteTagHover);
        deleteItem.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow != -1) {
                Object id = table.getModel().getValueAt(selectedRow, 0); // 假设 id 是表的第一列
                // 调用数据库进行删除
                SqlLogModel.deleteDataById(project, (Integer) id);

                // 删除展示的 table View 数据
                tableModel.removeDataByRowId(selectedRow);
                // 重新加载
                SwingUtilities.invokeLater(tableModel::fireTableDataChanged);
            }
        });
        // 添加到过滤
        JMenuItem ignoreSqlLogItem = new JMenuItem("Add to Filtered SQL Log", AllIcons.General.Add);    // Icons.ADD_ICON
        ignoreSqlLogItem.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow != -1) {
                Object id = table.getModel().getValueAt(selectedRow, 0); // 假设 id 是表的第一列

                // 调用数据库进行删除
                SqlLogFilterModel.insertLogFilter(project, tableModel.getSqlById((Integer) id));

                // 删除展示的 table View 数据
                tableModel.removeDataByRowId(selectedRow);
                // 重新加载
                SwingUtilities.invokeLater(tableModel::fireTableDataChanged);
            }
        });

        // EverSQL 查询分析
        JMenuItem optimizeWithEverSql = new JMenuItem("Optimize with EverSQL (Premium)");    // Icons.ADD_ICON
        optimizeWithEverSql.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow != -1) {
                Object id = table.getModel().getValueAt(selectedRow, 0); // 假设 id 是表的第一列
                String sql = tableModel.getSqlById((Integer) id);

                String url = "https://www.eversql.com/sql-query-optimizer/?utm_source=plugin&utm_campaign=jetbrains&query=" + Base64.getEncoder().encodeToString(sql.getBytes());
                BrowserUtil.browse(url);
            }
        });

        popupMenu.add(copyItem);
        popupMenu.addSeparator();
        popupMenu.add(optimizeWithEverSql);
        popupMenu.addSeparator();
        popupMenu.add(ignoreSqlLogItem);
        popupMenu.add(deleteItem);

        table.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    // 只设置选择，不显示菜单
                    int row = table.rowAtPoint(e.getPoint());
                    if (row < 0) {
                        return;
                    }
                    int col = table.columnAtPoint(e.getPoint());
                    table.setRowSelectionInterval(row, row);
                    table.setColumnSelectionInterval(col, col);
                }
            }

            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    // 显示菜单
                    int row = table.rowAtPoint(e.getPoint());
                    if (row < 0) {
                        return;
                    }
                    int col = table.columnAtPoint(e.getPoint());
                    table.setRowSelectionInterval(row, row);
                    table.setColumnSelectionInterval(col, col);
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });

        // 将表格放置在 JScrollPane 中
        JScrollPane scrollPane = new JBScrollPane(table);

        // 设置布局
        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);

        // 在渲染完成后设置列宽度
        SwingUtilities.invokeLater(() -> adjustColumnWidths(table));
        // 鼠标往下滚，实现向下翻页
        scrollPane.getVerticalScrollBar().addAdjustmentListener(e -> {
            if (e.getValue() == e.getAdjustable().getMaximum() - e.getAdjustable().getVisibleAmount()) {
                // 滚动到底部，加载下一页数据
                tableModel.nextPage();
            }
        });
    }

    public static MyTableView getInstance(Project project) {
        return project.getService(MyTableView.class);
    }

    /**
     * 展示后回调设置 TableView 每一列宽度
     *
     * @param table
     */
    private void adjustColumnWidths(JBTable table) {
        // 重新根据当前编辑器大小设置每列的宽度
        int totalWidth = table.getWidth();
        TableColumnModel columnModel = table.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(totalWidth * 7 / 100);
        columnModel.getColumn(1).setPreferredWidth(totalWidth * 70 / 100);
        columnModel.getColumn(2).setPreferredWidth(totalWidth * 5 / 100);
        columnModel.getColumn(3).setPreferredWidth(totalWidth * 14 / 100);
    }

    // 返回顶部
    public void scrollToTop() {
        table.scrollRectToVisible(new Rectangle(0, 0, 0, 0));
    }

    // 在插入新数据后调用此方法以更新 TableView
    public void updateData() {
        // 取消之前的定时任务
        debounceTimer.cancel();
        // 创建一个新的定时任务，在指定时间后执行通知操作
        debounceTimer = new Timer();
        debounceTimer.schedule(new TimerTask() {
            /**
             *  延时执行，防抖动。兼容瞬间来太多数据的情况
             */
            @Override
            public void run() {
                int preRefreshDataCount = tableModel.preRefreshData();
                if (preRefreshDataCount == 0) {
                    return;
                }

                // 在 EDT 更新表格
                SwingUtilities.invokeLater(tableModel::fireTableDataChanged);
            }
        }, 100);
    }

    // 刷新数据，从第一页开始
    public void refreshData() {
        tableModel.refreshData();
        SwingUtilities.invokeLater(tableModel::fireTableDataChanged);  // 在 EDT 更新表格
    }

    public MyTableModel getTableModel() {
        return tableModel;
    }

    // 自定义 TableModel 类
    public static class MyTableModel extends AbstractTableModel {
        private final Project project;
        private final int pageSize = 50; // 每页显示的数据条数
        private List<SqlLog> data;  // 数据集
        private String searchText = "";   // 搜索框
        private String selectedTimeRange = "No Limit";  // 时间限制

        private String sqlType = "All"; // sql 类型限制

        public MyTableModel(Project project) {
            this.project = project;
            refreshData();
        }

        private int getFirstItemId() {
            if (data.size() == 0) {
                return 0;
            }

            try {
                return data.get(0).getId();
            } catch (IndexOutOfBoundsException e) {
                return 0;
            }
        }

        public void setSearchText(String searchText) {
            this.searchText = searchText;
        }

        public void setSelectedTimeRange(String selectedTimeRange) {
            this.selectedTimeRange = selectedTimeRange;
        }

        public void setSelectedSqlType(String sqlType) {
            this.sqlType = sqlType;
        }

        /**
         * 加载数据，首次启动会调用这个方法
         */
        public synchronized void refreshData() {
            data = SqlLogModel.queryLogs(project, searchText, selectedTimeRange, sqlType, 0, 0, pageSize);
        }

        /**
         * 获取最后一页的 id
         *
         * @return integer
         */
        private int getLastItemId() {
            if (data.size() == 0) {
                return 0;
            }

            try {
                return data.get(data.size() - 1).getId();
            } catch (IndexOutOfBoundsException e) {
                return 0;
            }
        }

        /**
         * 加载数据，sql_log 有新增数据的时候，会调用这个方法
         */
        public synchronized int preRefreshData() {
            List<SqlLog> newDataList = SqlLogModel.queryLogs(project, searchText, selectedTimeRange, sqlType, 0, getFirstItemId(), pageSize);
            if (newDataList.isEmpty()) {  // 兼容这些 SQL 已被添加到过滤表的场景
                return 0;
            }

            int newDataListCount = newDataList.size();
            // 不是时间搜索的，才采用补数据的方式
            if (Objects.equals(selectedTimeRange, "No Limit")) {
                newDataList.addAll(data);
                data.clear();
                data.addAll(newDataList);
            } else {
                // 时间搜索，直接搜所有，因为数据有时效性
                data = newDataList;
            }

            return newDataListCount;
        }

        @Override
        public int getRowCount() {
            return data.size();
        }

        @Override
        public int getColumnCount() {
            return 4;  // 假设有 3 列
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            try {
                SqlLog item = data.get(rowIndex);
                switch (columnIndex) {
                    case 0:
                        return item.getId();
                    case 1:
                        String sql = item.getSql();
                        if (sql.length() > 1000) {
                            // 截取前300个字符并添加三个点
                            return sql.substring(0, 300) + "...(Right-click and select 'Copy SQL' to copy everything.)";
                        } else {
                            return sql;
                        }
                    case 2:
                        return item.getExecutionTime();

                    case 3:
                        return item.getCreatedAt();
                    default:
                        return null;
                }
            } catch (IndexOutOfBoundsException e) {
                return null;
            }
        }

        @Override
        public String getColumnName(int columnIndex) {
            String[] columnNames = {"Sequence", "SQL", "Duration", "Date"};
            return columnNames[columnIndex];
        }

        /**
         * 下一页
         */
        public synchronized void nextPage() {
            int lastItemId = getLastItemId();
            if (lastItemId == 0) {  // 也不清楚为什么会走到这里，但是确实是有进来的
                return;
            }

            List<SqlLog> list = SqlLogModel.queryLogs(project, searchText, selectedTimeRange, sqlType, lastItemId, 0, pageSize);
            if (list.size() == 0) {
                return;
            }

            // 时间段搜索的话，不分页
            if (!Objects.equals(selectedTimeRange, "No Limit")) {
                data.clear();
                data.addAll(list);
                return;
            }

            data.addAll(list);
        }

        public void removeDataByRowId(int rowId) {
            data.remove(rowId);
        }

        public String getSqlById(int id) {
            SqlLog sqlLog = SqlLogModel.getById(project, id);
            assert sqlLog != null;
            return sqlLog.getSql();
        }

        public List<SqlLog> data() {
            return data;
        }
    }
}
