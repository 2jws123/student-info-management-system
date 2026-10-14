package com.course.sims.ui.panel;

import com.course.sims.config.UiTheme;
import com.course.sims.entity.Clazz;
import com.course.sims.entity.Major;
import com.course.sims.service.BusinessException;
import com.course.sims.service.TeachingService;
import com.course.sims.ui.common.BasePanel;
import com.course.sims.ui.common.UiUtil;
import com.course.sims.ui.dialog.ClazzDialog;
import com.course.sims.ui.dialog.MajorDialog;
import com.course.sims.util.PageResult;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;

/**
 * 班级与专业管理面板（模块 M3）。
 *
 * <p>主体为班级管理：关键字（班级代码 / 班级名称 / 班主任）检索、按专业与年级筛选、
 * 分页浏览、新增 / 修改 / 删除班级；专业管理则通过按钮条上的「专业管理」弹出
 * {@link MajorManageDialog} 对话框（本类的静态内部类）完成，避免一个面板重复套用两套
 * 表格骨架。</p>
 *
 * <p>业务规则由 {@link TeachingService} 统一保证：班级代码全局唯一、班级下存在学生时
 * 不允许删除、班级容量不得小于现有学生人数。</p>
 *
 * @author 课程设计第 2 小组 - 成员 B/C
 * @version 1.0.0
 */
public class ClassPanel extends BasePanel {

    private static final long serialVersionUID = 1L;

    /** 教学基础数据业务对象 */
    private final TeachingService teachingService = new TeachingService();

    /** 关键字查询框（班级代码 / 班级名称 / 班主任） */
    private final JTextField keywordField = queryField(14);
    /** 专业下拉框（元素为 [专业ID, 专业名称]） */
    private final JComboBox<Object> majorBox = new JComboBox<>();
    /** 年级下拉框 */
    private final JComboBox<Object> gradeBox = queryCombo(new String[]{"2023", "2024", "2025"}, 90);

    /** 当前页班级数据，供修改时按主键定位实体 */
    private final List<Clazz> currentRows = new ArrayList<>();

    /**
     * 构造班级与专业管理面板。
     */
    public ClassPanel() {
        super("班级与专业管理");
        initUi();
    }

    @Override
    protected void configureColumns() {
        tableModel.setColumnIdentifiers(new String[]{
                "ID", "班级代码", "班级名称", "专业", "年级", "班主任",
                "容量", "实际人数", "剩余容量", "创建时间"});
    }

    @Override
    protected void buildQueryBar() {
        loadMajorOptions();
        addQueryItem("关键字", keywordField);
        addQueryItem("专业", majorBox);
        addQueryItem("年级", gradeBox);

        JButton search = UiUtil.button("查询", UiTheme.PRIMARY);
        search.setFont(UiTheme.pickChineseFont(12f));
        search.addActionListener(e -> {
            pageNo = 1;
            refreshData();
        });
        JButton reset = UiUtil.button("重置条件", new Color(0x6B7280));
        reset.setFont(UiTheme.pickChineseFont(12f));
        reset.addActionListener(e -> {
            keywordField.setText("");
            majorBox.setSelectedIndex(0);
            gradeBox.setSelectedIndex(0);
            pageNo = 1;
            refreshData();
        });
        queryBar.add(search);
        queryBar.add(reset);
        keywordField.addActionListener(e -> {
            pageNo = 1;
            refreshData();
        });
    }

