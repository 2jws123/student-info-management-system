package com.course.sims.ui.dialog;

import com.course.sims.entity.Clazz;
import com.course.sims.entity.Major;
import com.course.sims.service.BusinessException;
import com.course.sims.service.TeachingService;
import com.course.sims.ui.common.BaseDialog;
import com.course.sims.ui.common.UiUtil;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.Window;
import java.time.LocalDate;

/**
 * 班级信息编辑对话框（新增 / 修改共用）。
 *
 * <p>字段：班级代码、班级名称、所属专业、年级、班主任、班级容量。保存时交由
 * {@link TeachingService#saveClazz(Clazz)} 完成唯一性与容量校验（容量不得小于现有学生人数），
 * 业务校验失败以警告框提示，系统异常以错误框提示。</p>
 *
 * @author 课程设计第 2 小组 - 成员 B/C
 * @version 1.0.0
 */
public class ClazzDialog extends BaseDialog {

    private static final long serialVersionUID = 1L;

    /** 教学基础数据业务对象 */
    private final TeachingService teachingService = new TeachingService();
    /** 待修改的班级，为 null 表示新增 */
    private final Clazz editing;

    /** 班级代码 */
    private final JTextField codeField = UiUtil.textField(16);
    /** 班级名称 */
    private final JTextField nameField = UiUtil.textField(16);
    /** 所属专业（元素为 [专业ID, 专业名称]） */
    private final JComboBox<Object> majorBox = new JComboBox<>();
    /** 年级（入学年份） */
    private final JComboBox<String> gradeBox = new JComboBox<>(new String[]{"2023", "2024", "2025", "2026"});
    /** 班主任 */
    private final JTextField headteacherField = UiUtil.textField(16);
    /** 班级容量 */
    private final JTextField capacityField = UiUtil.textField(8);

    /**
     * 构造班级编辑对话框。
     *
     * @param owner 父窗口
     * @param clazz 待修改的班级，为 null 表示新增
     */
    public ClazzDialog(Window owner, Clazz clazz) {
        super(owner, clazz == null ? "新增班级" : "修改班级");
        this.editing = clazz;
        initDialog();
    }

    @Override
    protected void buildForm() {
        loadMajorOptions();
        addField("班级代码", codeField, true);
        addField("班级名称", nameField, true);
        addField("所属专业", majorBox, true);
        addField("年级", gradeBox, true);
        addField("班主任", headteacherField);
        addField("班级容量", capacityField, true);
        addHint("提示：班级代码须全局唯一（如 CS2301）；年级为入学年份；"
                + "班级容量必须大于 0 且不得小于班级当前人数。");
    }

    /**
     * 加载专业下拉选项（元素为 [专业ID, 专业名称]，渲染时只显示名称）。
     */
    private void loadMajorOptions() {
        for (Major major : teachingService.findAllMajors()) {
            majorBox.addItem(new Object[]{major.getId(), major.getMajorName()});
        }
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

    /**
     * 选中指定年级；若下拉框中没有该年份则临时补充，避免修改时丢失原有年级。
     *
     * @param grade 年级文本
     */
    private void selectGrade(String grade) {
        if (grade == null || grade.isBlank()) {
            return;
        }
        for (int i = 0; i < gradeBox.getItemCount(); i++) {
            if (grade.equals(gradeBox.getItemAt(i))) {
                gradeBox.setSelectedIndex(i);
                return;
            }
        }
        gradeBox.addItem(grade);
        gradeBox.setSelectedItem(grade);
    }

    @Override
    protected void loadDataToForm() {
        if (editing == null) {
            // 新增：默认当前年份与容量 45
            selectGrade(String.valueOf(LocalDate.now().getYear()));
            capacityField.setText("45");
            return;
        }
        codeField.setText(editing.getClassCode());
        nameField.setText(editing.getClassName());
        for (int i = 0; i < majorBox.getItemCount(); i++) {
            Object item = majorBox.getItemAt(i);
            if (item instanceof Object[] && editing.getMajorId() != null
                    && editing.getMajorId().equals(((Object[]) item)[0])) {
                majorBox.setSelectedIndex(i);
                break;
            }
        }
        selectGrade(editing.getGrade() == null ? "" : String.valueOf(editing.getGrade()));
        headteacherField.setText(editing.getHeadteacher());
        capacityField.setText(editing.getCapacity() == null ? "" : String.valueOf(editing.getCapacity()));
    }

    @Override
    protected void onSave() {
        try {
            Clazz clazz = editing == null ? new Clazz() : editing;
            clazz.setClassCode(codeField.getText().trim());
            clazz.setClassName(nameField.getText().trim());
            Object selectedMajor = majorBox.getSelectedItem();
            clazz.setMajorId(selectedMajor instanceof Object[] ? (Integer) ((Object[]) selectedMajor)[0] : null);
            String grade = (String) gradeBox.getSelectedItem();
            if (grade == null || grade.isBlank()) {
                throw new BusinessException("ERR-211", "请选择年级");
            }
            try {
                clazz.setGrade(Integer.valueOf(grade.trim()));
            } catch (NumberFormatException e) {
                throw new BusinessException("ERR-211", "年级必须为 4 位年份，如 2024");
            }
            clazz.setHeadteacher(headteacherField.getText().trim());
            String capacity = capacityField.getText().trim();
            if (capacity.isEmpty()) {
                throw new BusinessException("ERR-212", "班级容量不能为空");
            }
            try {
                clazz.setCapacity(Integer.valueOf(capacity));
            } catch (NumberFormatException e) {
                throw new BusinessException("ERR-212", "班级容量必须为数字，如 45");
            }
            teachingService.saveClazz(clazz);
            markSavedAndClose();
        } catch (BusinessException e) {
            UiUtil.warn(this, e.getMessage());
        } catch (RuntimeException e) {
            UiUtil.error(this, "保存失败：" + e.getMessage());
        }
    }
}
