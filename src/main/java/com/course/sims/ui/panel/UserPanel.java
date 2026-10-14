package com.course.sims.ui.panel;

import com.course.sims.config.UiTheme;
import com.course.sims.entity.User;
import com.course.sims.service.AuthService;
import com.course.sims.service.BusinessException;
import com.course.sims.ui.common.BasePanel;
import com.course.sims.ui.common.UiUtil;
import com.course.sims.ui.dialog.UserDialog;
import com.course.sims.util.PageResult;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 用户与权限管理面板（模块 M1 的界面部分，仅系统管理员可见）。
 *
 * <p>功能：按关键字（用户名 / 真实姓名）、角色、状态组合检索账号，分页浏览，
 * 新增 / 修改 / 删除账号，重置指定账号密码为系统默认密码，
 * 并在按钮区右侧给出各角色的权限说明。</p>
 *
 * <p>说明：本面板严格复用 {@link BasePanel} 的骨架（查询区、表格、分页条、按钮条），
 * 仅实现 {@link #configureColumns()}、{@link #loadData()} 与增删改钩子方法。</p>
 *
 * @author 课程设计第 2 小组 - 成员 D
 * @version 1.0.0
 */
public class UserPanel extends BasePanel {

    private static final long serialVersionUID = 1L;

    /** 下拉框"全部"选项文本（与 {@link BasePanel#comboValue} 的约定保持一致） */
    private static final String ALL = "全部";
    /** 状态选项：启用 */
    private static final String STATUS_ENABLED = "启用";
    /** 状态选项：禁用 */
    private static final String STATUS_DISABLED = "禁用";
    /** 重置密码使用的系统默认密码 */
    private static final String DEFAULT_PASSWORD = "123456";

    /** 用户业务服务 */
    private final AuthService authService = new AuthService();

    /** 关键字输入框（用户名 / 真实姓名） */
    private final JTextField keywordField = queryField(14);

    /**
     * 角色下拉框：元素为 [角色码, 角色名称] 形式的对象数组，首项为字符串"全部"。
     * <p>注意：这里不使用 {@link BasePanel#comboId}，因为该方法内部按 Integer 解析第 0 项，
     * 对 "ADMIN" 这类字符串角色码会返回 null，因此角色码由 {@link #selectedRoleCode()} 自行解析。</p>
     */
    private final JComboBox<Object> roleBox = new JComboBox<>(new Object[]{
            ALL,
            new Object[]{User.ROLE_ADMIN, "系统管理员"},
            new Object[]{User.ROLE_TEACHER, "教师"},
            new Object[]{User.ROLE_COUNSELOR, "辅导员"},
            new Object[]{User.ROLE_STUDENT, "学生"}});

    /** 状态下拉框（全部 / 启用 / 禁用） */
    private final JComboBox<Object> statusBox = queryCombo(new String[]{STATUS_ENABLED, STATUS_DISABLED}, 80);

    /**
     * 当前页数据缓存（主键 -> 用户对象）。
     * <p>{@code AuthService} 未提供按主键查询用户的方法，而"修改"功能需要完整的用户对象，
     * 因此在加载列表时顺手缓存当前页数据，修改时直接取用，避免界面层直接访问 DAO。</p>
     */
    private final Map<Integer, User> rowCache = new LinkedHashMap<>();

    /**
     * 构造用户与权限面板。
     */
    public UserPanel() {
        super("用户与权限");
        initUi();
    }

    @Override
    protected void configureColumns() {
        tableModel.setColumnIdentifiers(new String[]{
                "ID", "用户名", "真实姓名", "角色", "关联学号", "手机号", "状态", "最后登录时间", "创建时间"});
    }

    @Override
    protected void buildQueryBar() {
        keywordField.setPreferredSize(new Dimension(150, 28));

        roleBox.setFont(UiTheme.pickChineseFont(13f));
        roleBox.setPreferredSize(new Dimension(110, 28));
        roleBox.setRenderer(new RoleRenderer());

        addQueryItem("关键字", keywordField);
        addQueryItem("角色", roleBox);
        addQueryItem("状态", statusBox);

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
            roleBox.setSelectedIndex(0);
            statusBox.setSelectedIndex(0);
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

    @Override
    protected void buildButtons() {
        addButton("新增", UiTheme.PRIMARY, e -> onAdd());
        addButton("修改", new Color(0x0EA5E9), e -> onEdit());
        addButton("删除", UiTheme.DANGER, e -> onDelete());
        addButton("重置密码", UiTheme.WARNING, e -> resetPassword());
        addButton("刷新", new Color(0x6B7280), e -> refreshData());
        buttonBar.add(buildRoleHint());
    }

    @Override
    protected void loadData() {
        rowCache.clear();

        // 状态下拉框为字符串选项，这里映射为数据库中的 1 / 0，选择"全部"时不过滤
        String statusText = comboValue(statusBox);
        Integer status = null;
        if (STATUS_ENABLED.equals(statusText)) {
            status = Integer.valueOf(1);
        } else if (STATUS_DISABLED.equals(statusText)) {
            status = Integer.valueOf(0);
        }

        PageResult<User> page = authService.findUsers(keywordField.getText().trim(), selectedRoleCode(),
                status, pageNo, pageSize);
        for (User user : page.getRows()) {
            rowCache.put(user.getId(), user);
            addRow(user.getId(),
                    user.getUsername(),
                    user.getRealName(),
                    user.getRoleName(),
                    user.getRelatedSno(),
                    user.getPhone(),
                    user.isEnabled() ? STATUS_ENABLED : STATUS_DISABLED,
                    UiUtil.formatDateTime(user.getLastLogin()),
                    UiUtil.formatDateTime(user.getCreateTime()));
        }
        setPage(page);
        hideIdColumn();
    }

    /**
     * 隐藏 ID 列并设置各列宽度（第 0 列为主键，仅供内部使用）。
     */
    private void hideIdColumn() {
        if (table.getColumnCount() > 0) {
            table.getColumnModel().getColumn(0).setMinWidth(0);
            table.getColumnModel().getColumn(0).setMaxWidth(0);
            table.getColumnModel().getColumn(0).setWidth(0);
        }
        int[] widths = {0, 110, 100, 90, 110, 120, 70, 160, 160};
        for (int i = 1; i < Math.min(widths.length, table.getColumnCount()); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    /**
     * 读取角色下拉框选中的角色码。
     * <p>元素为 [角色码, 角色名称] 对象数组，选中"全部"（字符串）时返回 null。
     * 角色码是字符串（如 "ADMIN"），不能使用按 Integer 解析的 {@code comboId()}。</p>
     *
     * @return 角色码，未选择角色时返回 null
     */
    private String selectedRoleCode() {
        Object selected = roleBox.getSelectedItem();
        if (selected instanceof Object[]) {
            return String.valueOf(((Object[]) selected)[0]);
        }
        return null;
    }

    /**
     * 构建按钮区右侧的角色权限说明标签（HTML 富文本）。
     *
     * @return 说明标签
     */
    private JLabel buildRoleHint() {
        JLabel hint = new JLabel("<html><body style='color:#6B7280'>权限说明："
                + "<b>系统管理员</b> 拥有全部功能；<b>教师</b> 负责成绩与考勤；"
                + "<b>辅导员</b> 负责学生与班级；<b>学生</b> 仅可查询本人数据。</body></html>");
        hint.setFont(UiTheme.pickChineseFont(12f));
        hint.setBorder(BorderFactory.createEmptyBorder(0, 18, 0, 0));
        return hint;
    }

    @Override
    protected void onAdd() {
        UserDialog dialog = new UserDialog(SwingUtilities.getWindowAncestor(this), null);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            refreshData();
        }
    }

    @Override
    protected void onEdit() {
        Integer id = getSelectedId();
        if (id == null) {
            UiUtil.warn(this, "请先在列表中选择一个用户");
            return;
        }
        User user = rowCache.get(id);
        if (user == null) {
            UiUtil.warn(this, "未找到该用户的详细信息，请点击【刷新】后重试");
            return;
        }
        UserDialog dialog = new UserDialog(SwingUtilities.getWindowAncestor(this), user);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            refreshData();
        }
    }

    @Override
    protected void onDelete() {
        Integer id = getSelectedId();
        if (id == null) {
            UiUtil.warn(this, "请先在列表中选择一个用户");
            return;
        }
        if (!confirm("确认删除该账号？删除后该用户将无法登录系统，且不可恢复。")) {
            return;
        }
        try {
            authService.deleteUser(id);
            UiUtil.success(this, "删除成功");
            refreshData();
        } catch (BusinessException e) {
            // 例如"不能删除当前登录的账号""系统必须保留至少一个管理员账号"
            UiUtil.warn(this, e.getMessage());
        } catch (RuntimeException e) {
            UiUtil.error(this, "删除失败：" + e.getMessage());
        }
    }

    /**
     * 将选中账号的密码重置为系统默认密码（需二次确认）。
     */
    private void resetPassword() {
        Integer id = getSelectedId();
        if (id == null) {
            UiUtil.warn(this, "请先在列表中选择一个用户");
            return;
        }
        if (!confirm("确认将选中账号的密码重置为 " + DEFAULT_PASSWORD + " ？重置后请及时通知本人修改密码。")) {
            return;
        }
        try {
            authService.resetPassword(id, DEFAULT_PASSWORD);
            UiUtil.success(this, "密码已重置为 " + DEFAULT_PASSWORD);
            refreshData();
        } catch (BusinessException e) {
            UiUtil.warn(this, e.getMessage());
        } catch (RuntimeException e) {
            UiUtil.error(this, "重置密码失败：" + e.getMessage());
        }
    }

    /**
     * 角色下拉框渲染器：只显示 [角色码, 角色名称] 中的角色名称，
     * "全部"等普通字符串按原样显示。
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
