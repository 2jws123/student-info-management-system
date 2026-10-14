package com.course.sims.ui.dialog;

import com.course.sims.config.UiTheme;
import com.course.sims.entity.User;
import com.course.sims.service.AuthService;
import com.course.sims.service.BusinessException;
import com.course.sims.ui.common.BaseDialog;
import com.course.sims.ui.common.UiUtil;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.Window;

/**
 * 用户编辑对话框（新增 / 修改共用）。
 *
 * <p>新增时要求填写初始密码（6~20 位），用户名与密码都可录入；
 * 修改时用户名不可更改，密码输入框不显示（改密码请使用用户列表上的【重置密码】功能），
 * 调用 {@link AuthService#saveUser(User, String)} 时密码参数传 null。</p>
 *
 * @author 课程设计第 2 小组 - 成员 D
 * @version 1.0.0
 */
public class UserDialog extends BaseDialog {

    private static final long serialVersionUID = 1L;

    /** 状态选项：启用 */
    private static final String STATUS_ENABLED = "启用";
    /** 状态选项：禁用 */
    private static final String STATUS_DISABLED = "禁用";

    /** 用户业务服务 */
    private final AuthService authService = new AuthService();

    /** 待修改的用户，为 null 表示新增 */
    private final User editing;

    /** 用户名（修改时禁用） */
    private final JTextField usernameField = UiUtil.textField(16);

    /** 真实姓名 */
    private final JTextField realNameField = UiUtil.textField(16);

    /**
     * 角色下拉框：元素为 [角色码, 角色名称] 对象数组。
     * <p>角色码为字符串（ADMIN / TEACHER / COUNSELOR / STUDENT），
     * 由 {@link #selectedRoleCode()} 读取，不能使用按 Integer 解析的 {@code comboId()}。</p>
     */
    private final JComboBox<Object> roleBox = new JComboBox<>(new Object[]{
            new Object[]{User.ROLE_ADMIN, "系统管理员"},
            new Object[]{User.ROLE_TEACHER, "教师"},
            new Object[]{User.ROLE_COUNSELOR, "辅导员"},
            new Object[]{User.ROLE_STUDENT, "学生"}});

    /** 关联学号（仅学生角色需要填写） */
    private final JTextField relatedSnoField = UiUtil.textField(16);

    /** 手机号 */
    private final JTextField phoneField = UiUtil.textField(16);

    /** 状态（启用 / 禁用） */
    private final JComboBox<String> statusBox = new JComboBox<>(new String[]{STATUS_ENABLED, STATUS_DISABLED});

    /** 初始密码（仅新增时使用） */
    private final JPasswordField passwordField = new JPasswordField(16);

    /**
     * 构造用户编辑对话框。
     *
     * @param owner 父窗口
     * @param user  待修改的用户，为 null 表示新增
     */
    public UserDialog(Window owner, User user) {
        super(owner, user == null ? "新增用户" : "修改用户");
        this.editing = user;
        initDialog();
    }

    @Override
    protected void buildForm() {
        passwordField.setFont(UiTheme.pickChineseFont(13f));
        passwordField.setPreferredSize(new java.awt.Dimension(16 * 9, 28));

        roleBox.setFont(UiTheme.pickChineseFont(13f));
        roleBox.setRenderer(new RoleRenderer());

        usernameField.setToolTipText("登录账号，修改用户时不可更改");
        relatedSnoField.setToolTipText("仅学生角色需要填写");
        passwordField.setToolTipText("6~20 位");

        addField("用户名", usernameField, true);
        addField("真实姓名", realNameField, true);
        addField("角色", roleBox, true);
        addField("关联学号", relatedSnoField);
        addField("手机号", phoneField);
        addField("状态", statusBox, true);
        if (editing == null) {
            // 修改用户时不显示密码输入框：改密码请使用列表上的【重置密码】按钮
            addField("初始密码", passwordField, true);
        }
        addHint("新增用户必须设置初始密码（6~20 位）；修改用户如需改密码请使用列表上的【重置密码】按钮。");
    }

    @Override
    protected void loadDataToForm() {
        if (editing == null) {
            statusBox.setSelectedItem(STATUS_ENABLED);
            roleBox.setSelectedIndex(0);
            return;
        }
        usernameField.setText(editing.getUsername());
        // 用户名是登录凭据，修改时不允许更改
        usernameField.setEnabled(false);
        realNameField.setText(editing.getRealName());
        selectRole(editing.getRole());
        relatedSnoField.setText(editing.getRelatedSno());
        phoneField.setText(editing.getPhone());
        statusBox.setSelectedItem(editing.isEnabled() ? STATUS_ENABLED : STATUS_DISABLED);
    }

    /**
     * 按角色码选中下拉项；若角色码不在预设选项中（历史数据），则动态补充一项。
     *
     * @param roleCode 角色码
     */
    private void selectRole(String roleCode) {
        for (int i = 0; i < roleBox.getItemCount(); i++) {
            Object item = roleBox.getItemAt(i);
            if (item instanceof Object[] && String.valueOf(((Object[]) item)[0]).equals(roleCode)) {
                roleBox.setSelectedIndex(i);
                return;
            }
        }
        if (roleCode != null && !roleCode.isBlank()) {
            roleBox.addItem(new Object[]{roleCode, roleCode});
            roleBox.setSelectedIndex(roleBox.getItemCount() - 1);
        }
    }

    /**
     * 读取角色下拉框选中的角色码。
     *
     * @return 角色码
     */
    private String selectedRoleCode() {
        Object selected = roleBox.getSelectedItem();
        if (selected instanceof Object[]) {
            return String.valueOf(((Object[]) selected)[0]);
        }
        return null;
    }

    @Override
    protected void onSave() {
        try {
            User user = editing == null ? new User() : editing;
            user.setUsername(usernameField.getText().trim());
            user.setRealName(realNameField.getText().trim());
            user.setRole(selectedRoleCode());
            user.setRelatedSno(relatedSnoField.getText().trim());
            user.setPhone(phoneField.getText().trim());
            user.setStatus(STATUS_DISABLED.equals(statusBox.getSelectedItem()) ? 0 : 1);

            // 新增时提交明文初始密码；修改时传 null（AuthService 在 id 不为空时忽略密码参数）
            String rawPassword = editing == null ? new String(passwordField.getPassword()) : null;
            authService.saveUser(user, rawPassword);
            markSavedAndClose();
        } catch (BusinessException e) {
            UiUtil.warn(this, e.getMessage());
        } catch (RuntimeException e) {
            UiUtil.error(this, "保存失败：" + e.getMessage());
        }
    }

    /**
     * 角色下拉框渲染器：只显示 [角色码, 角色名称] 中的角色名称。
     */
    private static class RoleRenderer extends DefaultListCellRenderer {

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
    }
}
