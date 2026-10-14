package com.course.sims.ui.dialog;

import com.course.sims.config.UiTheme;
import com.course.sims.entity.Course;
import com.course.sims.service.BusinessException;
import com.course.sims.service.TeachingService;
import com.course.sims.ui.common.BaseDialog;
import com.course.sims.ui.common.UiUtil;

import javax.swing.JComboBox;
import javax.swing.JTextField;
import java.awt.Window;

/**
 * 课程信息编辑对话框（新增 / 修改共用）。
 *
 * @author 课程设计第 2 小组 - 成员 C
 * @version 1.0.0
 */
public class CourseDialog extends BaseDialog {

    private static final long serialVersionUID = 1L;

    private final TeachingService teachingService = new TeachingService();
    private final Course editing;

    private final JTextField codeField = UiUtil.textField(16);
    private final JTextField nameField = UiUtil.textField(16);
    private final JTextField creditField = UiUtil.textField(8);
    private final JTextField hoursField = UiUtil.textField(8);
    private final JComboBox<String> typeBox = new JComboBox<>(Course.ALL_TYPES);
    private final JComboBox<String> assessBox = new JComboBox<>(Course.ALL_ASSESS);
    private final JTextField teacherField = UiUtil.textField(16);
    private final JComboBox<String> semesterBox = new JComboBox<>();

    /**
     * 构造课程编辑对话框。
     *
     * @param owner  父窗口
     * @param course 待修改的课程，为 null 表示新增
     */
    public CourseDialog(Window owner, Course course) {
        super(owner, course == null ? "新增课程" : "修改课程信息");
        this.editing = course;
        initDialog();
    }

    @Override
    protected void buildForm() {
        loadSemesterOptions();
        creditField.setText("3.0");
        hoursField.setText("48");
        teacherField.setFont(UiTheme.pickChineseFont(13f));
        semesterBox.setEditable(true);
        semesterBox.setFont(UiTheme.pickChineseFont(13f));
        typeBox.setFont(UiTheme.pickChineseFont(13f));
        assessBox.setFont(UiTheme.pickChineseFont(13f));

        addField("课程代码", codeField, true);
        addField("课程名称", nameField, true);
        addField("学分", creditField, true);
        addField("学时", hoursField, true);
        addField("课程类型", typeBox, true);
        addField("考核方式", assessBox, true);
        addField("授课教师", teacherField);
        addField("开课学期", semesterBox);
        addHint("提示：课程代码需唯一；学分取值 0.5~10，学时取值 8~200；"
                + "学期格式如 2024-2025-1，可直接输入新学期待后续选用。");
    }

    /**
     * 加载已有学期作为下拉选项。
     */
    private void loadSemesterOptions() {
        for (String semester : teachingService.findAllSemesters()) {
            semesterBox.addItem(semester);
        }
        if (semesterBox.getItemCount() == 0) {
            semesterBox.addItem("2024-2025-1");
        }
    }

    @Override
    protected void loadDataToForm() {
        if (editing == null) {
            return;
        }
        codeField.setText(editing.getCourseCode());
        nameField.setText(editing.getCourseName());
        creditField.setText(editing.getCredit() == null ? "" : String.valueOf(editing.getCredit()));
        hoursField.setText(editing.getHours() == null ? "" : String.valueOf(editing.getHours()));
        typeBox.setSelectedItem(editing.getCourseType());
        assessBox.setSelectedItem(editing.getAssessType());
        teacherField.setText(editing.getTeacher());
        semesterBox.setSelectedItem(editing.getSemester());
    }

    @Override
    protected void onSave() {
        try {
            Course course = editing == null ? new Course() : editing;
            course.setCourseCode(codeField.getText().trim());
            course.setCourseName(nameField.getText().trim());
            course.setCredit(parseDouble(creditField.getText(), "学分"));
            course.setHours(parseInt(hoursField.getText(), "学时"));
            course.setCourseType((String) typeBox.getSelectedItem());
            course.setAssessType((String) assessBox.getSelectedItem());
            course.setTeacher(teacherField.getText().trim());
            Object semester = semesterBox.getEditor().getItem();
            course.setSemester(semester == null ? "" : semester.toString().trim());
            teachingService.saveCourse(course);
            markSavedAndClose();
        } catch (BusinessException e) {
            UiUtil.warn(this, e.getMessage());
        } catch (RuntimeException e) {
            UiUtil.error(this, "保存失败：" + e.getMessage());
        }
    }

    /**
     * 解析小数输入。
     *
     * @param text 文本
     * @param name 字段名称
     * @return 数值
     * @throws BusinessException 格式错误
     */
    private Double parseDouble(String text, String name) {
        if (text == null || text.isBlank()) {
            throw new BusinessException("ERR-219", "请填写" + name);
        }
        try {
            return Double.valueOf(text.trim());
        } catch (NumberFormatException e) {
            throw new BusinessException("ERR-219", name + "必须为数字");
        }
    }

    /**
     * 解析整数输入。
     *
     * @param text 文本
     * @param name 字段名称
     * @return 数值
     * @throws BusinessException 格式错误
     */
    private Integer parseInt(String text, String name) {
        if (text == null || text.isBlank()) {
            throw new BusinessException("ERR-220", "请填写" + name);
        }
        try {
            return Integer.valueOf(text.trim());
        } catch (NumberFormatException e) {
            throw new BusinessException("ERR-220", name + "必须为整数");
        }
    }
}
