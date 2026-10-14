package com.course.sims.ui.panel;

import com.course.sims.config.Session;
import com.course.sims.entity.Student;
import com.course.sims.service.BusinessException;
import com.course.sims.service.ReportService;
import com.course.sims.service.StudentService;
import com.course.sims.ui.common.BasePanel;
import com.course.sims.ui.common.UiUtil;
import com.course.sims.ui.dialog.StudentDialog;
import com.course.sims.ui.dialog.TranscriptDialog;
import com.course.sims.util.PageResult;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import java.io.File;

/**
 * 学生信息管理面板（模块 M2）。
 *
 * <p>功能：关键字检索（学号 / 姓名 / 手机号）、按班级与学籍状态筛选、分页浏览、
 * 新增 / 修改 / 删除学生、查看并导出学生成绩单、导出学生名册 Excel。</p>
 *
 * @author 课程设计第 2 小组 - 成员 B
 * @version 1.0.0
 */
public class StudentPanel extends BasePanel {

    private static final long serialVersionUID = 1L;

    private final StudentService studentService = new StudentService();
    private final ReportService reportService = new ReportService();

    private final JTextField keywordField = queryField(14);
    private final JComboBox<Object> classBox = new JComboBox<>();
    private final JComboBox<Object> statusBox = queryCombo(Student.ALL_STATUS, 90);
    private final JComboBox<Object> genderBox = queryCombo(new String[]{"男", "女"}, 80);

    /**
     * 构造学生管理面板。
     */
    public StudentPanel() {
        super("学生信息管理");
        initUi();
    }

    @Override
    protected void configureColumns() {
        tableModel.setColumnIdentifiers(new String[]{
                "ID", "学号", "姓名", "性别", "出生日期", "班级", "专业", "年级",
                "手机号", "邮箱", "家庭住址", "入学年份", "学籍状态"});
    }

    @Override
    protected void buildQueryBar() {
        classBox.setFont(com.course.sims.config.UiTheme.pickChineseFont(13f));
        classBox.setPreferredSize(new java.awt.Dimension(170, 28));
        loadClassOptions();
        addQueryItem("关键字", keywordField);
        addQueryItem("班级", classBox);
        addQueryItem("学籍状态", statusBox);
        addQueryItem("性别", genderBox);
        JButton search = UiUtil.button("查询", com.course.sims.config.UiTheme.PRIMARY);
        search.setFont(com.course.sims.config.UiTheme.pickChineseFont(12f));
        search.addActionListener(e -> {
            pageNo = 1;
            refreshData();
        });
        JButton reset = UiUtil.button("重置条件", new java.awt.Color(0x6B7280));
        reset.setFont(com.course.sims.config.UiTheme.pickChineseFont(12f));
        reset.addActionListener(e -> {
            keywordField.setText("");
            classBox.setSelectedIndex(0);
            statusBox.setSelectedIndex(0);
            genderBox.setSelectedIndex(0);
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
        for (var clazz : new com.course.sims.service.TeachingService().findAllClasses()) {
            classBox.addItem(new Object[]{clazz.getId(), clazz.getClassName()});
        }
        classBox.setRenderer(new javax.swing.DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list, Object value,
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
        addButton("新增", com.course.sims.config.UiTheme.PRIMARY, e -> onAdd());
        addButton("修改", new java.awt.Color(0x0EA5E9), e -> onEdit());
        addButton("删除", com.course.sims.config.UiTheme.DANGER, e -> onDelete());
        addButton("成绩单", new java.awt.Color(0x8B5CF6), e -> showTranscript());
        addButton("导出名册", com.course.sims.config.UiTheme.SUCCESS, e -> exportExcel());
        addButton("刷新", new java.awt.Color(0x6B7280), e -> refreshData());
    }

    @Override
    protected void loadData() {
        PageResult<Student> page = studentService.findByPage(keywordField.getText().trim(), comboId(classBox),
                comboValue(statusBox), comboValue(genderBox), pageNo, pageSize);
        for (Student s : page.getRows()) {
            addRow(s.getId(), s.getSno(), s.getName(), s.getGender(),
                    UiUtil.formatDate(s.getBirthday()), s.getClassName(), s.getMajorName(), s.getGrade(),
                    s.getPhone(), s.getEmail(), s.getAddress(), s.getEnrollYear(), s.getStatus());
        }
        setPage(page);
        hideIdColumn();
    }

    /**
     * 隐藏 ID 列（仅内部使用）。
     */
    private void hideIdColumn() {
        if (table.getColumnCount() > 0) {
            table.getColumnModel().getColumn(0).setMinWidth(0);
            table.getColumnModel().getColumn(0).setMaxWidth(0);
            table.getColumnModel().getColumn(0).setWidth(0);
        }
        int[] widths = {0, 80, 70, 50, 90, 150, 150, 60, 100, 160, 170, 70, 70};
        for (int i = 1; i < Math.min(widths.length, table.getColumnCount()); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    @Override
    protected void onAdd() {
        StudentDialog dialog = new StudentDialog(javax.swing.SwingUtilities.getWindowAncestor(this), null);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            refreshData();
        }
    }

    @Override
    protected void onEdit() {
        Integer id = getSelectedId();
        if (id == null) {
            UiUtil.warn(this, "请先在列表中选择一名学生");
            return;
        }
        Student student = studentService.findById(id);
        if (student == null) {
            UiUtil.error(this, "学生不存在或已被删除");
            return;
        }
        StudentDialog dialog = new StudentDialog(javax.swing.SwingUtilities.getWindowAncestor(this), student);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            refreshData();
        }
    }

    @Override
    protected void onDelete() {
        Integer id = getSelectedId();
        if (id == null) {
            UiUtil.warn(this, "请先在列表中选择一名学生");
            return;
        }
        if (!confirm("确认删除该学生？删除后其成绩与考勤记录将一并删除，且不可恢复。")) {
            return;
        }
        try {
            studentService.delete(id);
            UiUtil.success(this, "删除成功");
            refreshData();
        } catch (BusinessException e) {
            UiUtil.warn(this, e.getMessage());
        } catch (RuntimeException e) {
            UiUtil.error(this, "删除失败：" + e.getMessage());
        }
    }

    /**
     * 查看选中学生的成绩单。
     */
    private void showTranscript() {
        Integer id = getSelectedId();
        if (id == null) {
            UiUtil.warn(this, "请先在列表中选择一名学生");
            return;
        }
        try {
            if (Session.isStudent()) {
                String sno = Session.getRelatedSno();
                Student self = studentService.findBySno(sno);
                if (self != null) {
                    id = self.getId();
                }
            }
            new TranscriptDialog(javax.swing.SwingUtilities.getWindowAncestor(this), id).setVisible(true);
        } catch (BusinessException e) {
            UiUtil.warn(this, e.getMessage());
        }
    }

    /**
     * 导出学生名册。
     */
    private void exportExcel() {
        File file = UiUtil.chooseSaveExcel(this, "学生名册.xlsx");
        if (file == null) {
            return;
        }
        try {
            int count = reportService.exportStudents(file, keywordField.getText().trim(), comboId(classBox),
                    comboValue(statusBox));
            UiUtil.success(this, "导出成功，共 " + count + " 条记录\n文件位置：" + file.getAbsolutePath());
        } catch (RuntimeException e) {
            UiUtil.error(this, "导出失败：" + e.getMessage());
        }
    }
}
