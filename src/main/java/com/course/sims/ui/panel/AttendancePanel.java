package com.course.sims.ui.panel;

import com.course.sims.config.Session;
import com.course.sims.config.UiTheme;
import com.course.sims.entity.Attendance;
import com.course.sims.entity.Student;
import com.course.sims.service.AttendanceService;
import com.course.sims.service.BusinessException;
import com.course.sims.service.ReportService;
import com.course.sims.service.StudentService;
import com.course.sims.service.TeachingService;
import com.course.sims.ui.common.BasePanel;
import com.course.sims.ui.common.UiUtil;
import com.course.sims.ui.dialog.AttendanceDialog;
import com.course.sims.util.PageResult;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.MaskFormatter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.io.File;
import java.sql.Date;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 考勤管理面板（模块 M6）。
 *
 * <p>功能：按关键字（学号 / 姓名 / 课程名）、班级、课程、考勤状态与日期区间检索考勤记录，
 * 支持分页浏览、新增 / 修改 / 删除单条考勤、整班批量登记、考勤预警名单查看、
 * 班级出勤率统计以及考勤记录 Excel 导出。</p>
 *
 * <p>角色差异：学生角色登录时本面板只展示本人考勤记录（不分页），
 * 并显示本人出勤率，且不允许维护数据。</p>
 *
 * @author 课程设计第 2 小组 - 成员 C
 * @version 1.0.0
 */
public class AttendancePanel extends BasePanel {

    private static final long serialVersionUID = 1L;

    /** 考勤业务服务 */
    private final AttendanceService attendanceService = new AttendanceService();
    /** 学生业务服务（批量登记取名单使用） */
    private final StudentService studentService = new StudentService();
    /** 报表业务服务（导出使用） */
    private final ReportService reportService = new ReportService();

    /** 当前表格中的考勤记录，与表格行一一对应，供"修改"时回填表单 */
    private final List<Attendance> loadedRows = new ArrayList<>();

    /** 关键字输入框（学号 / 姓名 / 课程名） */
    private final JTextField keywordField = queryField(14);
    /** 班级下拉框 */
    private final JComboBox<Object> classBox = new JComboBox<>();
    /** 课程下拉框 */
    private final JComboBox<Object> courseBox = new JComboBox<>();
    /** 考勤状态下拉框 */
    private final JComboBox<Object> statusBox = queryCombo(Attendance.ALL_STATUS, 90);
    /** 起始日期输入框 */
    private final JFormattedTextField startField = createDateField();
    /** 截止日期输入框 */
    private final JFormattedTextField endField = createDateField();

    /**
     * 构造考勤管理面板。
     */
    public AttendancePanel() {
        super("考勤管理");
        initUi();
    }

    @Override
    protected void configureColumns() {
        tableModel.setColumnIdentifiers(new String[]{
                "ID", "日期", "学号", "姓名", "课程", "考勤状态", "备注"});
    }

