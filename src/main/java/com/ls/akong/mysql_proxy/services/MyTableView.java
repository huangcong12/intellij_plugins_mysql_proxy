package com.ls.akong.mysql_proxy.services;

import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.ls.akong.mysql_proxy.entity.SqlLog;
import com.ls.akong.mysql_proxy.model.SqlLogFilterModel;
import com.ls.akong.mysql_proxy.model.SqlLogModel;
import com.ls.akong.mysql_proxy.ui.action.RecordingSwitchAction;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Timer;
import java.util.*;


@Service(Service.Level.PROJECT)
public final class MyTableView extends JPanel {
    private final MyTableModel tableModel;

    private final JBTable table;
    private final int maxDataCount = 100; // 设置一次刷新的最大数据量
    private final long refreshInterval = 100; // 设置刷新的时间间隔（毫秒）
    private final Project project;
    private Timer debounceTimer = new Timer();
    private int dataCount = 0; // 数据计数器

    private MyTableView(Project project) {
        tableModel = new MyTableModel(project);
        table = new JBTable(tableModel);

        this.project = project;

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

        // phind 查询分析
        JMenuItem optimizeWithPhind = new JMenuItem("Optimize with Phind(Free GPT 3.5)");    // Icons.ADD_ICON
        optimizeWithPhind.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow != -1) {
                Object id = table.getModel().getValueAt(selectedRow, 0); // 假设 id 是表的第一列
                String sql = tableModel.getSqlById((Integer) id);

                // 获取默认的系统区域设置
                Locale defaultLocale = Locale.getDefault();
                String systemLanguage = defaultLocale.getLanguage();

                String question = "I have an SQL query \n```\n" + sql + "\n```\n could you please check if there's any room for optimization?" + (systemLanguage.equals("") ? "" : "Please answer me in " + systemLanguage);

                String url = "https://www.phind.com/agent?q=" + URLEncoder.encode(question, StandardCharsets.UTF_8) + "&source=searchbox";
                BrowserUtil.browse(url);
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

        // 返回顶部
        Icon scrollToTopIcon = IconLoader.getIcon("/icons/top.svg", RecordingSwitchAction.class);
        JMenuItem scrollToTop = new JMenuItem("Scroll To Top", scrollToTopIcon);
        scrollToTop.addActionListener(e -> this.scrollToTop());

        popupMenu.add(copyItem);
        popupMenu.addSeparator();
        popupMenu.add(optimizeWithPhind);
        popupMenu.add(optimizeWithEverSql);
        popupMenu.addSeparator();
        popupMenu.add(ignoreSqlLogItem);
        popupMenu.add(deleteItem);
        popupMenu.addSeparator();
        popupMenu.add(scrollToTop);

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

    /**
     * 在插入新数据后调用此方法以更新 TableView
     * 每次调用延迟 100 ms 刷新；假如满 100 条数据也刷新
     */
    public void updateData() {
        dataCount++; // 增加数据计数

        if (dataCount >= maxDataCount) {
            preRefreshData(); // 达到最大数据量，执行刷新操作
        } else {
            try {
                debounceTimer.cancel(); // 取消之前的定时任务

                debounceTimer = new Timer();
                debounceTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        preRefreshData(); // 在指定时间后执行刷新操作
                    }
                }, refreshInterval);
            } catch (IllegalStateException e) {
                // 不处理，等下一个线程来了再刷新吧
            }
        }
    }

    /**
     * 更新执行时间字段
     *
     * @param id
     */
    public void updateExecutionTimeById(int id) {
        SqlLog sqlLog = SqlLogModel.getById(project, id);
        if (sqlLog == null) {
            return;
        }
        this.tableModel.updateExecuteTime(sqlLog);

        SwingUtilities.invokeLater(tableModel::fireTableDataChanged);
    }

    /**
     * 刷新前置数据
     */
    private synchronized void preRefreshData() {
        dataCount = 0; // 重置数据计数
        int preRefreshDataCount = tableModel.preRefreshData();
        if (preRefreshDataCount == 0) {
            return;
        }

//        ApplicationManager.getApplication().invokeLater(tableModel::fireTableDataChanged);
        SwingUtilities.invokeLater(tableModel::fireTableDataChanged);
    }

    // 刷新数据，从第一页开始
    public void refreshData() {
        tableModel.refreshData();
        SwingUtilities.invokeLater(tableModel::fireTableDataChanged);
    }

    public MyTableModel getTableModel() {
        return tableModel;
    }

    // 自定义 TableModel 类
    public static class MyTableModel extends AbstractTableModel {
        private final Project project;
        private final int pageSize = 100; // 每页显示的数据条数
        private ArrayList<SqlLog> data;  // 数据集
        private String searchText = "";   // 搜索框
        private String durationFilter = "";// 执行时间
        private int selectedTimeRange = 0;  // 最近执行时间条件搜索
        private String sqlType = "All"; // sql 类型限制

        public MyTableModel(Project project) {
            this.project = project;
            refreshData();
        }

        public void setDurationFilter(String durationFilter) {
            this.durationFilter = durationFilter.trim();
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

        public void setSelectedTimeRange(int selectedTimeRange) {
            this.selectedTimeRange = selectedTimeRange;
        }

        public void setSelectedSqlType(String sqlType) {
            this.sqlType = sqlType;
        }

        /**
         * 加载数据，首次启动会调用这个方法
         */
        public synchronized void refreshData() {
            data = SqlLogModel.queryLogs(project, searchText, durationFilter, selectedTimeRange, sqlType, 0, 0, pageSize);
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
            ArrayList<SqlLog> newDataList = SqlLogModel.queryLogs(project, searchText, durationFilter, selectedTimeRange, sqlType, 0, getFirstItemId(), pageSize);
            if (newDataList.isEmpty()) {  // 兼容这些 SQL 已被添加到过滤表的场景
                return 0;
            }

            int newDataListCount = newDataList.size();
            // 不是时间搜索的，才采用补数据的方式
            if (selectedTimeRange == 0) {
                data.addAll(0, newDataList);
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
                        return item.getFormatExecutionTime();

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

            ArrayList<SqlLog> list = SqlLogModel.queryLogs(project, searchText, durationFilter, selectedTimeRange, sqlType, lastItemId, 0, pageSize);
            if (list.size() == 0) {
                return;
            }

            // 时间段搜索的话，不分页
            if (selectedTimeRange > 0) {
                data = list;
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

        public ArrayList<SqlLog> data() {
            return data;
        }

        /**
         * 更新执行时间
         *
         * @param sqlLog
         */
        public void updateExecuteTime(SqlLog sqlLog) {
            int id = sqlLog.getId();
            synchronized (data) {       // 暂时锁定 data
                for (int i = 0; i < data.size(); i++) {
                    SqlLog dataItem = data.get(i);
                    if (dataItem.getId() == id) {
                        // 更新列表中的对象
                        data.set(i, sqlLog);
                        break; // 找到匹配的项后可以退出循环
                    }
                }
            }
        }
    }
}
