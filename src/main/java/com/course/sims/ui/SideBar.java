package com.course.sims.ui;

import com.course.sims.config.UiTheme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 左侧导航菜单组件。
 *
 * <p>根据当前登录角色动态装配菜单项：只有 {@link MainFrame#MODULES} 中声明对该角色
 * 可见的模块才会出现在导航栏中，实现界面级权限控制。</p>
 *
 * @author 课程设计第 2 小组 - 成员 D
 * @version 1.0.0
 */
public class SideBar extends JPanel {

    private static final long serialVersionUID = 1L;

    private final MainFrame mainFrame;
    private final Map<String, JLabel> menuItems = new LinkedHashMap<>();
    private String activeKey;

    /**
     * 构造导航栏。
     *
     * @param mainFrame 主窗体
     */
    public SideBar(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(UiTheme.SIDEBAR_BG);
        setPreferredSize(new Dimension(198, 0));
        setBorder(BorderFactory.createEmptyBorder(16, 0, 16, 0));

        JLabel logo = new JLabel("  SIMS 系统导航");
        logo.setFont(UiTheme.boldFont(15f));
        logo.setForeground(Color.WHITE);
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);
        logo.setBorder(BorderFactory.createEmptyBorder(0, 14, 12, 14));
        add(logo);

        for (String key : mainFrame.visibleModuleKeys()) {
            addMenuItem(key, mainFrame.moduleName(key));
        }
        add(Box.createVerticalGlue());

        JLabel version = new JLabel("  v" + com.course.sims.App.APP_VERSION + "  |  课程设计");
        version.setFont(UiTheme.pickChineseFont(11f));
        version.setForeground(new Color(0x6B7280));
        version.setAlignmentX(Component.LEFT_ALIGNMENT);
        version.setBorder(BorderFactory.createEmptyBorder(10, 14, 0, 14));
        add(version);
    }

    /**
     * 添加一个菜单项。
     *
     * @param key  模块 Key
     * @param name 显示名称
     */
    private void addMenuItem(String key, String name) {
        JLabel item = new JLabel("   " + name);
        item.setFont(UiTheme.pickChineseFont(14f));
        item.setForeground(UiTheme.SIDEBAR_FG);
        item.setOpaque(true);
        item.setBackground(UiTheme.SIDEBAR_BG);
        item.setAlignmentX(Component.LEFT_ALIGNMENT);
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        item.setPreferredSize(new Dimension(198, 40));
        item.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        item.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                select(key);
                mainFrame.showModule(key);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (!key.equals(activeKey)) {
                    item.setBackground(new Color(0x374151));
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!key.equals(activeKey)) {
                    item.setBackground(UiTheme.SIDEBAR_BG);
                }
            }
        });
        menuItems.put(key, item);
        add(item);
    }

    /**
     * 选中指定菜单项。
     *
     * @param key 模块 Key
     */
    public void select(String key) {
        for (Map.Entry<String, JLabel> entry : menuItems.entrySet()) {
            boolean active = entry.getKey().equals(key);
            entry.getValue().setBackground(active ? UiTheme.PRIMARY : UiTheme.SIDEBAR_BG);
            entry.getValue().setForeground(active ? UiTheme.SIDEBAR_ACTIVE_FG : UiTheme.SIDEBAR_FG);
            entry.getValue().setFont(active ? UiTheme.boldFont(14f) : UiTheme.pickChineseFont(14f));
        }
        this.activeKey = key;
    }

    /**
     * 获取当前选中的模块 Key。
     *
     * @return 模块 Key
     */
    public String getActiveKey() {
        return activeKey;
    }

    /**
     * 获取全部菜单项（供自动化截图程序使用）。
     *
     * @return 菜单项映射
     */
    public Map<String, JLabel> getMenuItems() {
        return menuItems;
    }
}
