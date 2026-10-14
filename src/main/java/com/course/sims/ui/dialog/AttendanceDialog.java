package com.course.sims.ui.dialog;

import com.course.sims.entity.Attendance;
import com.course.sims.entity.Student;
import com.course.sims.service.AttendanceService;
import com.course.sims.service.BusinessException;
import com.course.sims.service.StudentService;
import com.course.sims.service.TeachingService;
import com.course.sims.ui.common.BaseDialog;
import com.course.sims.ui.common.UiUtil;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.text.MaskFormatter;
import java.awt.Component;
import java.awt.Window;
import java.sql.Date;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.List;

/**
 * 考勤登记 / 修改对话框（单条记录）。
 *
 * <p>表单包含学生、课程、考勤日期、考勤状态与备注五项；其中学生下拉最多加载 300 条，
 * 覆盖课程设计演示数据规模。业务规则（同一学生 + 同一课程 + 同一天唯一、日期不得晚于今天、
 * 状态取值合法）由 {@link AttendanceService} 统一校验，界面负责把提示信息反馈给用户。</p>
 *
 * @author 课程设计第 2 小组 - 成员 C
 * @version 1.0.0
 */
public class AttendanceDialog extends BaseDialog {

    private static final long serialVersionUID = 1L;

    /** 学生下拉最多加载条数，避免数据量过大导致界面卡顿 */
    private static final int MAX_STUDENT_OPTIONS = 300;

    /** 考勤业务服务 */
    private final AttendanceService attendanceService = new AttendanceService();
    /** 学生业务服务 */
    private final StudentService studentService = new StudentService();
    /** 教学基础数据服务 */
    private final TeachingService teachingService = new TeachingService();
    /** 待修改的考勤记录，为 null 表示新增 */
    private final Attendance editing;

    /** 学生下拉框（元素为 [学生ID, 学号 姓名]） */
    private final JComboBox<Object> studentBox = new JComboBox<>();
    /** 课程下拉框（元素为 [课程ID, 课程名称]） */
    private final JComboBox<Object> courseBox = new JComboBox<>();
    /** 考勤日期输入框 */
    private final JFormattedTextField dateField;
    /** 考勤状态下拉框 */
    private final JComboBox<String> statusBox = new JComboBox<>(Attendance.ALL_STATUS);
    /** 备注输入框 */
    private final JTextField remarkField = UiUtil.textField(20);

    /**
     * 构造考勤登记对话框。
     *
     * @param owner      父窗口
     * @param attendance 待修改的考勤记录，为 null 表示新增
     */
    public AttendanceDialog(Window owner, Attendance attendance) {
        super(owner, attendance == null ? "新增考勤记录" : "修改考勤记录");
        this.editing = attendance;
        JFormattedTextField field;
        try {
            field = new JFormattedTextField(new MaskFormatter("####-##-##"));
            field.setColumns(20);
        } catch (ParseException e) {
            field = new JFormattedTextField();
            field.setColumns(20);
        }
        this.dateField = field;
        initDialog();
    }

    @Override
    protected void buildForm() {
        loadStudentOptions();
        loadCourseOptions();
        addField("学生", studentBox, true);
        addField("课程", courseBox, true);
        addField("考勤日期", dateField, true);
        addField("考勤状态", statusBox, true);
        addField("备注", remarkField);
        addHint("同一学生、同一课程、同一天只能登记一条考勤记录；考勤日期不能晚于今天。");
    }

    /**
     * 加载学生下拉选项（元素为 [学生ID, 学号 姓名]，最多 300 条）。
     */
    private void loadStudentOptions() {
        List<Student> students = studentService.findList(null, null, null);
        int count = 0;
        for (Student student : students) {
            if (count++ >= MAX_STUDENT_OPTIONS) {
                break;
            }
            studentBox.addItem(new Object[]{student.getId(), student.getSno() + " " + student.getName()});
        }
        studentBox.setRenderer(optionRenderer());
    }

    /**
     * 加载课程下拉选项（元素为 [课程ID, 课程名称]）。
     */
    private void loadCourseOptions() {
        for (var course : teachingService.findAllCourses()) {
            courseBox.addItem(new Object[]{course.getId(), course.getCourseName()});
        }
        courseBox.setRenderer(optionRenderer());
    }

    /**
     * 创建"只显示名称"的下拉框渲染器（选项为 [主键, 名称] 形式）。
     *
     * @return 渲染器
     */
    private DefaultListCellRenderer optionRenderer() {
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

    @Override
    protected void loadDataToForm() {
        if (editing == null) {
            dateField.setText(LocalDate.now().toString());
            statusBox.setSelectedItem(Attendance.PRESENT);
            return;
        }
        selectOption(studentBox, editing.getStudentId());
        // 学生不在前 300 条之内时，补一条选项，保证修改时能看到当前学生
        if (editing.getStudentId() != null && !isSelectedId(studentBox, editing.getStudentId())) {
            String label = (editing.getSno() == null ? "" : editing.getSno()) + " "
                    + (editing.getStudentName() == null ? "" : editing.getStudentName());
            studentBox.addItem(new Object[]{editing.getStudentId(), label.trim()});
            studentBox.setSelectedIndex(studentBox.getItemCount() - 1);
        }
        selectOption(courseBox, editing.getCourseId());
        dateField.setText(editing.getAttDate() == null ? "" : editing.getAttDate().toString());
        statusBox.setSelectedItem(editing.getStatus());
        remarkField.setText(editing.getRemark() == null ? "" : editing.getRemark());
    }

    @Override
    protected void onSave() {
        try {
            Date attDate = parseDate(dateField.getText());
            if (attDate == null) {
                UiUtil.warn(this, "请填写正确的考勤日期，格式 yyyy-MM-dd");
                return;
            }
            Integer studentId = selectedId(studentBox);
            Integer courseId = selectedId(courseBox);
            String status = (String) statusBox.getSelectedItem();
            // 组装待保存对象：修改时携带原主键，业务层据此执行 update
            Attendance attendance = new Attendance(studentId, courseId, attDate, status);
            if (editing != null) {
                attendance.setId(editing.getId());
            }
            attendance.setRemark(remarkField.getText().trim());
            attendanceService.save(attendance);
            markSavedAndClose();
        } catch (BusinessException e) {
            UiUtil.warn(this, e.getMessage());
        } catch (RuntimeException e) {
            UiUtil.error(this, "保存失败：" + e.getMessage());
        }
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
     * 按主键选中下拉框中的选项。
     *
     * @param combo 下拉框
     * @param id    主键
     */
    private static void selectOption(JComboBox<Object> combo, Integer id) {
        if (id == null) {
            return;
        }
        for (int i = 0; i < combo.getItemCount(); i++) {
            Object item = combo.getItemAt(i);
            if (item instanceof Object[] && id.equals(((Object[]) item)[0])) {
                combo.setSelectedIndex(i);
                return;
            }
        }
    }

    /**
     * 判断下拉框当前是否选中了指定主键。
     *
     * @param combo 下拉框
     * @param id    主键
     * @return 已选中返回 true
     */
    private static boolean isSelectedId(JComboBox<Object> combo, Integer id) {
        Object item = combo.getSelectedItem();
        return item instanceof Object[] && id.equals(((Object[]) item)[0]);
    }

    /**
     * 读取下拉框选中项对应的主键。
     *
     * @param combo 下拉框
     * @return 主键，未选择返回 null
     */
    private static Integer selectedId(JComboBox<Object> combo) {
        Object item = combo.getSelectedItem();
        if (!(item instanceof Object[])) {
            return null;
        }
        Object id = ((Object[]) item)[0];
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
}
