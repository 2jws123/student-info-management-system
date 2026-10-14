package com.course.sims.config;

import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Font;

/**
 * 界面主题与全局样式配置（模块 M1 的界面支撑）。
 *
 * <p>统一设置 FlatLaf 外观、中文字体、主色调与圆角风格，
 * 避免各面板各自设置导致界面风格不一致。</p>
 *
 * @author 课程设计第 2 小组 - 成员 D
 * @version 1.0.0
 */
public final class UiTheme {

    /** 主色调（深蓝） */
    public static final Color PRIMARY = new Color(0x2F6FED);
    /** 主色调浅色（选中态） */
    public static final Color PRIMARY_LIGHT = new Color(0xE8F0FE);
    /** 侧边栏背景色 */
    public static final Color SIDEBAR_BG = new Color(0x1F2937);
    /** 侧边栏菜单文字色 */
    public static final Color SIDEBAR_FG = new Color(0xD1D5DB);
    /** 侧边栏选中菜单文字色 */
    public static final Color SIDEBAR_ACTIVE_FG = Color.WHITE;
    /** 页面背景色 */
    public static final Color PAGE_BG = new Color(0xF5F6F8);
    /** 成功色 */
    public static final Color SUCCESS = new Color(0x16A34A);
    /** 警告色 */
    public static final Color WARNING = new Color(0xF59E0B);
    /** 危险色 */
    public static final Color DANGER = new Color(0xDC2626);

    private UiTheme() {
    }

    /**
     * 应用全局主题与中文字体。
     */
    public static void apply() {
        FlatLightLaf.setup();
        UIManager.put("Component.arc", 8);
        UIManager.put("Button.arc", 8);
        UIManager.put("TextComponent.arc", 8);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Component.innerFocusWidth", 1);
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.showVerticalLines", true);
        UIManager.put("Table.rowHeight", 28);
        UIManager.put("TableHeader.height", 30);
        UIManager.put("ScrollBar.thumbArc", 8);
        UIManager.put("ScrollBar.width", 12);
        UIManager.put("TabbedPane.showTabSeparators", true);
        UIManager.put("OptionPane.okButtonText", "确定");
        UIManager.put("OptionPane.cancelButtonText", "取消");
        UIManager.put("FileChooser.saveButtonText", "保存");
        UIManager.put("FileChooser.cancelButtonText", "取消");

        Font font = pickChineseFont(14f);
        setUiFont(font);
    }

    /**
     * 选择系统中可用的中文字体，保证界面不出现方框乱码。
     *
     * @param size 字号
     * @return 字体对象
     */
    public static Font pickChineseFont(float size) {
        String[] candidates = {"微软雅黑", "Microsoft YaHei", "等线", "DengXian", "宋体", "SimSun"};
        String[] available = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames();
        for (String candidate : candidates) {
            for (String name : available) {
                if (name.equalsIgnoreCase(candidate)) {
                    return new Font(name, Font.PLAIN, (int) size);
                }
            }
        }
        return new Font(Font.SANS_SERIF, Font.PLAIN, (int) size);
    }

    /**
     * 批量设置各类控件的默认字体。
     *
     * @param font 字体
     */
    private static void setUiFont(Font font) {
        String[] keys = {"Label.font", "Button.font", "TextField.font", "PasswordField.font", "TextArea.font",
                "ComboBox.font", "CheckBox.font", "RadioButton.font", "Table.font", "TableHeader.font",
                "List.font", "Tree.font", "TitledBorder.font", "TabbedPane.font", "Menu.font", "MenuItem.font",
                "OptionPane.messageFont", "OptionPane.buttonFont", "Spinner.font", "ToolTip.font"};
        for (String key : keys) {
            UIManager.put(key, font);
        }
    }

    /**
     * 按尺寸生成加粗字体（用于标题）。
     *
     * @param size 字号
     * @return 加粗字体
     */
    public static Font boldFont(float size) {
        Font base = pickChineseFont(size);
        return base.deriveFont(Font.BOLD);
    }

    /**
     * 加载 SVG 图标（FlatLaf 扩展能力），失败时返回 null 由界面降级为纯文字。
     *
     * @param path classpath 路径
     * @param size 图标尺寸
     * @return 图标对象，加载失败返回 null
     */
    public static FlatSVGIcon svgIcon(String path, int size) {
        try {
            FlatSVGIcon icon = new FlatSVGIcon(path, size, size);
            return icon;
        } catch (Exception e) {
            return null;
        }
    }
}
