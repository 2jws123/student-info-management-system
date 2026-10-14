package com.course.sims.ui.panel;

import com.course.sims.config.Session;
import com.course.sims.config.UiTheme;
import com.course.sims.entity.Clazz;
import com.course.sims.entity.Course;
import com.course.sims.entity.Score;
import com.course.sims.entity.Student;
import com.course.sims.service.BusinessException;
import com.course.sims.service.ReportService;
import com.course.sims.service.ScoreService;
import com.course.sims.service.StudentService;
import com.course.sims.service.TeachingService;
import com.course.sims.ui.common.BasePanel;
import com.course.sims.ui.common.UiUtil;
import com.course.sims.ui.dialog.ScoreDialog;
import com.course.sims.ui.dialog.TranscriptDialog;
import com.course.sims.util.PageResult;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
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
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 成绩查询面板（模块 M5）。
 *
 * <p>功能：按关键字（学号 / 姓名 / 课程名）、班级、课程、学期与及格情况组合检索成绩，
 * 分页浏览查询结果，并支持新增 / 修改 / 删除成绩、查看学生成绩单、
 * 查看不及格名单以及按当前条件导出成绩表。</p>
 *
 * <p>学生角色登录时只显示本人成绩，基类会自动禁用新增 / 修改 / 删除按钮。</p>
 *
 * @author 课程设计第 2 小组 - 成员 C
 * @version 1.0.0
 */
public class ScoreQueryPanel extends BasePanel {

    private static final long serialVersionUID = 1L;

    private final ScoreService scoreService = new ScoreService();
    private final StudentService studentService = new StudentService();
    private final TeachingService teachingService = new TeachingService();
    private final ReportService reportService = new ReportService();

    private final JTextField keywordField = queryField(14);
    private final JComboBox<Object> classBox = new JComboBox<>();
    private final JComboBox<Object> courseBox = new JComboBox<>();
    private final JComboBox<String> semesterBox = new JComboBox<>();
    private final JComboBox<Object> passBox = queryCombo(new String[]{"及格", "不及格"}, 90);

    /** 当前页成绩对象，与表格行一一对应（用于修改与成绩单） */
    private final List<Score> currentRows = new ArrayList<>();

    /**
     * 构造成绩查询面板。
     */
    public ScoreQueryPanel() {
        super("成绩查询");
        initUi();
    }

    @Override
    protected void configureColumns() {
        tableModel.setColumnIdentifiers(new String[]{"ID", "学号", "姓名", "班级", "课程代码", "课程名称",
                "类型", "学分", "学期", "平时", "期末", "补考", "总评", "绩点", "等级"});
    }

