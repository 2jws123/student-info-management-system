package com.course.sims.ui.panel;

import com.course.sims.config.UiTheme;
import com.course.sims.entity.Course;
import com.course.sims.service.BusinessException;
import com.course.sims.service.TeachingService;
import com.course.sims.ui.common.BasePanel;
import com.course.sims.ui.common.UiUtil;
import com.course.sims.ui.dialog.CourseDialog;
import com.course.sims.util.PageResult;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import java.awt.Color;

/**
 * 课程管理面板（模块 M4）。
 *
 * <p>功能：课程信息增删改查、按课程代码 / 名称 / 授课教师关键字检索、
 * 按课程类型与开课学期筛选、分页浏览、课程统计信息展示。</p>
 *
 * @author 课程设计第 2 小组 - 成员 C
 * @version 1.0.0
 */
public class CoursePanel extends BasePanel {

    private static final long serialVersionUID = 1L;

    private final TeachingService teachingService = new TeachingService();

    private final JTextField keywordField = queryField(14);
    private final JComboBox<Object> typeBox = queryCombo(Course.ALL_TYPES, 90);
    private final JComboBox<Object> semesterBox = new JComboBox<>();

    /**
     * 构造课程管理面板。
     */
    public CoursePanel() {
        super("课程管理");
        initUi();
    }

    @Override
    protected void configureColumns() {
        tableModel.setColumnIdentifiers(new String[]{
                "ID", "课程代码", "课程名称", "学分", "学时", "课程类型",
                "考核方式", "授课教师", "开课学期", "创建时间"});
    }

    @Override
    protected void buildQueryBar() {
        loadSemesterOptions();
        semesterBox.setFont(UiTheme.pickChineseFont(13f));
        semesterBox.setPreferredSize(new java.awt.Dimension(140, 28));
        addQueryItem("关键字", keywordField);
        addQueryItem("课程类型", typeBox);
        addQueryItem("开课学期", semesterBox);

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
            typeBox.setSelectedIndex(0);
            semesterBox.setSelectedIndex(0);
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
     * 加载学期下拉选项（首项为"全部"）。
     */
    private void loadSemesterOptions() {
        semesterBox.removeAllItems();
        semesterBox.addItem("全部");
        for (String semester : teachingService.findAllSemesters()) {
            semesterBox.addItem(semester);
        }
    }

    @Override
    protected void buildButtons() {
        addButton("新增", UiTheme.PRIMARY, e -> onAdd());
        addButton("修改", new Color(0x0EA5E9), e -> onEdit());
        addButton("删除", UiTheme.DANGER, e -> onDelete());
        addButton("统计", new Color(0x8B5CF6), e -> showSummary());
        addButton("刷新", new Color(0x6B7280), e -> refreshData());
    }

    @Override
    protected void loadData() {
        PageResult<Course> page = teachingService.findCourses(keywordField.getText().trim(),
                comboValue(typeBox), comboValue(semesterBox), pageNo, pageSize);
        for (Course c : page.getRows()) {
            addRow(c.getId(), c.getCourseCode(), c.getCourseName(), UiUtil.formatScore(c.getCredit()),
                    c.getHours(), c.getCourseType(), c.getAssessType(), c.getTeacher(), c.getSemester(),
                    UiUtil.formatDateTime(c.getCreateTime()));
        }
        setPage(page);
        hideIdColumn();
    }

    /**
     * 隐藏 ID 列并设置各列宽度。
     */
    private void hideIdColumn() {
        if (table.getColumnCount() > 0) {
            table.getColumnModel().getColumn(0).setMinWidth(0);
            table.getColumnModel().getColumn(0).setMaxWidth(0);
            table.getColumnModel().getColumn(0).setWidth(0);
        }
        int[] widths = {0, 90, 170, 60, 60, 80, 80, 90, 110, 150};
        for (int i = 1; i < Math.min(widths.length, table.getColumnCount()); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    @Override
    protected void onAdd() {
        CourseDialog dialog = new CourseDialog(javax.swing.SwingUtilities.getWindowAncestor(this), null);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            loadSemesterOptions();
            refreshData();
        }
    }

    @Override
    protected void onEdit() {
        Integer id = getSelectedId();
        if (id == null) {
            UiUtil.warn(this, "请先在列表中选择一门课程");
            return;
        }
        Course course = findCourse(id);
        if (course == null) {
            UiUtil.error(this, "课程不存在或已被删除");
            return;
        }
        CourseDialog dialog = new CourseDialog(javax.swing.SwingUtilities.getWindowAncestor(this), course);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            refreshData();
        }
    }

    @Override
    protected void onDelete() {
        Integer id = getSelectedId();
        if (id == null) {
            UiUtil.warn(this, "请先在列表中选择一门课程");
            return;
        }
        if (!confirm("确认删除该课程？若课程已存在成绩或考勤记录将无法删除。")) {
            return;
        }
        try {
            teachingService.deleteCourse(id);
            UiUtil.success(this, "删除成功");
            loadSemesterOptions();
            refreshData();
        } catch (BusinessException e) {
            UiUtil.warn(this, e.getMessage());
        } catch (RuntimeException e) {
            UiUtil.error(this, "删除失败：" + e.getMessage());
        }
    }

    /**
     * 在当前页数据中定位课程对象（避免界面层越层访问 DAO）。
     *
     * @param id 课程主键
     * @return 课程对象，当前页找不到时返回 null
     */
    private Course findCourse(Integer id) {
        for (Course c : teachingService.findAllCourses()) {
            if (c.getId().equals(id)) {
                return c;
            }
        }
        return null;
    }

    /**
     * 显示课程统计信息。
     */
    private void showSummary() {
        double[] summary = teachingService.courseSummary();
        StringBuilder sb = new StringBuilder();
        sb.append("课程总数：").append((int) summary[0]).append(" 门\n");
        sb.append("总学分：").append(UiUtil.formatScore(summary[1])).append(" 学分\n");
        sb.append("必修课程：").append((int) summary[2]).append(" 门\n");
        sb.append("选修课程：").append((int) (summary[0] - summary[2])).append(" 门");
        UiUtil.info(this, sb.toString());
    }
}
