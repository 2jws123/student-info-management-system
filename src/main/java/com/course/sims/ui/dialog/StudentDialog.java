package com.course.sims.ui.dialog;

import com.course.sims.entity.Clazz;
import com.course.sims.entity.Major;
import com.course.sims.entity.Student;
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
import java.text.ParseException;
import java.time.LocalDate;
import java.util.List;

/**
 * 学生信息编辑对话框（新增 / 修改共用）。
 *
 * @author 课程设计第 2 小组 - 成员 B
 * @version 1.0.0
 */
public class StudentDialog extends BaseDialog {

    private static final long serialVersionUID = 1L;

    private final StudentService studentService = new StudentService();
    private final TeachingService teachingService = new TeachingService();
    private final Student editing;

    private final JTextField snoField = UiUtil.textField(16);
    private final JTextField nameField = UiUtil.textField(16);
    private final JComboBox<String> genderBox = new JComboBox<>(new String[]{"男", "女"});
    private final JFormattedTextField birthdayField;
    private final JComboBox<Object> classBox = new JComboBox<>();
    private final JTextField phoneField = UiUtil.textField(16);
    private final JTextField emailField = UiUtil.textField(16);
    private final JTextField addressField = UiUtil.textField(16);
    private final JTextField enrollYearField = UiUtil.textField(16);
    private final JComboBox<String> statusBox = new JComboBox<>(Student.ALL_STATUS);

    /**
     * 构造学生编辑对话框。
     *
     * @param owner   父窗口
     * @param student 待修改的学生，为 null 表示新增
     */
    public StudentDialog(Window owner, Student student) {
        super(owner, student == null ? "新增学生" : "修改学生信息");
        this.editing = student;
        JFormattedTextField field;
        try {
            field = new JFormattedTextField(new MaskFormatter("####-##-##"));
            field.setColumns(16);
        } catch (ParseException e) {
            field = new JFormattedTextField();
            field.setColumns(16);
        }
        this.birthdayField = field;
        initDialog();
    }

    @Override
    protected void buildForm() {
        loadClassOptions();

        // 学号与姓名并排显示，节省纵向空间
        javax.swing.JPanel codeRow = new javax.swing.JPanel(new java.awt.BorderLayout(6, 0));
        codeRow.setOpaque(false);
        codeRow.add(snoField, java.awt.BorderLayout.CENTER);
        javax.swing.JButton genButton = UiUtil.button("自动生成", new java.awt.Color(0x6B7280));
        genButton.setFont(com.course.sims.config.UiTheme.pickChineseFont(11f));
        genButton.addActionListener(e -> snoField.setText(
                studentService.generateSno(LocalDate.now().getYear())));
        codeRow.add(genButton, java.awt.BorderLayout.EAST);

        enrollYearField.setText(String.valueOf(LocalDate.now().getYear()));

        addField("学号", codeRow, true);
        addField("姓名", nameField, true);
        addField("性别", genderBox, true);
        addField("出生日期", birthdayField);
        addField("所属班级", classBox, true);
        addField("手机号", phoneField);
        addField("邮箱", emailField);
        addField("家庭住址", addressField);
        addField("入学年份", enrollYearField, true);
        addField("学籍状态", statusBox, true);
        addHint("提示：学号格式为 7 位数字（入学年份 + 3 位流水号）；手机号须为 11 位；"
                + "学生人数不得超过班级容量上限。");
    }

    /**
     * 加载班级下拉选项。
     */
    private void loadClassOptions() {
        List<Clazz> classes = teachingService.findAllClasses();
        List<Major> majors = teachingService.findAllMajors();
        for (Clazz clazz : classes) {
            String majorName = "";
            for (Major major : majors) {
                if (major.getId().equals(clazz.getMajorId())) {
                    majorName = major.getMajorName();
                    break;
                }
            }
            classBox.addItem(new Object[]{clazz.getId(), clazz.getClassName() + "（" + majorName + "）"});
        }
        classBox.setRenderer(new DefaultListCellRenderer() {
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
    protected void loadDataToForm() {
        if (editing == null) {
            genderBox.setSelectedItem("男");
            statusBox.setSelectedItem(Student.STATUS_STUDYING);
            return;
        }
        snoField.setText(editing.getSno());
        nameField.setText(editing.getName());
        genderBox.setSelectedItem(editing.getGender());
        birthdayField.setText(editing.getBirthday() == null ? "" : editing.getBirthday().toString());
        for (int i = 0; i < classBox.getItemCount(); i++) {
            Object item = classBox.getItemAt(i);
            if (item instanceof Object[] && editing.getClassId() != null
                    && editing.getClassId().equals(((Object[]) item)[0])) {
                classBox.setSelectedIndex(i);
                break;
            }
        }
        phoneField.setText(editing.getPhone());
        emailField.setText(editing.getEmail());
        addressField.setText(editing.getAddress());
        enrollYearField.setText(editing.getEnrollYear() == null ? "" : editing.getEnrollYear().toString());
        statusBox.setSelectedItem(editing.getStatus());
    }

    @Override
    protected void onSave() {
        try {
            Student student = editing == null ? new Student() : editing;
            student.setSno(snoField.getText().trim());
            student.setName(nameField.getText().trim());
            student.setGender((String) genderBox.getSelectedItem());
            String birthday = birthdayField.getText() == null ? "" : birthdayField.getText().trim();
            if (birthday.isEmpty() || birthday.contains(" ")) {
                student.setBirthday(null);
            } else {
                student.setBirthday(java.sql.Date.valueOf(birthday));
            }
            Object selectedClass = classBox.getSelectedItem();
            student.setClassId(selectedClass instanceof Object[] ? (Integer) ((Object[]) selectedClass)[0] : null);
            student.setPhone(phoneField.getText().trim());
            student.setEmail(emailField.getText().trim());
            student.setAddress(addressField.getText().trim());
            String year = enrollYearField.getText().trim();
            if (!year.isEmpty()) {
                try {
                    student.setEnrollYear(Integer.valueOf(year));
                } catch (NumberFormatException e) {
                    throw new BusinessException("ERR-112", "入学年份必须为数字，如 2024");
                }
            }
            student.setStatus((String) statusBox.getSelectedItem());
            studentService.save(student);
            markSavedAndClose();
        } catch (BusinessException e) {
            UiUtil.warn(this, e.getMessage());
        } catch (IllegalArgumentException e) {
            UiUtil.warn(this, "出生日期格式不正确，应为 yyyy-MM-dd");
        } catch (RuntimeException e) {
            UiUtil.error(this, "保存失败：" + e.getMessage());
        }
    }
}