    /**
     * 加载专业下拉选项（元素为 [专业ID, 专业名称]，渲染时只显示名称）。
     */
    private void loadMajorOptions() {
        majorBox.removeAllItems();
        majorBox.addItem("全部");
        for (Major major : teachingService.findAllMajors()) {
            majorBox.addItem(new Object[]{major.getId(), major.getMajorName()});
        }
        majorBox.setFont(UiTheme.pickChineseFont(13f));
        majorBox.setPreferredSize(new Dimension(180, 28));
        majorBox.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Object[]) {
                    setText(String.valueOf(((Object[]) value)[1]));
                }
                return this;
            }
        });
    }

    @Override
    protected void buildButtons() {
        addButton("新增班级", UiTheme.PRIMARY, e -> onAdd());
        addButton("修改班级", new Color(0x0EA5E9), e -> onEdit());
        addButton("删除班级", UiTheme.DANGER, e -> onDelete());
        addButton("专业管理", new Color(0x8B5CF6), e -> openMajorManage());
        addButton("刷新", new Color(0x6B7280), e -> refreshData());
    }

    /**
     * 打开专业管理对话框；关闭后同步刷新专业下拉选项与班级列表。
     */
    private void openMajorManage() {
        new MajorManageDialog(SwingUtilities.getWindowAncestor(this)).setVisible(true);
        loadMajorOptions();
        refreshData();
    }

    @Override
    protected void loadData() {
        PageResult<Clazz> page = teachingService.findClasses(keywordField.getText().trim(), comboId(majorBox),
                parseGrade(), pageNo, pageSize);
        currentRows.clear();
        for (Clazz clazz : page.getRows()) {
            currentRows.add(clazz);
            addRow(clazz.getId(), clazz.getClassCode(), clazz.getClassName(), clazz.getMajorName(),
                    clazz.getGrade(), clazz.getHeadteacher(), clazz.getCapacity(), clazz.getStudentCount(),
                    clazz.isFull() ? "已满" : String.valueOf(clazz.getRemainCapacity()),
                    UiUtil.formatDateTime(clazz.getCreateTime()));
        }
        setPage(page);
        hideIdColumn();
    }

    /**
     * 隐藏 ID 列（仅内部使用），并设置各列参考宽度。
     */
    private void hideIdColumn() {
        if (table.getColumnCount() > 0) {
            table.getColumnModel().getColumn(0).setMinWidth(0);
            table.getColumnModel().getColumn(0).setMaxWidth(0);
            table.getColumnModel().getColumn(0).setWidth(0);
        }
        int[] widths = {0, 100, 160, 160, 70, 100, 70, 80, 90, 150};
        for (int i = 1; i < Math.min(widths.length, table.getColumnCount()); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    /**
     * 把年级下拉框的选项转换为过滤参数。
     *
     * @return 年级，选择"全部"返回 null
     */
    private Integer parseGrade() {
        String value = comboValue(gradeBox);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 在当前页数据中按主键查找班级。
     *
     * @param id 班级主键
     * @return 班级对象，未找到返回 null
     */
    private Clazz findClazz(Integer id) {
        for (Clazz clazz : currentRows) {
            if (id.equals(clazz.getId())) {
                return clazz;
            }
        }
        return null;
    }

    @Override
    protected void onAdd() {
        ClazzDialog dialog = new ClazzDialog(SwingUtilities.getWindowAncestor(this), null);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            refreshData();
        }
    }

    @Override
    protected void onEdit() {
        Integer id = getSelectedId();
        if (id == null) {
            UiUtil.warn(this, "请先在列表中选择一个班级");
            return;
        }
        Clazz clazz = findClazz(id);
        if (clazz == null) {
            UiUtil.error(this, "班级不存在或已被删除，请刷新后重试");
            return;
        }
        ClazzDialog dialog = new ClazzDialog(SwingUtilities.getWindowAncestor(this), clazz);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            refreshData();
        }
    }

    @Override
    protected void onDelete() {
        Integer id = getSelectedId();
        if (id == null) {
            UiUtil.warn(this, "请先在列表中选择一个班级");
            return;
        }
        if (!confirm("确认删除该班级？班级下若仍有学生将无法删除。")) {
            return;
        }
        try {
            teachingService.deleteClazz(id);
            UiUtil.success(this, "删除成功");
            refreshData();
        } catch (BusinessException e) {
            UiUtil.warn(this, e.getMessage());
        } catch (RuntimeException e) {
            UiUtil.error(this, "删除失败：" + e.getMessage());
        }
    }

    /**
     * 专业管理对话框（班级面板的静态内部类）。
     *
     * <p>使用独立的 JTable 管理专业列表：支持按关键字检索、新增 / 修改 / 删除专业，
     * 其中新增与修改复用 {@link MajorDialog}，删除由 {@link TeachingService#deleteMajor(Integer)}
     * 保证"专业下仍有班级不允许删除"的业务规则。</p>
     *
     * @author 课程设计第 2 小组 - 成员 B/C
     * @version 1.0.0
     */
    public static class MajorManageDialog extends JDialog {

        private static final long serialVersionUID = 1L;

        /** 教学基础数据业务对象 */
        private final TeachingService teachingService = new TeachingService();

        /** 关键字查询框（专业代码 / 专业名称 / 院系） */
        private final JTextField keywordField = UiUtil.textField(14);
        /** 状态提示标签 */
        private final JLabel statusLabel = new JLabel("就绪");
        /** 专业表格模型（只读） */
        private final DefaultTableModel tableModel = new DefaultTableModel() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        /** 专业表格 */
        private final JTable table = new JTable(tableModel);
        /** 当前列表数据，供修改时按主键定位实体 */
        private final List<Major> rows = new ArrayList<>();

        /**
         * 构造专业管理对话框。
         *
         * @param owner 父窗口
         */
        public MajorManageDialog(Window owner) {
            super(owner, "专业管理", ModalityType.APPLICATION_MODAL);
            setLayout(new BorderLayout());
            getContentPane().setBackground(Color.WHITE);
            setSize(860, 540);
            setMinimumSize(new Dimension(700, 420));
            setLocationRelativeTo(owner);
            buildUi();
            loadData();
        }

        /**
         * 装配界面：标题与查询区、表格、按钮区。
         */
        private void buildUi() {
            JPanel top = new JPanel(new BorderLayout());
            top.setBackground(Color.WHITE);
            top.setBorder(BorderFactory.createEmptyBorder(12, 16, 6, 16));

            JLabel titleLabel = new JLabel("专业管理");
            titleLabel.setFont(UiTheme.boldFont(17f));
            top.add(titleLabel, BorderLayout.WEST);

            JPanel queryBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            queryBar.setOpaque(false);
            queryBar.add(UiUtil.formLabel("关键字"));
            keywordField.setPreferredSize(new Dimension(160, 28));
            keywordField.addActionListener(e -> loadData());
            queryBar.add(keywordField);
            JButton search = UiUtil.button("查询", UiTheme.PRIMARY);
            search.setFont(UiTheme.pickChineseFont(12f));
            search.addActionListener(e -> loadData());
            JButton reset = UiUtil.button("重置条件", new Color(0x6B7280));
            reset.setFont(UiTheme.pickChineseFont(12f));
            reset.addActionListener(e -> {
                keywordField.setText("");
                loadData();
            });
            queryBar.add(search);
            queryBar.add(reset);
            top.add(queryBar, BorderLayout.EAST);
            add(top, BorderLayout.NORTH);

            tableModel.setColumnIdentifiers(new String[]{
                    "ID", "专业代码", "专业名称", "所属院系", "学制", "班级数", "备注"});
            UiUtil.styleTable(table);
            table.setFillsViewportHeight(true);
            table.setRowHeight(28);
            table.getTableHeader().setReorderingAllowed(false);
            table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
            hideIdColumn();

            JScrollPane scrollPane = new JScrollPane(table);
            scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));
            scrollPane.getViewport().setBackground(Color.WHITE);
            add(scrollPane, BorderLayout.CENTER);

            JPanel bottom = new JPanel(new BorderLayout());
            bottom.setBackground(Color.WHITE);
            bottom.setBorder(BorderFactory.createEmptyBorder(8, 16, 12, 16));

            JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            buttonBar.setOpaque(false);
            JButton add = UiUtil.button("新增专业", UiTheme.PRIMARY);
            add.setFont(UiTheme.pickChineseFont(12f));
            add.addActionListener(e -> onAddMajor());
            JButton edit = UiUtil.button("修改专业", new Color(0x0EA5E9));
            edit.setFont(UiTheme.pickChineseFont(12f));
            edit.addActionListener(e -> onEditMajor());
            JButton delete = UiUtil.button("删除专业", UiTheme.DANGER);
            delete.setFont(UiTheme.pickChineseFont(12f));
            delete.addActionListener(e -> onDeleteMajor());
            JButton refresh = UiUtil.button("刷新", new Color(0x6B7280));
            refresh.setFont(UiTheme.pickChineseFont(12f));
            refresh.addActionListener(e -> loadData());
            JButton close = UiUtil.button("关闭", new Color(0x6B7280));
            close.setFont(UiTheme.pickChineseFont(12f));
            close.addActionListener(e -> dispose());
            buttonBar.add(add);
            buttonBar.add(edit);
            buttonBar.add(delete);
            buttonBar.add(refresh);
            buttonBar.add(close);
            bottom.add(buttonBar, BorderLayout.WEST);

            statusLabel.setFont(UiTheme.pickChineseFont(12f));
            statusLabel.setForeground(new Color(0x6B7280));
            bottom.add(statusLabel, BorderLayout.EAST);
            add(bottom, BorderLayout.SOUTH);
        }

        /**
         * 隐藏 ID 列并设置各列参考宽度。
         */
        private void hideIdColumn() {
            if (table.getColumnCount() > 0) {
                table.getColumnModel().getColumn(0).setMinWidth(0);
                table.getColumnModel().getColumn(0).setMaxWidth(0);
                table.getColumnModel().getColumn(0).setWidth(0);
            }
            int[] widths = {0, 100, 180, 190, 70, 80, 240};
            for (int i = 1; i < Math.min(widths.length, table.getColumnCount()); i++) {
                table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
            }
        }

        /**
         * 加载专业列表数据（最多 200 条）。
         */
        private void loadData() {
            try {
                rows.clear();
                tableModel.setRowCount(0);
                PageResult<Major> page = teachingService.findMajors(keywordField.getText().trim(), 1, 200);
                for (Major major : page.getRows()) {
                    rows.add(major);
                    tableModel.addRow(new Object[]{major.getId(), major.getMajorCode(), major.getMajorName(),
                            major.getDeptName(),
                            major.getDuration() == null ? "" : major.getDuration() + " 年",
                            major.getClassCount(), major.getRemark()});
                }
                statusLabel.setText("共 " + page.getTotal() + " 个专业");
            } catch (RuntimeException e) {
                UiUtil.error(this, "加载专业数据失败：" + e.getMessage());
            }
        }

        /**
         * 获取选中行的专业主键。
         *
         * @return 主键，未选中返回 null
         */
        private Integer getSelectedId() {
            int row = table.getSelectedRow();
            if (row < 0) {
                return null;
            }
            Object value = tableModel.getValueAt(table.convertRowIndexToModel(row), 0);
            try {
                return Integer.valueOf(String.valueOf(value));
            } catch (NumberFormatException e) {
                return null;
            }
        }

        /**
         * 在当前列表中按主键查找专业。
         *
         * @param id 专业主键
         * @return 专业对象，未找到返回 null
         */
        private Major findMajor(Integer id) {
            for (Major major : rows) {
                if (id.equals(major.getId())) {
                    return major;
                }
            }
            return null;
        }

        /**
         * 新增专业。
         */
        private void onAddMajor() {
            MajorDialog dialog = new MajorDialog(this, null);
            dialog.setVisible(true);
            if (dialog.isSaved()) {
                loadData();
            }
        }

        /**
         * 修改选中的专业。
         */
        private void onEditMajor() {
            Integer id = getSelectedId();
            if (id == null) {
                UiUtil.warn(this, "请先在列表中选择一个专业");
                return;
            }
            Major major = findMajor(id);
            if (major == null) {
                UiUtil.error(this, "专业不存在或已被删除，请刷新后重试");
                return;
            }
            MajorDialog dialog = new MajorDialog(this, major);
            dialog.setVisible(true);
            if (dialog.isSaved()) {
                loadData();
            }
        }

        /**
         * 删除选中的专业（专业下仍有班级时由业务层拦截）。
         */
        private void onDeleteMajor() {
            Integer id = getSelectedId();
            if (id == null) {
                UiUtil.warn(this, "请先在列表中选择一个专业");
                return;
            }
            if (!UiUtil.confirm(this, "确认删除该专业？专业下若仍有班级将无法删除。")) {
                return;
            }
            try {
                teachingService.deleteMajor(id);
                UiUtil.success(this, "删除成功");
                loadData();
            } catch (BusinessException e) {
                UiUtil.warn(this, e.getMessage());
            } catch (RuntimeException e) {
                UiUtil.error(this, "删除失败：" + e.getMessage());
            }
        }
    }
}
