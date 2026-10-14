package com.course.sims.ui.dialog;

import com.course.sims.entity.Course;
import com.course.sims.entity.Score;
import com.course.sims.entity.Student;
import com.course.sims.service.BusinessException;
import com.course.sims.service.ScoreService;
import com.course.sims.service.StudentService;
import com.course.sims.service.TeachingService;
import com.course.sims.ui.common.BaseDialog;
import com.course.sims.ui.common.UiUtil;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.Window;
import java.util.List;

/**
 * 单条成绩录入 / 修改对话框（模块 M5）。
 *
 * <p>提供学生、课程、学期与三项成绩（平时 / 期末 / 补考）的录入界面：
 * 总评成绩 = 平时成绩 × 30% + 期末成绩 × 70%，填写补考成绩时以补考成绩为准。
 * 修改场景下由 {@link ScoreService#save(Score)} 依据主键判定新增或更新。</p>
 *
 * @author 课程设计第 2 小组 - 成员 C
 * @version 1.0.0
 */
public class ScoreDialog extends BaseDialog {

    private static final long serialVersionUID = 1L;

    /** 学生下拉最多展示条数，避免下拉过长影响操作 */
    private static final int STUDENT_LIMIT = 200;

    private final ScoreService scoreService = new ScoreService();
    private final StudentService studentService = new StudentService();
    private final TeachingService teachingService = new TeachingService();

    /** 待修改的成绩，为 null 表示新增 */
    private final Score editing;

    private final JComboBox<Object> studentBox = new JComboBox<>();
    private final JComboBox<Object> courseBox = new JComboBox<>();
    private final JComboBox<String> semesterBox = new JComboBox<>();
    private final JTextField usualField = UiUtil.textField(8);
    private final JTextField examField = UiUtil.textField(8);
    private final JTextField makeupField = UiUtil.textField(8);
    private final JTextField remarkField = UiUtil.textField(20);

    /**
     * 构造成绩编辑对话框。
     *
     * @param owner 父窗口
     * @param score 待修改的成绩，为 null 表示新增
     */
    public ScoreDialog(Window owner, Score score) {
        super(owner, score == null ? "新增成绩" : "修改成绩");
        this.editing = score;
        initDialog();
    }

    @Override
    protected void buildForm() {
        loadStudentOptions();
        loadCourseOptions();
        loadSemesterOptions();

        semesterBox.setEditable(true);
        studentBox.setRenderer(idNameRenderer());
        courseBox.setRenderer(idNameRenderer());

        addField("学生", studentBox, true);
        addField("课程", courseBox, true);
        addField("学期", semesterBox, true);
        addField("平时成绩", usualField);
        addField("期末成绩", examField);
        addField("补考成绩", makeupField);
        addField("备注", remarkField);
        addHint("总评成绩 = 平时成绩 × 30% + 期末成绩 × 70%；填写补考成绩时以补考成绩为准。");
    }

    @Override
    protected void loadDataToForm() {
        if (editing == null) {
            return;
        }
        selectStudent(editing);
        selectCourse(editing);
        String semester = editing.getSemester();
        if (semester != null && !semester.isBlank()) {
            semesterBox.setSelectedItem(semester);
            semesterBox.getEditor().setItem(semester);
        }
        usualField.setText(UiUtil.formatScore(editing.getUsualScore()));
        examField.setText(UiUtil.formatScore(editing.getExamScore()));
        makeupField.setText(UiUtil.formatScore(editing.getMakeupScore()));
        remarkField.setText(editing.getRemark() == null ? "" : editing.getRemark());
    }

