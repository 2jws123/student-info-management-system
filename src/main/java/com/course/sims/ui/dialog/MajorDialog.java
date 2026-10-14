package com.course.sims.ui.dialog;

import com.course.sims.entity.Major;
import com.course.sims.service.BusinessException;
import com.course.sims.service.TeachingService;
import com.course.sims.ui.common.BaseDialog;
import com.course.sims.ui.common.UiUtil;

import javax.swing.JComboBox;
import javax.swing.JTextField;
import java.awt.Window;

/**
 * 专业信息编辑对话框（新增 / 修改共用）。
 *
 * <p>字段：专业代码、专业名称、所属院系、学制年数、备注。保存时交由
 * {@link TeachingService#saveMajor(Major)} 完成唯一性校验与持久化，
 * 业务校验失败以警告框提示，系统异常以错误框提示。</p>
 *
 * @author 课程设计第 2 小组 - 成员 B/C
 * @version 1.0.0
 */
public class MajorDialog extends BaseDialog {

    private static final long serialVersionUID = 1L;

    /** 教学基础数据业务对象 */
    private final TeachingService teachingService = new TeachingService();
    /** 待修改的专业，为 null 表示新增 */
    private final Major editing;

    /** 专业代码 */
    private final JTextField codeField = UiUtil.textField(16);
    /** 专业名称 */
    private final JTextField nameField = UiUtil.textField(16);
    /** 所属院系 */
    private final JTextField deptField = UiUtil.textField(16);
    /** 学制年数 */
    private final JComboBox<String> durationBox = new JComboBox<>(new String[]{"2", "3", "4", "5"});
    /** 备注 */
    private final JTextField remarkField = UiUtil.textField(20);

    /**
     * 构造专业编辑对话框。
     *
     * @param owner 父窗口
     * @param major 待修改的专业，为 null 表示新增
     */
    public MajorDialog(Window owner, Major major) {
        super(owner, major == null ? "新增专业" : "修改专业");
        this.editing = major;
        initDialog();
    }

    @Override
    protected void buildForm() {
        addField("专业代码", codeField, true);
        addField("专业名称", nameField, true);
        addField("所属院系", deptField, true);
        addField("学制年数", durationBox, true);
        addField("备注", remarkField);
        addHint("提示：专业代码须全局唯一（如 CS01）；学制年数取值范围 2~6 年（专科 3 年、本科 4~5 年）；"
                + "删除专业前需先删除或调整该专业下的班级。");
    }

    @Override
    protected void loadDataToForm() {
        if (editing == null) {
            // 新增：默认学制 4 年
            durationBox.setSelectedItem("4");
            return;
        }
        codeField.setText(editing.getMajorCode());
        nameField.setText(editing.getMajorName());
        deptField.setText(editing.getDeptName());
        durationBox.setSelectedItem(editing.getDuration() == null ? "4" : String.valueOf(editing.getDuration()));
        remarkField.setText(editing.getRemark());
    }

    @Override
    protected void onSave() {
        try {
            Major major = editing == null ? new Major() : editing;
            major.setMajorCode(codeField.getText().trim());
            major.setMajorName(nameField.getText().trim());
            major.setDeptName(deptField.getText().trim());
            String duration = (String) durationBox.getSelectedItem();
            try {
                major.setDuration(duration == null ? 4 : Integer.valueOf(duration.trim()));
            } catch (NumberFormatException e) {
                throw new BusinessException("ERR-204", "学制年数必须为数字");
            }
            major.setRemark(remarkField.getText().trim());
            teachingService.saveMajor(major);
            markSavedAndClose();
        } catch (BusinessException e) {
            UiUtil.warn(this, e.getMessage());
        } catch (RuntimeException e) {
            UiUtil.error(this, "保存失败：" + e.getMessage());
        }
    }
}
