package com.course.sims.ui;

import com.course.sims.App;
import com.course.sims.config.Session;
import com.course.sims.config.UiTheme;
import com.course.sims.entity.User;
import com.course.sims.service.AuthService;
import com.course.sims.service.BusinessException;
import com.course.sims.ui.common.UiUtil;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;

/**
 * 登录窗口（模块 M1 的界面入口）。
 *
 * <p>界面元素：系统标题、用户名 / 密码输入框、显示密码复选框、登录与重置按钮、
 * 演示账号提示区（课程设计演示便利，正式部署可删除）。</p>
 *
 * @author 课程设计第 2 小组 - 成员 D
 * @version 1.0.0
 */
public class LoginFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private final AuthService authService = new AuthService();
    private final JTextField usernameField = new JTextField(16);
    private final JPasswordField passwordField = new JPasswordField(16);
    private final JCheckBox showPassword = new JCheckBox("显示密码");

    /**
     * 构造登录窗口。
     */
    public LoginFrame() {
        setTitle(App.APP_NAME + " - 登录");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setLayout(new BorderLayout());

        add(buildBanner(), BorderLayout.NORTH);
        add(buildForm(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        pack();
        setSize(new Dimension(Math.max(getWidth(), 460), getHeight() + 20));
        setLocationRelativeTo(null);
        getRootPane().setDefaultButton(findLoginButton());
        usernameField.requestFocusInWindow();
    }

    /**
     * 顶部标题区（深色渐变背景）。
     *
     * @return 面板
     */
    private JPanel buildBanner() {
        JPanel banner = new JPanel() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(java.awt.Graphics g) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                java.awt.GradientPaint paint = new java.awt.GradientPaint(
                        0, 0, new Color(0x1E3A8A), getWidth(), getHeight(), new Color(0x2F6FED));
                g2.setPaint(paint);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        banner.setLayout(new BoxLayout(banner, BoxLayout.Y_AXIS));
        banner.setBorder(BorderFactory.createEmptyBorder(24, 20, 24, 20));

        JLabel title = new JLabel(App.APP_NAME);
        title.setFont(UiTheme.boldFont(24f));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(CENTER_ALIGNMENT);
        JLabel sub = new JLabel("Student Information Management System  v" + App.APP_VERSION);
        sub.setFont(UiTheme.pickChineseFont(12f));
        sub.setForeground(new Color(0xDBEAFE));
        sub.setAlignmentX(CENTER_ALIGNMENT);

        banner.add(title);
        banner.add(Box.createVerticalStrut(6));
        banner.add(sub);
        return banner;
    }

    /**
     * 中部表单区。
     *
     * @return 面板
     */
    private JPanel buildForm() {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(Color.WHITE);
        wrapper.setBorder(BorderFactory.createEmptyBorder(18, 30, 6, 30));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 4, 6, 4);
        c.fill = GridBagConstraints.HORIZONTAL;

        usernameField.setFont(UiTheme.pickChineseFont(14f));
        passwordField.setFont(UiTheme.pickChineseFont(14f));
        usernameField.setPreferredSize(new Dimension(240, 32));
        passwordField.setPreferredSize(new Dimension(240, 32));

        c.gridx = 0;
        c.gridy = 0;
        wrapper.add(UiUtil.formLabel("用户名"), c);
        c.gridx = 1;
        wrapper.add(usernameField, c);

        c.gridx = 0;
        c.gridy = 1;
        wrapper.add(UiUtil.formLabel("密　码"), c);
        c.gridx = 1;
        wrapper.add(passwordField, c);

        showPassword.setOpaque(false);
        showPassword.setFont(UiTheme.pickChineseFont(12f));
        showPassword.addActionListener(e -> {
            if (showPassword.isSelected()) {
                passwordField.setEchoChar((char) 0);
            } else {
                passwordField.setEchoChar('\u2022');
            }
        });
        c.gridx = 1;
        c.gridy = 2;
        wrapper.add(showPassword, c);
        return wrapper;
    }

    /**
     * 底部按钮区与演示账号提示。
     *
     * @return 面板
     */
    private JPanel buildFooter() {
        JPanel footer = new JPanel();
        footer.setBackground(Color.WHITE);
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
        footer.setBorder(BorderFactory.createEmptyBorder(4, 30, 18, 30));

        JPanel buttons = new JPanel();
        buttons.setOpaque(false);
        JButton loginButton = UiUtil.button("登  录", UiTheme.PRIMARY);
        loginButton.setName("loginButton");
        loginButton.setPreferredSize(new Dimension(120, 36));
        JButton resetButton = UiUtil.button("重  置", new Color(0x6B7280));
        resetButton.setPreferredSize(new Dimension(100, 36));
        loginButton.addActionListener(this::doLogin);
        resetButton.addActionListener(e -> {
            usernameField.setText("");
            passwordField.setText("");
            usernameField.requestFocusInWindow();
        });
        buttons.add(loginButton);
        buttons.add(Box.createHorizontalStrut(12));
        buttons.add(resetButton);

        JLabel hint = new JLabel("<html>演示账号：admin / teacher / zs，密码 123456；"
                + "学生账号 t2024001，密码为学号</html>");
        hint.setFont(UiTheme.pickChineseFont(11f));
        hint.setForeground(new Color(0x9CA3AF));
        hint.setHorizontalAlignment(SwingConstants.CENTER);
        hint.setAlignmentX(CENTER_ALIGNMENT);

        buttons.setAlignmentX(CENTER_ALIGNMENT);
        footer.add(buttons);
        footer.add(Box.createVerticalStrut(10));
        footer.add(hint);
        return footer;
    }

    /**
     * 查找登录按钮（用于设置默认按钮）。
     *
     * @return 登录按钮
     */
    private JButton findLoginButton() {
        return findButton(this, "loginButton");
    }

    /**
     * 递归查找指定名称的按钮。
     *
     * @param container 容器
     * @param name      按钮名称
     * @return 按钮，未找到返回 null
     */
    private JButton findButton(java.awt.Container container, String name) {
        for (java.awt.Component component : container.getComponents()) {
            if (component instanceof JButton && name.equals(component.getName())) {
                return (JButton) component;
            }
            if (component instanceof java.awt.Container) {
                JButton found = findButton((java.awt.Container) component, name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /**
     * 执行登录。
     *
     * @param event 事件
     */
    private void doLogin(ActionEvent event) {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());
        try {
            User user = authService.login(username, password);
            dispose();
            MainFrame frame = new MainFrame(user);
            frame.setVisible(true);
            UiUtil.success(frame, "欢迎回来，" + user.getRealName() + "（" + user.getRoleName() + "）");
        } catch (BusinessException e) {
            UiUtil.warn(this, e.getMessage());
            passwordField.setText("");
            passwordField.requestFocusInWindow();
        } catch (RuntimeException e) {
            UiUtil.error(this, "登录失败：" + e.getMessage());
        }
    }

    /**
     * 独立启动登录窗口（便于开发调试）。
     *
     * @param args 参数
     */
    public static void main(String[] args) {
        UiTheme.apply();
        com.course.sims.config.DatabaseInitializer.init();
        javax.swing.SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }

    /**
     * 获取当前会话用户（供测试与截图程序使用）。
     *
     * @return 用户
     */
    public User currentUser() {
        return Session.getCurrentUser();
    }

    /**
     * 设置演示用的默认账号密码（供截图程序使用）。
     *
     * @param username 用户名
     * @param password 密码
     */
    public void presetAccount(String username, String password) {
        usernameField.setText(username);
        passwordField.setText(password);
        Font font = UiTheme.pickChineseFont(14f);
        usernameField.setFont(font);
    }
}