    @Override
    protected void buildQueryBar() {
        loadClassOptions();
        loadCourseOptions();
        loadSemesterOptions();

        keywordField.addActionListener(e -> {
            pageNo = 1;
            refreshData();
        });
        addQueryItem("关键字", keywordField);
        addQueryItem("班级", classBox);
        addQueryItem("课程", courseBox);
        addQueryItem("学期", semesterBox);
        addQueryItem("及格情况", passBox);

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
            semesterBox.setSelectedIndex(0);
            passBox.setSelectedIndex(0);
            pageNo = 1;
            refreshData();
        });
        queryBar.add(search);
        queryBar.add(reset);
    }

    @Override
    protected void buildButtons() {
        addButton("新增", UiTheme.PRIMARY, e -> onAdd());
        addButton("修改", new Color(0x0EA5E9), e -> onEdit());
        addButton("删除", UiTheme.DANGER, e -> onDelete());
        addButton("成绩单", new Color(0x8B5CF6), e -> showTranscript());
        addButton("不及格名单", UiTheme.WARNING, e -> showFailedList());
        addButton("导出成绩表", UiTheme.SUCCESS, e -> exportExcel());
        addButton("刷新", new Color(0x6B7280), e -> refreshData());
    }

    @Override
    protected void loadData() {
        String semester = comboValue(semesterBox);
        if (Session.isStudent()) {
            loadSelfData(semester);
            return;
        }
        Boolean pass = null;
        String passValue = comboValue(passBox);
        if ("及格".equals(passValue)) {
            pass = Boolean.TRUE;
        } else if ("不及格".equals(passValue)) {
            pass = Boolean.FALSE;
        }
        PageResult<Score> page = scoreService.findByPage(keywordField.getText().trim(), comboId(classBox),
                comboId(courseBox), semester, pass, pageNo, pageSize);
        fillTable(page.getRows());
        setPage(page);
        hideIdColumn();
    }

    /**
     * 加载当前登录学生本人的成绩（学生角色专用）。
     *
     * @param semester 学期过滤，可为 null
     */
    private void loadSelfData(String semester) {
        Student self = studentService.findBySno(Session.getRelatedSno());
        if (self == null) {
            currentRows.clear();
            setPage(PageResult.empty(1, pageSize));
            statusLabel.setText("未找到与当前账号关联的学生信息");
            hideIdColumn();
            return;
        }
        List<Score> list = scoreService.findByStudent(self.getId(), semester);
        fillTable(list);
        setPage(PageResult.empty(1, pageSize));
        statusLabel.setText("共 " + list.size() + " 条记录（仅显示本人成绩）");
        hideIdColumn();
    }

    /**
     * 把成绩列表填充到表格，并缓存对应的实体对象。
     *
     * @param list 成绩列表
     */
    private void fillTable(List<Score> list) {
        clearTable();
        currentRows.clear();
        currentRows.addAll(list);
        for (Score score : list) {
            addRow(score.getId(), score.getSno(), score.getStudentName(), score.getClassName(),
                    score.getCourseCode(), score.getCourseName(), score.getCourseType(),
                    UiUtil.formatScore(score.getCredit()), score.getSemester(),
                    UiUtil.formatScore(score.getUsualScore()), UiUtil.formatScore(score.getExamScore()),
                    UiUtil.formatScore(score.getMakeupScore()), UiUtil.formatScore(score.getTotalScore()),
                    UiUtil.format2(score.getGradePoint()), score.getScoreLevel());
        }
    }

    /**
     * 隐藏 ID 列并设置列宽。
     */
    private void hideIdColumn() {
        if (table.getColumnCount() > 0) {
            table.getColumnModel().getColumn(0).setMinWidth(0);
            table.getColumnModel().getColumn(0).setMaxWidth(0);
            table.getColumnModel().getColumn(0).setWidth(0);
        }
        int[] widths = {0, 80, 70, 130, 80, 160, 60, 60, 110, 60, 60, 60, 60, 60, 60};
        for (int i = 1; i < Math.min(widths.length, table.getColumnCount()); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    // ------------------------------------------------------------------
    //  增删改与成绩单
    // ------------------------------------------------------------------

    @Override
    protected void onAdd() {
        ScoreDialog dialog = new ScoreDialog(SwingUtilities.getWindowAncestor(this), null);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            refreshData();
        }
    }

    @Override
    protected void onEdit() {
        Score score = selectedScore();
        if (score == null) {
            UiUtil.warn(this, "请先在列表中选择一条成绩记录");
            return;
        }
        ScoreDialog dialog = new ScoreDialog(SwingUtilities.getWindowAncestor(this), score);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            refreshData();
        }
    }

    @Override
    protected void onDelete() {
        Integer id = getSelectedId();
        if (id == null) {
            UiUtil.warn(this, "请先在列表中选择一条成绩记录");
            return;
        }
        if (!confirm("确认删除该条成绩记录？删除后不可恢复。")) {
            return;
        }
        try {
            scoreService.delete(id);
            UiUtil.success(this, "删除成功");
            refreshData();
        } catch (BusinessException e) {
            UiUtil.warn(this, e.getMessage());
        } catch (RuntimeException e) {
            UiUtil.error(this, "删除失败：" + e.getMessage());
        }
    }

    /**
     * 获取选中行对应的成绩对象。
     *
     * @return 成绩对象，未选中返回 null
     */
    private Score selectedScore() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            return null;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        if (modelRow < 0 || modelRow >= currentRows.size()) {
            return null;
        }
        return currentRows.get(modelRow);
    }

    /**
     * 查看选中成绩所属学生的成绩单。
     */
    private void showTranscript() {
        Integer studentId = null;
        Score score = selectedScore();
        if (score != null) {
            studentId = score.getStudentId();
        }
        try {
            if (Session.isStudent()) {
                Student self = studentService.findBySno(Session.getRelatedSno());
                if (self != null) {
                    studentId = self.getId();
                }
            }
            if (studentId == null) {
                UiUtil.warn(this, "请先在列表中选择一条成绩记录");
                return;
            }
            new TranscriptDialog(SwingUtilities.getWindowAncestor(this), studentId).setVisible(true);
        } catch (BusinessException e) {
            UiUtil.warn(this, e.getMessage());
        } catch (RuntimeException e) {
            UiUtil.error(this, "打开成绩单失败：" + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    //  不及格名单与导出
    // ------------------------------------------------------------------

    /**
     * 弹出不及格名单窗口（按当前所选课程过滤）。
     */
    private void showFailedList() {
        Integer courseId = comboId(courseBox);
        List<Map<String, Object>> list = scoreService.failedList(courseId);
        DefaultTableModel model = new DefaultTableModel() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        model.setColumnIdentifiers(new String[]{"学号", "姓名", "班级", "课程", "总评"});
        for (Map<String, Object> item : list) {
            model.addRow(new Object[]{item.get("sno"), item.get("name"), item.get("class_name"),
                    item.get("course_name"), statText(item.get("total_score"))});
        }
        JTable failedTable = new JTable(model);
        UiUtil.styleTable(failedTable);
        failedTable.setFillsViewportHeight(true);
        JScrollPane scroll = new JScrollPane(failedTable);
        scroll.setPreferredSize(new Dimension(640, 340));

        java.awt.Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(owner, "不及格名单（共 " + list.size() + " 人）",
                Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(Color.WHITE);
        dialog.add(scroll, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        bottom.setOpaque(false);
        JButton close = UiUtil.button("关闭", new Color(0x6B7280));
        close.addActionListener(e -> dialog.dispose());
        bottom.add(close);
        dialog.add(bottom, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    /**
     * 按当前查询条件导出成绩表。
     */
    private void exportExcel() {
        File file = UiUtil.chooseSaveExcel(this, "成绩表.xlsx");
        if (file == null) {
            return;
        }
        try {
            int count = reportService.exportScores(file, keywordField.getText().trim(), comboId(classBox),
                    comboId(courseBox), comboValue(semesterBox));
            UiUtil.success(this, "导出成功，共 " + count + " 条记录\n文件位置：" + file.getAbsolutePath());
        } catch (RuntimeException e) {
            UiUtil.error(this, "导出失败：" + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    //  下拉选项加载
    // ------------------------------------------------------------------

    /**
     * 加载班级下拉选项（元素为 [班级ID, 班级名称]）。
     */
    private void loadClassOptions() {
        classBox.removeAllItems();
        classBox.addItem("全部");
        for (Clazz clazz : teachingService.findAllClasses()) {
            classBox.addItem(new Object[]{clazz.getId(), clazz.getClassName()});
        }
        classBox.setFont(UiTheme.pickChineseFont(13f));
        classBox.setPreferredSize(new Dimension(170, 28));
        classBox.setRenderer(idNameRenderer());
    }

    /**
     * 加载课程下拉选项（元素为 [课程ID, 课程名称]）。
     */
    private void loadCourseOptions() {
        courseBox.removeAllItems();
        courseBox.addItem("全部");
        for (Course course : teachingService.findAllCourses()) {
            courseBox.addItem(new Object[]{course.getId(), course.getCourseName()});
        }
        courseBox.setFont(UiTheme.pickChineseFont(13f));
        courseBox.setPreferredSize(new Dimension(180, 28));
        courseBox.setRenderer(idNameRenderer());
    }

    /**
     * 加载学期下拉选项。
     */
    private void loadSemesterOptions() {
        semesterBox.removeAllItems();
        semesterBox.addItem("全部");
        for (String semester : teachingService.findAllSemesters()) {
            semesterBox.addItem(semester);
        }
        semesterBox.setFont(UiTheme.pickChineseFont(13f));
        semesterBox.setPreferredSize(new Dimension(120, 28));
    }

    /**
     * 创建"只显示名称"的下拉渲染器（选项元素为 [主键, 名称]）。
     *
     * @return 渲染器
     */
    private static DefaultListCellRenderer idNameRenderer() {
        return new DefaultListCellRenderer() {
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
        };
    }

    /**
     * 统计值格式化（null 显示为 "-"）。
     *
     * @param value 统计值
     * @return 文本
     */
    private static String statText(Object value) {
        if (value == null) {
            return "-";
        }
        if (value instanceof Number) {
            return UiUtil.formatScore((Number) value);
        }
        String text = String.valueOf(value);
        return text.isBlank() ? "-" : text;
    }
}