    @Override
    protected void buildQueryBar() {
        loadClassOptions();
        loadCourseOptions();
        addQueryItem("关键字", keywordField);
        addQueryItem("班级", classBox);
        addQueryItem("课程", courseBox);
        addQueryItem("考勤状态", statusBox);
        addQueryItem("起始日期", startField);
        addQueryItem("截止日期", endField);

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
            classBox.setSelectedIndex(0);
            courseBox.setSelectedIndex(0);
            statusBox.setSelectedIndex(0);
            startField.setText("");
            endField.setText("");
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
     * 加载班级下拉选项（元素为 [班级ID, 班级名称]）。
     */
    private void loadClassOptions() {
        classBox.removeAllItems();
        classBox.addItem("全部");
        for (var clazz : new TeachingService().findAllClasses()) {
            classBox.addItem(new Object[]{clazz.getId(), clazz.getClassName()});
        }
        classBox.setFont(UiTheme.pickChineseFont(13f));
        classBox.setPreferredSize(new Dimension(160, 28));
        classBox.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Object[]) {
                    setText(String.valueOf(((Object[]) value)[1]));
                }
                return this;
            }
        });
    }

    /**
     * 加载课程下拉选项（元素为 [课程ID, 课程名称]）。
     */
    private void loadCourseOptions() {
        courseBox.removeAllItems();
        courseBox.addItem("全部");
        for (var course : new TeachingService().findAllCourses()) {
            courseBox.addItem(new Object[]{course.getId(), course.getCourseName()});
        }
        courseBox.setFont(UiTheme.pickChineseFont(13f));
        courseBox.setPreferredSize(new Dimension(160, 28));
        courseBox.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean isSelected, boolean cellHasFocus) {
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
        addButton("新增", UiTheme.PRIMARY, e -> onAdd());
        addButton("修改", new Color(0x0EA5E9), e -> onEdit());
        addButton("删除", UiTheme.DANGER, e -> onDelete());
        addButton("批量登记", new Color(0x8B5CF6), e -> showBatchDialog());
        addButton("考勤预警", UiTheme.WARNING, e -> showWarningDialog());
        addButton("出勤统计", new Color(0x14B8A6), e -> showClassRateDialog());
        addButton("导出考勤", UiTheme.SUCCESS, e -> exportExcel());
        addButton("刷新", new Color(0x6B7280), e -> refreshData());
    }

    @Override
    protected void loadData() {
        loadedRows.clear();
        // 学生角色：仅展示本人考勤记录（不分页，也不回填分页条）
        if (Session.isStudent()) {
            loadSelfData();
            return;
        }
        PageResult<Attendance> page = attendanceService.findByPage(keywordField.getText().trim(),
                comboId(classBox), comboId(courseBox), comboValue(statusBox),
                parseDate(startField.getText()), parseDate(endField.getText()), pageNo, pageSize);
        for (Attendance a : page.getRows()) {
            loadedRows.add(a);
            addRow(a.getId(), UiUtil.formatDate(a.getAttDate()), a.getSno(), a.getStudentName(),
                    a.getCourseName(), a.getStatus(), a.getRemark());
        }
        setPage(page);
        hideIdColumn();
    }

    /**
     * 加载当前登录学生本人的考勤记录（不分页）。
     */
    private void loadSelfData() {
        Student self = studentService.findBySno(Session.getRelatedSno());
        if (self == null) {
            statusLabel.setText("未找到与当前账号关联的学生信息");
            hideIdColumn();
            return;
        }
        List<Attendance> list = attendanceService.findByStudent(self.getId());
        for (Attendance a : list) {
            loadedRows.add(a);
            addRow(a.getId(), UiUtil.formatDate(a.getAttDate()), a.getSno(), a.getStudentName(),
                    a.getCourseName(), a.getStatus(), a.getRemark());
        }
        double rate = attendanceService.attendanceRate(self.getId());
        statusLabel.setText("本人考勤记录共 " + list.size() + " 条，出勤率 " + UiUtil.formatScore(rate) + "%");
        hideIdColumn();
    }

    /**
     * 隐藏 ID 列（仅内部使用）并设置各列宽度。
     */
    private void hideIdColumn() {
        if (table.getColumnCount() > 0) {
            table.getColumnModel().getColumn(0).setMinWidth(0);
            table.getColumnModel().getColumn(0).setMaxWidth(0);
            table.getColumnModel().getColumn(0).setWidth(0);
        }
        int[] widths = {0, 90, 90, 80, 170, 90, 220};
        for (int i = 1; i < Math.min(widths.length, table.getColumnCount()); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    @Override
    protected void onAdd() {
        AttendanceDialog dialog = new AttendanceDialog(SwingUtilities.getWindowAncestor(this), null);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            refreshData();
        }
    }

    @Override
    protected void onEdit() {
        Integer id = getSelectedId();
        if (id == null) {
            UiUtil.warn(this, "请先在列表中选择一条考勤记录");
            return;
        }
        Attendance target = null;
        for (Attendance a : loadedRows) {
            if (id.equals(a.getId())) {
                target = a;
                break;
            }
        }
        if (target == null) {
            UiUtil.error(this, "考勤记录不存在或已被删除");
            return;
        }
        AttendanceDialog dialog = new AttendanceDialog(SwingUtilities.getWindowAncestor(this), target);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            refreshData();
        }
    }

    @Override
    protected void onDelete() {
        Integer id = getSelectedId();
        if (id == null) {
            UiUtil.warn(this, "请先在列表中选择一条考勤记录");
            return;
        }
        if (!confirm("确认删除该条考勤记录？删除后不可恢复。")) {
            return;
        }
        try {
            attendanceService.delete(id);
            UiUtil.success(this, "删除成功");
            refreshData();
        } catch (BusinessException e) {
            UiUtil.warn(this, e.getMessage());
        } catch (RuntimeException e) {
            UiUtil.error(this, "删除失败：" + e.getMessage());
        }
    }

    /**
     * 弹出批量登记考勤对话框（整班一次登记）。
     */
    private void showBatchDialog() {
        BatchAttendanceDialog dialog = new BatchAttendanceDialog(SwingUtilities.getWindowAncestor(this));
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            refreshData();
        }
    }

    /**
     * 查看考勤预警名单。
     */
    private void showWarningDialog() {
        try {
            List<Map<String, Object>> list = attendanceService.warningList();
            if (list.isEmpty()) {
                UiUtil.info(this, "暂无考勤预警学生");
                return;
            }
            List<Object[]> rows = new ArrayList<>();
            for (Map<String, Object> row : list) {
                rows.add(new Object[]{str(row.get("sno")), str(row.get("name")), str(row.get("class_name")),
                        str(row.get("absent_count")), str(row.get("abnormal_count")),
                        str(row.get("total_count")), str(row.get("rate"))});
            }
            showTableDialog("考勤预警名单（共 " + rows.size() + " 人）",
                    new String[]{"学号", "姓名", "班级", "缺勤次数", "异常次数", "总次数", "出勤率(%)"}, rows);
        } catch (RuntimeException e) {
            UiUtil.error(this, "查询考勤预警失败：" + e.getMessage());
        }
    }

    /**
     * 查看各班级出勤率统计。
     */
    private void showClassRateDialog() {
        try {
            List<Map<String, Object>> list = attendanceService.classAttendanceRate();
            if (list.isEmpty()) {
                UiUtil.info(this, "暂无班级出勤统计数据");
                return;
            }
            List<Object[]> rows = new ArrayList<>();
            for (Map<String, Object> row : list) {
                rows.add(new Object[]{str(row.get("class_name")), str(row.get("total_count")),
                        str(row.get("present_count")), str(row.get("rate"))});
            }
            showTableDialog("班级出勤率统计（共 " + rows.size() + " 个班级）",
                    new String[]{"班级", "考勤总次数", "出勤次数", "出勤率(%)"}, rows);
        } catch (RuntimeException e) {
            UiUtil.error(this, "查询出勤统计失败：" + e.getMessage());
        }
    }

    /**
     * 以只读表格形式弹出一个统计结果对话框。
     *
     * @param title   对话框标题
     * @param columns 表头
     * @param rows    数据行
     */
    private void showTableDialog(String title, String[] columns, List<Object[]> rows) {
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        for (Object[] row : rows) {
            model.addRow(row);
        }
        JTable dialogTable = new JTable(model);
        UiUtil.styleTable(dialogTable);

        JScrollPane scrollPane = new JScrollPane(dialogTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(0xE5E7EB)));
        scrollPane.getViewport().setBackground(Color.WHITE);

        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(owner, title, java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(Color.WHITE);
        dialog.add(scrollPane, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        bottom.setBackground(Color.WHITE);
        JButton close = UiUtil.button("关闭", new Color(0x6B7280));
        close.addActionListener(e -> dialog.dispose());
        bottom.add(close);
        dialog.add(bottom, BorderLayout.SOUTH);

        dialog.setSize(780, 480);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    /**
     * 导出当前查询条件下的考勤记录。
     */
    private void exportExcel() {
        File file = UiUtil.chooseSaveExcel(this, "考勤记录.xlsx");
        if (file == null) {
            return;
        }
        try {
            int count = reportService.exportAttendance(file, comboId(classBox), comboId(courseBox),
                    parseDate(startField.getText()), parseDate(endField.getText()));
            UiUtil.success(this, "导出成功，共 " + count + " 条记录\n文件位置：" + file.getAbsolutePath());
        } catch (RuntimeException e) {
            UiUtil.error(this, "导出失败：" + e.getMessage());
        }
    }

    /**
     * 创建统一风格的日期输入框（掩码 yyyy-MM-dd）。
     *
     * @return 日期输入框
     */
    private static JFormattedTextField createDateField() {
        JFormattedTextField field;
        try {
            field = new JFormattedTextField(new MaskFormatter("####-##-##"));
        } catch (ParseException e) {
            field = new JFormattedTextField();
        }
        field.setColumns(10);
        field.setFont(UiTheme.pickChineseFont(13f));
        field.setPreferredSize(new Dimension(100, 28));
        return field;
    }

    /**
     * 把 yyyy-MM-dd 文本解析为 SQL 日期。
     *
     * @param text 文本
     * @return 日期，文本为空、含空格或格式非法时返回 null
     */
    private static Date parseDate(String text) {
        if (text == null) {
            return null;
        }
        String value = text.trim();
        if (value.isEmpty() || value.contains(" ")) {
            return null;
        }
        try {
            return Date.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * 读取下拉框中选项对应的主键（选项须为 [主键, 名称] 形式的对象数组）。
     *
     * @param combo 下拉框
     * @return 主键，未选择返回 null
     */
    private static Integer itemId(JComboBox<?> combo) {
        Object selected = combo.getSelectedItem();
        if (!(selected instanceof Object[])) {
            return null;
        }
        Object id = ((Object[]) selected)[0];
        if (id instanceof Integer) {
            return (Integer) id;
        }
        if (id == null) {
            return null;
        }
        try {
            return Integer.valueOf(String.valueOf(id));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Map 取值转字符串，null 转为空串（避免统计表格出现 "null"）。
     *
     * @param value 值
     * @return 字符串
     */
    private static String str(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    /**
     * 批量登记考勤对话框（选择课程 / 班级 / 日期后加载整班名单，逐行登记状态）。
     *
     * <p>为避免新增文件，本对话框实现为考勤面板的私有静态内部类。</p>
     *
     * @author 课程设计第 2 小组 - 成员 C
     * @version 1.0.0
     */
    private static class BatchAttendanceDialog extends JDialog {

        private static final long serialVersionUID = 1L;

        /** 考勤业务服务 */
        private final AttendanceService attendanceService = new AttendanceService();
        /** 学生业务服务 */
        private final StudentService studentService = new StudentService();

        /** 课程下拉框（[课程ID, 课程名称]） */
        private final JComboBox<Object> courseBox = new JComboBox<>();
        /** 班级下拉框（[班级ID, 班级名称]） */
        private final JComboBox<Object> classBox = new JComboBox<>();
        /** 考勤日期输入框 */
        private final JFormattedTextField dateField = createDateField();
        /** 名单表格模型：学号、姓名、考勤状态、备注 */
        private final DefaultTableModel model = new DefaultTableModel(
                new String[]{"学号", "姓名", "考勤状态", "备注"}, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return column >= 2;
            }
        };
        /** 名单表格 */
        private final JTable table = new JTable(model);
        /** 名单对应的学生主键（与表格行一一对应） */
        private final List<Integer> studentIds = new ArrayList<>();
        /** 保存成功标记 */
        private boolean saved;

        /**
         * 构造批量登记对话框。
         *
         * @param owner 父窗口
         */
        BatchAttendanceDialog(Window owner) {
            super(owner, "批量登记考勤", java.awt.Dialog.ModalityType.APPLICATION_MODAL);
            buildUi();
            setSize(780, 520);
            setLocationRelativeTo(owner);
        }

        /**
         * 判断是否保存成功。
         *
         * @return 保存成功返回 true
         */
        public boolean isSaved() {
            return saved;
        }

        /**
         * 装配界面。
         */
        private void buildUi() {
            setLayout(new BorderLayout());
            getContentPane().setBackground(Color.WHITE);

            loadClassOptions();
            loadCourseOptions();
            dateField.setText(java.time.LocalDate.now().toString());

            JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
            top.setBackground(Color.WHITE);
            top.add(UiUtil.formLabel("课程"));
            top.add(courseBox);
            top.add(UiUtil.formLabel("班级"));
            top.add(classBox);
            top.add(UiUtil.formLabel("考勤日期"));
            top.add(dateField);
            JButton loadButton = UiUtil.button("加载学生名单", UiTheme.PRIMARY);
            loadButton.setFont(UiTheme.pickChineseFont(12f));
            loadButton.addActionListener(e -> loadStudents());
            top.add(loadButton);

            UiUtil.styleTable(table);
            table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
            table.getColumnModel().getColumn(2).setCellEditor(
                    new DefaultCellEditor(new JComboBox<>(Attendance.ALL_STATUS)));
            JScrollPane scrollPane = new JScrollPane(table);
            scrollPane.setBorder(BorderFactory.createLineBorder(new Color(0xE5E7EB)));
            scrollPane.getViewport().setBackground(Color.WHITE);

            JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
            bottom.setBackground(Color.WHITE);
            JButton allPresent = UiUtil.button("全部出勤", UiTheme.SUCCESS);
            allPresent.addActionListener(e -> markAllPresent());
            JButton save = UiUtil.button("保存", UiTheme.PRIMARY);
            save.addActionListener(e -> onSave());
            JButton cancel = UiUtil.button("取消", new Color(0x6B7280));
            cancel.addActionListener(e -> dispose());
            bottom.add(allPresent);
            bottom.add(save);
            bottom.add(cancel);

            JLabel hint = new JLabel("提示：先选择课程、班级与考勤日期，再加载学生名单；"
                    + "表格中可直接修改考勤状态与备注，已存在的记录会被覆盖更新。");
            hint.setFont(UiTheme.pickChineseFont(12f));
            hint.setForeground(new Color(0x6B7280));
            hint.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

            add(top, BorderLayout.NORTH);
            add(scrollPane, BorderLayout.CENTER);
            JPanel south = new JPanel(new BorderLayout());
            south.setBackground(Color.WHITE);
            south.add(hint, BorderLayout.WEST);
            south.add(bottom, BorderLayout.EAST);
            add(south, BorderLayout.SOUTH);
        }

        /**
         * 加载班级下拉选项（元素为 [班级ID, 班级名称]）。
         */
        private void loadClassOptions() {
            for (var clazz : new TeachingService().findAllClasses()) {
                classBox.addItem(new Object[]{clazz.getId(), clazz.getClassName()});
            }
            classBox.setFont(UiTheme.pickChineseFont(13f));
            classBox.setPreferredSize(new Dimension(150, 28));
            classBox.setRenderer(new DefaultListCellRenderer() {
                private static final long serialVersionUID = 1L;

                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value,
                                                              int index, boolean isSelected, boolean cellHasFocus) {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (value instanceof Object[]) {
                        setText(String.valueOf(((Object[]) value)[1]));
                    }
                    return this;
                }
            });
        }

        /**
         * 加载课程下拉选项（元素为 [课程ID, 课程名称]）。
         */
        private void loadCourseOptions() {
            for (var course : new TeachingService().findAllCourses()) {
                courseBox.addItem(new Object[]{course.getId(), course.getCourseName()});
            }
            courseBox.setFont(UiTheme.pickChineseFont(13f));
            courseBox.setPreferredSize(new Dimension(150, 28));
            courseBox.setRenderer(new DefaultListCellRenderer() {
                private static final long serialVersionUID = 1L;

                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value,
                                                              int index, boolean isSelected, boolean cellHasFocus) {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (value instanceof Object[]) {
                        setText(String.valueOf(((Object[]) value)[1]));
                    }
                    return this;
                }
            });
        }

        /**
         * 按所选班级加载学生名单（默认全部出勤）。
         */
        private void loadStudents() {
            Integer classId = itemId(classBox);
            if (classId == null) {
                UiUtil.warn(this, "请先选择班级");
                return;
            }
            try {
                List<Student> students = studentService.findByClass(classId);
                model.setRowCount(0);
                studentIds.clear();
                for (Student student : students) {
                    studentIds.add(student.getId());
                    model.addRow(new Object[]{student.getSno(), student.getName(), Attendance.PRESENT, ""});
                }
                if (students.isEmpty()) {
                    UiUtil.info(this, "该班级暂无学生");
                }
            } catch (RuntimeException e) {
                UiUtil.error(this, "加载学生名单失败：" + e.getMessage());
            }
        }

        /**
         * 把名单中所有学生的考勤状态批量设为"出勤"。
         */
        private void markAllPresent() {
            if (model.getRowCount() == 0) {
                UiUtil.warn(this, "请先点击「加载学生名单」");
                return;
            }
            for (int i = 0; i < model.getRowCount(); i++) {
                model.setValueAt(Attendance.PRESENT, i, 2);
            }
        }

        /**
         * 保存批量登记结果。
         */
        private void onSave() {
            Integer courseId = itemId(courseBox);
            if (courseId == null) {
                UiUtil.warn(this, "请选择课程");
                return;
            }
            Date attDate = parseDate(dateField.getText());
            if (attDate == null) {
                UiUtil.warn(this, "请填写正确的考勤日期，格式 yyyy-MM-dd");
                return;
            }
            if (model.getRowCount() == 0) {
                UiUtil.warn(this, "请先点击「加载学生名单」");
                return;
            }
            List<Object[]> rows = new ArrayList<>();
            for (int i = 0; i < model.getRowCount(); i++) {
                Object status = model.getValueAt(i, 2);
                Object remark = model.getValueAt(i, 3);
                rows.add(new Object[]{studentIds.get(i),
                        status == null ? Attendance.PRESENT : String.valueOf(status),
                        remark == null ? "" : String.valueOf(remark)});
            }
            try {
                int count = attendanceService.batchSave(courseId, attDate, rows);
                UiUtil.success(this, "批量登记成功，共 " + count + " 条记录");
                saved = true;
                dispose();
            } catch (BusinessException e) {
                UiUtil.warn(this, e.getMessage());
            } catch (RuntimeException e) {
                UiUtil.error(this, "批量登记失败：" + e.getMessage());
            }
        }
    }
}
