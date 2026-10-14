package com.course.sims.ui.common;

import com.course.sims.config.UiTheme;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 界面通用工具类。
 *
 * <p>集中提供对话框、文件选择、组件样式、数值格式化等重复性代码，
 * 保证 10 个模块的交互风格完全一致。</p>
 *
 * @author 课程设计第 2 小组 - 成员 D
 * @version 1.0.0
 */
public final class UiUtil {

    private static final SimpleDateFormat DATE_TIME = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final SimpleDateFormat DATE = new SimpleDateFormat("yyyy-MM-dd");

    private UiUtil() {
    }

    /**
     * 显示信息提示框。
     *
     * @param parent  父组件
     * @param message 信息
     */
    public static void info(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "提示", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * 显示成功提示框。
     *
     * @param parent  父组件
     * @param message 信息
     */
    public static void success(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "操作成功", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * 显示错误提示框。
     *
     * @param parent  父组件
     * @param message 信息
     */
    public static void error(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "操作失败", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * 显示警告提示框。
     *
     * @param parent  父组件
     * @param message 信息
     */
    public static void warn(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "警告", JOptionPane.WARNING_MESSAGE);
    }

    /**
     * 显示确认对话框。
     *
     * @param parent  父组件
     * @param message 信息
     * @return 用户点击"是"返回 true
     */
    public static boolean confirm(Component parent, String message) {
        return JOptionPane.showConfirmDialog(parent, message, "操作确认",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    /**
     * 显示输入对话框。
     *
     * @param parent       父组件
     * @param message      提示
     * @param defaultValue 默认值
     * @return 用户输入，取消返回 null
     */
    public static String input(Component parent, String message, String defaultValue) {
        return JOptionPane.showInputDialog(parent, message, defaultValue);
    }

    /**
     * 弹出 Excel 保存文件选择框。
     *
     * @param parent       父组件
     * @param defaultName  默认文件名
     * @return 目标文件，取消返回 null
     */
    public static File chooseSaveExcel(Component parent, String defaultName) {
        return chooseSaveFile(parent, defaultName, "Excel 工作簿 (*.xlsx)", "xlsx");
    }

    /**
     * 弹出保存文件选择框。
     *
     * @param parent      父组件
     * @param defaultName 默认文件名
     * @param filterDesc  过滤器描述
     * @param extension   扩展名
     * @return 目标文件，取消返回 null
     */
    public static File chooseSaveFile(Component parent, String defaultName, String filterDesc, String extension) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("选择保存位置");
        chooser.setSelectedFile(new File(defaultName));
        chooser.setFileFilter(new FileNameExtensionFilter(filterDesc, extension));
        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith("." + extension)) {
            file = new File(file.getAbsolutePath() + "." + extension);
        }
        return file;
    }

    /**
     * 弹出打开文件选择框。
     *
     * @param parent     父组件
     * @param filterDesc 过滤器描述
     * @param extension  扩展名
     * @return 选择的文件，取消返回 null
     */
    public static File chooseOpenFile(Component parent, String filterDesc, String extension) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("选择文件");
        chooser.setFileFilter(new FileNameExtensionFilter(filterDesc, extension));
        if (chooser.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        return chooser.getSelectedFile();
    }

    /**
     * 创建统一样式的表单标签。
     *
     * @param text 文本
     * @return 标签
     */
    public static JLabel formLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(UiTheme.pickChineseFont(13f));
        return label;
    }

    /**
     * 创建统一样式的必填项标签（带红色星号）。
     *
     * @param text 文本
     * @return 标签
     */
    public static JLabel requiredLabel(String text) {
        JLabel label = new JLabel("<html>" + text + " <font color='red'>*</font></html>");
        label.setFont(UiTheme.pickChineseFont(13f));
        return label;
    }

    /**
     * 创建统一样式的文本框。
     *
     * @param columns 列数
     * @return 文本框
     */
    public static JTextField textField(int columns) {
        JTextField field = new JTextField(columns);
        field.setFont(UiTheme.pickChineseFont(13f));
        field.setPreferredSize(new Dimension(columns * 9, 28));
        return field;
    }

    /**
     * 创建统一样式的按钮。
     *
     * @param text  文字
     * @param color 背景色
     * @return 按钮
     */
    public static JButton button(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(UiTheme.pickChineseFont(13f));
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));
        return button;
    }

    /**
     * 创建卡片式面板（白底 + 细边框 + 内边距）。
     *
     * @param title 卡片标题，可为 null
     * @return 面板
     */
    public static JPanel card(String title) {
        JPanel panel = new JPanel(new java.awt.BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE5E7EB)),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        if (title != null) {
            JLabel label = new JLabel(title);
            label.setFont(UiTheme.boldFont(15f));
            label.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
            panel.add(label, java.awt.BorderLayout.NORTH);
        }
        return panel;
    }

    /**
     * 创建统计数值卡片（大号数字 + 说明文字），用于首页看板。
     *
     * @param title 说明文字
     * @param value 数值
     * @param color 数字颜色
     * @return 卡片面板
     */
    public static JPanel statCard(String title, String value, Color color) {
        JPanel panel = new JPanel(new java.awt.BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE5E7EB)),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)));
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(UiTheme.pickChineseFont(13f));
        titleLabel.setForeground(new Color(0x6B7280));
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(UiTheme.boldFont(26f));
        valueLabel.setForeground(color);
        panel.add(titleLabel, java.awt.BorderLayout.NORTH);
        panel.add(valueLabel, java.awt.BorderLayout.CENTER);
        panel.setPreferredSize(new Dimension(170, 84));
        return panel;
    }

    /**
     * 创建统计数字卡片并返回数值标签，便于后续动态更新。
     *
     * @param title 说明文字
     * @param color 数字颜色
     * @return 数组 [卡片面板, 数值标签]
     */
    public static Object[] statCardWithLabel(String title, Color color) {
        JPanel panel = new JPanel(new java.awt.BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE5E7EB)),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)));
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(UiTheme.pickChineseFont(13f));
        titleLabel.setForeground(new Color(0x6B7280));
        JLabel valueLabel = new JLabel("--");
        valueLabel.setFont(UiTheme.boldFont(26f));
        valueLabel.setForeground(color);
        panel.add(titleLabel, java.awt.BorderLayout.NORTH);
        panel.add(valueLabel, java.awt.BorderLayout.CENTER);
        panel.setPreferredSize(new Dimension(170, 84));
        return new Object[]{panel, valueLabel};
    }

    /**
     * 创建工具条面板。
     *
     * @return 工具条
     */
    public static JPanel toolBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        bar.setOpaque(false);
        return bar;
    }

    /**
     * 格式化日期时间。
     *
     * @param date 日期
     * @return 字符串
     */
    public static String formatDateTime(Date date) {
        return date == null ? "" : DATE_TIME.format(date);
    }

    /**
     * 格式化日期。
     *
     * @param date 日期
     * @return 字符串
     */
    public static String formatDate(Date date) {
        return date == null ? "" : DATE.format(date);
    }

    /**
     * 数值格式化：保留 1 位小数，null 显示为空。
     *
     * @param value 数值
     * @return 字符串
     */
    public static String formatScore(Number value) {
        if (value == null) {
            return "";
        }
        return BigDecimal.valueOf(value.doubleValue()).setScale(1, RoundingMode.HALF_UP).toPlainString();
    }

    /**
     * 数值格式化：保留 2 位小数。
     *
     * @param value 数值
     * @return 字符串
     */
    public static String format2(Number value) {
        if (value == null) {
            return "";
        }
        return BigDecimal.valueOf(value.doubleValue()).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /**
     * 表格整体样式设置。
     *
     * @param table 表格
     */
    public static void styleTable(JTable table) {
        table.setRowHeight(28);
        table.setFont(UiTheme.pickChineseFont(13f));
        table.getTableHeader().setFont(UiTheme.boldFont(13f));
        table.setGridColor(new Color(0xE5E7EB));
        table.setShowGrid(true);
    }

    /**
     * 设置组件字体。
     *
     * @param font  字体
     * @param items 组件
     */
    public static void font(Font font, Component... items) {
        for (Component c : items) {
            c.setFont(font);
        }
    }
}
