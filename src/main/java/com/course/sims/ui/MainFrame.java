package com.course.sims.ui;

import com.course.sims.App;
import com.course.sims.config.Session;
import com.course.sims.config.UiTheme;
import com.course.sims.entity.User;
import com.course.sims.service.AuthService;
import com.course.sims.ui.common.Refreshable;
import com.course.sims.ui.common.UiUtil;
import com.course.sims.ui.panel.AttendancePanel;
import com.course.sims.ui.panel.ClassPanel;
import com.course.sims.ui.panel.CoursePanel;
import com.course.sims.ui.panel.DashboardPanel;
import com.course.sims.ui.panel.ScoreEntryPanel;
import com.course.sims.ui.panel.ScoreQueryPanel;
import com.course.sims.ui.panel.StatsPanel;
import com.course.sims.ui.panel.StudentPanel;
import com.course.sims.ui.panel.SystemPanel;
import com.course.sims.ui.panel.UserPanel;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 系统主窗体（MDI 单文档界面 + 左侧导航）。
 *
 * <p>布局：顶部标题栏（系统名、当前用户、修改密码、注销）→ 左侧导航菜单
 * → 中部功能面板（CardLayout 切换）→ 底部状态栏（用户、角色、在线时长、时间）。</p>
 *
 * <p>模块权限在此统一配置：{@link #MODULES} 中每项包含模块 Key、名称、面板类与
 * 可见角色，导航栏据此动态装配菜单，从而实现"不同角色看到不同功能"。</p>
 *
 * @author 课程设计第 2 小组 - 成员 D
 * @version 1.0.0
 */
public class MainFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    /**
     * 模块定义：Key、显示名称、面板类、可见角色。
     */
    public static final Object[][] MODULES = {
            {"dashboard", "数据看板", DashboardPanel.class, "ALL"},
            {"student", "学生信息管理", StudentPanel.class, "ADMIN,TEACHER,COUNSELOR"},
            {"clazz", "班级与专业管理", ClassPanel.class, "ADMIN,COUNSELOR"},
            {"course", "课程管理", CoursePanel.class, "ADMIN,TEACHER,COUNSELOR"},
            {"scoreEntry", "成绩录入", ScoreEntryPanel.class, "ADMIN,TEACHER,COUNSELOR"},
            {"scoreQuery", "成绩查询", ScoreQueryPanel.class, "ALL"},
            {"attendance", "考勤管理", AttendancePanel.class, "ADMIN,TEACHER,COUNSELOR,STUDENT"},
            {"stats", "统计分析报表", StatsPanel.class, "ADMIN,TEACHER,COUNSELOR"},
            {"user", "用户与权限", UserPanel.class, "ADMIN"},
            {"system", "系统维护与日志", SystemPanel.class, "ADMIN"}
    };

    private final User currentUser;
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(cardLayout);
    private final Map<String, JPanel> panelCache = new LinkedHashMap<>();
    private final JLabel statusUser = new JLabel();
    private final JLabel statusClock = new JLabel();
    private final SideBar sideBar;

    /**
     * 构造主窗体。
     *
     * @param user 登录用户
     */
    public MainFrame(User user) {
        this.currentUser = user;
        setTitle(App.APP_NAME + " v" + App.APP_VERSION + " - " + user.getRealName()
                + "（" + user.getRoleName() + "）");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLayout(new BorderLayout());
        setMinimumSize(new Dimension(1180, 720));

        sideBar = new SideBar(this);
        add(sideBar, BorderLayout.WEST);
        add(buildHeader(), BorderLayout.NORTH);

        contentPanel.setBackground(UiTheme.PAGE_BG);
        add(contentPanel, BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);

        // 默认打开第一个可见模块
        String firstKey = visibleModuleKeys().isEmpty() ? null : visibleModuleKeys().get(0);
        if (firstKey != null) {
            showModule(firstKey);
            sideBar.select(firstKey);
        }

        setSize(1320, 800);
        setLocationRelativeTo(null);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                doExit();
            }
        });
        startClock();
    }

    /**
     * 构建顶部标题栏。
     *
     * @return 面板
     */
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xE5E7EB)),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)));

        JLabel title = new JLabel("学生信息管理系统");
        title.setFont(UiTheme.boldFont(19f));
        title.setForeground(new Color(0x1F2937));
        header.add(title, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        JLabel userLabel = new JLabel("当前用户：" + currentUser.getRealName()
                + "（" + currentUser.getUsername() + " / " + currentUser.getRoleName() + "）");
        userLabel.setFont(UiTheme.pickChineseFont(13f));
        userLabel.setForeground(new Color(0x374151));

        JButton changePwd = UiUtil.button("修改密码", new Color(0x0EA5E9));
        changePwd.setFont(UiTheme.pickChineseFont(12f));
        changePwd.addActionListener(e -> showChangePasswordDialog());

        JButton logout = UiUtil.button("注销登录", new Color(0x6B7280));
        logout.setFont(UiTheme.pickChineseFont(12f));
        logout.addActionListener(e -> doLogout());

        right.add(userLabel);
        right.add(changePwd);
        right.add(logout);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    /**
     * 构建底部状态栏。
     *
     * @return 面板
     */
    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(0xF9FAFB));
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0xE5E7EB)),
                BorderFactory.createEmptyBorder(4, 14, 4, 14)));
        statusUser.setText("登录用户：" + currentUser.getRealName() + "　|　角色：" + currentUser.getRoleName()
                + "　|　部门/班级：" + (currentUser.getRelatedSno() == null || currentUser.getRelatedSno().isBlank()
                ? "计算机与信息工程学院" : "学号 " + currentUser.getRelatedSno()));
        statusUser.setFont(UiTheme.pickChineseFont(12f));
        statusUser.setForeground(new Color(0x6B7280));
        statusClock.setFont(UiTheme.pickChineseFont(12f));
        statusClock.setForeground(new Color(0x6B7280));
        bar.add(statusUser, BorderLayout.WEST);
        bar.add(statusClock, BorderLayout.EAST);
        return bar;
    }

    /**
     * 启动时钟（每秒刷新状态栏时间与在线时长）。
     */
    private void startClock() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Timer timer = new Timer(1000, e -> statusClock.setText(
                "在线 " + Session.getOnlineSeconds() + " 秒　|　" + format.format(new java.util.Date())));
        timer.start();
    }

    /**
     * 切换显示指定模块面板（首次访问时懒加载）。
     *
     * @param key 模块 Key
     */
    public void showModule(String key) {
        if (key == null) {
            return;
        }
        JPanel panel = panelCache.get(key);
        if (panel == null) {
            panel = createPanel(key);
            if (panel == null) {
                UiUtil.error(this, "模块加载失败：" + key);
                return;
            }
            panelCache.put(key, panel);
            contentPanel.add(panel, key);
        } else if (panel instanceof Refreshable) {
            ((Refreshable) panel).refreshData();
        }
        cardLayout.show(contentPanel, key);
        setTitle(App.APP_NAME + " - " + moduleName(key) + " - " + currentUser.getRealName());
        // 同步左侧导航高亮，保证任何入口（点击菜单、程序切换模块、快捷键）下
        // 「当前显示的面板」与「菜单选中项」始终一致
        sideBar.select(key);
    }

    /**
     * 创建模块面板实例。
     *
     * @param key 模块 Key
     * @return 面板实例，失败返回 null
     */
    private JPanel createPanel(String key) {
        for (Object[] module : MODULES) {
            if (module[0].equals(key)) {
                try {
                    Class<?> clazz = (Class<?>) module[2];
                    return (JPanel) clazz.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    System.err.println("[错误] 创建面板失败 " + key + "：" + e);
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * 获取模块显示名称。
     *
     * @param key 模块 Key
     * @return 名称
     */
    public String moduleName(String key) {
        for (Object[] module : MODULES) {
            if (module[0].equals(key)) {
                return String.valueOf(module[1]);
            }
        }
        return key;
    }

    /**
     * 计算当前角色可见的模块 Key 列表。
     *
     * @return 模块 Key 列表
     */
    public java.util.List<String> visibleModuleKeys() {
        java.util.List<String> keys = new java.util.ArrayList<>();
        for (Object[] module : MODULES) {
            String roles = String.valueOf(module[3]);
            if ("ALL".equals(roles) || roles.contains(currentUser.getRole())) {
                keys.add(String.valueOf(module[0]));
            }
        }
        return keys;
    }

    /**
     * 刷新当前显示的面板数据。
     */
    public void refreshCurrent() {
        for (java.awt.Component component : contentPanel.getComponents()) {
            if (component.isVisible() && component instanceof Refreshable) {
                ((Refreshable) component).refreshData();
            }
        }
    }

    /**
     * 显示修改密码对话框。
     */
    private void showChangePasswordDialog() {
        javax.swing.JPasswordField oldField = new javax.swing.JPasswordField(14);
        javax.swing.JPasswordField newField = new javax.swing.JPasswordField(14);
        javax.swing.JPasswordField confirmField = new javax.swing.JPasswordField(14);
        JPanel panel = new JPanel(new java.awt.GridLayout(3, 2, 8, 8));
        panel.add(UiUtil.formLabel("原密码"));
        panel.add(oldField);
        panel.add(UiUtil.formLabel("新密码"));
        panel.add(newField);
        panel.add(UiUtil.formLabel("确认新密码"));
        panel.add(confirmField);
        int result = JOptionPane.showConfirmDialog(this, panel, "修改密码",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            new AuthService().changePassword(currentUser.getId(), new String(oldField.getPassword()),
                    new String(newField.getPassword()), new String(confirmField.getPassword()));
            UiUtil.success(this, "密码修改成功，下次登录请使用新密码");
        } catch (RuntimeException e) {
            UiUtil.error(this, e.getMessage());
        }
    }

    /**
     * 注销并返回登录窗口。
     */
    private void doLogout() {
        if (!UiUtil.confirm(this, "确认注销当前账号并返回登录界面？")) {
            return;
        }
        new AuthService().logout();
        dispose();
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }

    /**
     * 退出系统。
     */
    private void doExit() {
        if (!UiUtil.confirm(this, "确认退出学生信息管理系统？")) {
            return;
        }
        new AuthService().logout();
        System.exit(0);
    }

    /**
     * 获取当前登录用户。
     *
     * @return 用户
     */
    public User getCurrentUser() {
        return currentUser;
    }
}