    @Override
    protected void onSave() {
        try {
            Integer studentId = selectedId(studentBox);
            if (studentId == null) {
                UiUtil.warn(this, "请选择学生");
                return;
            }
            Integer courseId = selectedId(courseBox);
            if (courseId == null) {
                UiUtil.warn(this, "请选择课程");
                return;
            }
            String semester = selectedSemester();
            if (semester.isEmpty()) {
                UiUtil.warn(this, "请选择或输入学期");
                return;
            }
            String usualText = usualField.getText() == null ? "" : usualField.getText().trim();
            String examText = examField.getText() == null ? "" : examField.getText().trim();
            String makeupText = makeupField.getText() == null ? "" : makeupField.getText().trim();
            if (!isNumeric(usualText) || !isNumeric(examText) || !isNumeric(makeupText)) {
                UiUtil.warn(this, "分数必须为数字");
                return;
            }
            Score score = editing == null ? new Score(studentId, courseId, semester) : editing;
            score.setStudentId(studentId);
            score.setCourseId(courseId);
            score.setSemester(semester);
            score.setUsualScore(toDouble(usualText));
            score.setExamScore(toDouble(examText));
            score.setMakeupScore(toDouble(makeupText));
            score.setRemark(remarkField.getText() == null ? "" : remarkField.getText().trim());
            if (score.getIsRetake() == null) {
                score.setIsRetake(Boolean.FALSE);
            }
            scoreService.save(score);
            markSavedAndClose();
        } catch (BusinessException e) {
            UiUtil.warn(this, e.getMessage());
        } catch (RuntimeException e) {
            UiUtil.error(this, "保存失败：" + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    //  下拉选项加载与回填
    // ------------------------------------------------------------------

    /**
     * 加载学生下拉选项（元素为 [学生ID, 学号 姓名]，最多 200 条）。
     */
    private void loadStudentOptions() {
        studentBox.removeAllItems();
        List<Student> students = studentService.findList(null, null, null);
        int limit = Math.min(students.size(), STUDENT_LIMIT);
        for (int i = 0; i < limit; i++) {
            Student student = students.get(i);
            studentBox.addItem(new Object[]{student.getId(), student.getSno() + " " + student.getName()});
        }
    }

    /**
     * 加载课程下拉选项（元素为 [课程ID, 课程名称]）。
     */
    private void loadCourseOptions() {
        courseBox.removeAllItems();
        for (Course course : teachingService.findAllCourses()) {
            courseBox.addItem(new Object[]{course.getId(), course.getCourseName()});
        }
    }

    /**
     * 加载学期下拉选项。
     */
    private void loadSemesterOptions() {
        semesterBox.removeAllItems();
        for (String semester : teachingService.findAllSemesters()) {
            semesterBox.addItem(semester);
        }
    }

    /**
     * 在学生下拉中选中指定成绩对应的学生。
     *
     * @param score 成绩对象
     */
    private void selectStudent(Score score) {
        if (score.getStudentId() == null) {
            return;
        }
        for (int i = 0; i < studentBox.getItemCount(); i++) {
            Object item = studentBox.getItemAt(i);
            if (item instanceof Object[] && score.getStudentId().equals(((Object[]) item)[0])) {
                studentBox.setSelectedIndex(i);
                return;
            }
        }
        // 学生不在下拉前 200 条时补充一条占位选项，保证修改时不丢失关联学生
        studentBox.addItem(new Object[]{score.getStudentId(),
                text(score.getSno()) + " " + text(score.getStudentName())});
        studentBox.setSelectedIndex(studentBox.getItemCount() - 1);
    }

    /**
     * 在课程下拉中选中指定成绩对应的课程。
     *
     * @param score 成绩对象
     */
    private void selectCourse(Score score) {
        if (score.getCourseId() == null) {
            return;
        }
        for (int i = 0; i < courseBox.getItemCount(); i++) {
            Object item = courseBox.getItemAt(i);
            if (item instanceof Object[] && score.getCourseId().equals(((Object[]) item)[0])) {
                courseBox.setSelectedIndex(i);
                return;
            }
        }
        courseBox.addItem(new Object[]{score.getCourseId(), text(score.getCourseName())});
        courseBox.setSelectedIndex(courseBox.getItemCount() - 1);
    }

    // ------------------------------------------------------------------
    //  通用工具方法
    // ------------------------------------------------------------------

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
     * 读取下拉框选项对应的主键。
     *
     * @param combo 下拉框
     * @return 主键，未选择返回 null
     */
    private static Integer selectedId(JComboBox<?> combo) {
        Object value = combo.getSelectedItem();
        if (!(value instanceof Object[])) {
            return null;
        }
        Object id = ((Object[]) value)[0];
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
     * 读取学期（可编辑下拉时优先取编辑框内容）。
     *
     * @return 学期文本，未填写返回空串
     */
    private String selectedSemester() {
        Object value = semesterBox.isEditable() ? semesterBox.getEditor().getItem()
                : semesterBox.getSelectedItem();
        return value == null ? "" : String.valueOf(value).trim();
    }

    /**
     * 判断文本是否为合法数字（空串视为合法，表示未填写）。
     *
     * @param text 文本
     * @return 合法返回 true
     */
    private static boolean isNumeric(String text) {
        if (text == null || text.isEmpty()) {
            return true;
        }
        try {
            Double.parseDouble(text);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 文本转分数。
     *
     * @param text 文本
     * @return 分数，空串返回 null
     */
    private static Double toDouble(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        return Double.valueOf(text);
    }

    /**
     * null 安全的文本转换。
     *
     * @param value 值
     * @return 文本
     */
    private static String text(String value) {
        return value == null ? "" : value;
    }
}
