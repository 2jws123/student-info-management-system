package com.course.sims.ui.common;

import com.course.sims.config.UiTheme;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 自绘统计图表组件（柱状图 / 饼图 / 横向条形图）。
 *
 * <p>为降低课程设计的部署复杂度，统计图形不引入第三方图表库，
 * 而是直接使用 Java2D 绘制：{@link #paintComponent(Graphics)} 中完成坐标换算与绘制，
 * 支持柱状图、饼图、横向条形图三种形态，满足"统计分析"模块的可视化需求。</p>
 *
 * @author 课程设计第 2 小组 - 成员 E
 * @version 1.0.0
 */
public class ChartPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    /** 图表类型 */
    public enum Type {
        /** 柱状图 */
        BAR,
        /** 饼图 */
        PIE,
        /** 横向条形图 */
        H_BAR
    }

    /** 配色方案 */
    private static final Color[] PALETTE = {
            new Color(0x2F6FED), new Color(0x16A34A), new Color(0xF59E0B), new Color(0xDC2626),
            new Color(0x8B5CF6), new Color(0x0EA5E9), new Color(0xEC4899), new Color(0x14B8A6),
            new Color(0x6366F1), new Color(0x84CC16)
    };

    private final Type type;
    private final String title;
    private final List<String> labels = new ArrayList<>();
    private final List<Double> values = new ArrayList<>();

    /**
     * 构造图表组件。
     *
     * @param type  图表类型
     * @param title 图表标题
     */
    public ChartPanel(Type type, String title) {
        this.type = type;
        this.title = title;
        setBackground(Color.WHITE);
        setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(new Color(0xE5E7EB)),
                javax.swing.BorderFactory.createEmptyBorder(8, 12, 12, 12)));
    }

    /**
     * 设置图表数据。
     *
     * @param labels 标签数组
     * @param values 数值数组
     */
    public void setData(String[] labels, double[] values) {
        this.labels.clear();
        this.values.clear();
        for (int i = 0; i < labels.length; i++) {
            this.labels.add(labels[i]);
            this.values.add(i < values.length ? values[i] : 0D);
        }
        repaint();
    }

    /**
     * 设置图表数据（List 形式）。
     *
     * @param labels 标签列表
     * @param values 数值列表
     */
    public void setData(List<String> labels, List<Double> values) {
        this.labels.clear();
        this.values.clear();
        this.labels.addAll(labels);
        this.values.addAll(values);
        repaint();
    }

    /**
     * 清空数据。
     */
    public void clearData() {
        labels.clear();
        values.clear();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(UiTheme.boldFont(14f));
            g2.setColor(new Color(0x1F2937));
            g2.drawString(title, 10, 20);

            int top = 38;
            int left = 12;
            int right = getWidth() - 16;
            int bottom = getHeight() - 12;
            if (labels.isEmpty() || right - left < 40 || bottom - top < 40) {
                g2.setFont(UiTheme.pickChineseFont(13f));
                g2.setColor(new Color(0x9CA3AF));
                g2.drawString("暂无数据", left, top + 20);
                return;
            }
            switch (type) {
                case PIE:
                    drawPie(g2, left, top, right, bottom);
                    break;
                case H_BAR:
                    drawHorizontalBar(g2, left, top, right, bottom);
                    break;
                default:
                    drawBar(g2, left, top, right, bottom);
                    break;
            }
        } finally {
            g2.dispose();
        }
    }

    /**
     * 绘制柱状图。
     */
    private void drawBar(Graphics2D g2, int left, int top, int right, int bottom) {
        double max = maxValue();
        int chartBottom = bottom - 22;
        int chartHeight = chartBottom - top - 6;
        int count = labels.size();
        int gap = 14;
        int barWidth = Math.max(10, (right - left - gap * (count + 1)) / count);

        // 坐标轴
        g2.setColor(new Color(0xD1D5DB));
        g2.setStroke(new BasicStroke(1f));
        g2.drawLine(left, chartBottom, right, chartBottom);

        g2.setFont(UiTheme.pickChineseFont(11f));
        FontMetrics fm = g2.getFontMetrics();
        for (int i = 0; i < count; i++) {
            double value = values.get(i);
            int height = max <= 0 ? 0 : (int) Math.round(value / max * chartHeight);
            int x = left + gap + i * (barWidth + gap);
            int y = chartBottom - height;
            g2.setColor(PALETTE[i % PALETTE.length]);
            g2.fillRoundRect(x, y, barWidth, height, 6, 6);

            // 数值
            g2.setColor(new Color(0x374151));
            String valueText = format(value);
            g2.drawString(valueText, x + (barWidth - fm.stringWidth(valueText)) / 2, Math.max(y - 4, top + 10));

            // 标签（旋转前先尝试水平居中，过长则截断）
            String label = labels.get(i);
            if (fm.stringWidth(label) > barWidth + gap) {
                label = label.length() > 4 ? label.substring(0, 4) + "…" : label;
            }
            g2.setColor(new Color(0x6B7280));
            g2.drawString(label, x + (barWidth - fm.stringWidth(label)) / 2, chartBottom + 15);
        }
    }

    /**
     * 绘制横向条形图（适合类别名称较长的场景）。
     */
    private void drawHorizontalBar(Graphics2D g2, int left, int top, int right, int bottom) {
        double max = maxValue();
        int count = labels.size();
        int labelWidth = 96;
        int barArea = right - left - labelWidth - 52;
        int rowHeight = Math.max(16, Math.min(30, (bottom - top) / Math.max(count, 1)));
        g2.setFont(UiTheme.pickChineseFont(12f));
        FontMetrics fm = g2.getFontMetrics();
        for (int i = 0; i < count; i++) {
            int y = top + i * rowHeight;
            if (y + rowHeight > bottom) {
                break;
            }
            String label = labels.get(i);
            if (fm.stringWidth(label) > labelWidth - 6) {
                label = label.substring(0, Math.min(label.length(), 6)) + "…";
            }
            g2.setColor(new Color(0x374151));
            g2.drawString(label, left, y + rowHeight - 7);

            double value = values.get(i);
            int width = max <= 0 ? 0 : (int) Math.round(value / max * barArea);
            g2.setColor(PALETTE[i % PALETTE.length]);
            g2.fillRoundRect(left + labelWidth, y + 4, width, rowHeight - 10, 6, 6);

            g2.setColor(new Color(0x6B7280));
            g2.drawString(format(value), left + labelWidth + width + 6, y + rowHeight - 7);
        }
    }

    /**
     * 绘制饼图（含图例）。
     */
    private void drawPie(Graphics2D g2, int left, int top, int right, int bottom) {
        double total = 0D;
        for (double v : values) {
            total += v;
        }
        int size = Math.min(right - left - 150, bottom - top);
        if (size < 60) {
            size = Math.max(60, bottom - top);
        }
        int cx = left + size / 2 + 10;
        int cy = top + (bottom - top) / 2;
        int startAngle = 90;
        g2.setFont(UiTheme.pickChineseFont(12f));
        FontMetrics fm = g2.getFontMetrics();
        for (int i = 0; i < values.size(); i++) {
            double value = values.get(i);
            int angle = total <= 0 ? 0 : (int) Math.round(value / total * 360);
            g2.setColor(PALETTE[i % PALETTE.length]);
            g2.fillArc(cx - size / 2, cy - size / 2, size, size, startAngle, -angle);
            g2.setColor(Color.WHITE);
            g2.drawArc(cx - size / 2, cy - size / 2, size, size, startAngle, -angle);
            startAngle -= angle;
        }
        // 图例
        int legendX = cx + size / 2 + 18;
        int legendY = top + 6;
        for (int i = 0; i < labels.size(); i++) {
            int y = legendY + i * 20;
            if (y > bottom - 6) {
                break;
            }
            g2.setColor(PALETTE[i % PALETTE.length]);
            g2.fillRoundRect(legendX, y, 12, 12, 3, 3);
            g2.setColor(new Color(0x374151));
            String text = labels.get(i) + "  " + format(values.get(i));
            if (fm.stringWidth(text) > right - legendX - 14) {
                text = text.substring(0, Math.min(text.length(), 8)) + "…";
            }
            g2.drawString(text, legendX + 18, y + 11);
        }
    }

    /**
     * 求数值最大值（至少为 1，避免除零）。
     *
     * @return 最大值
     */
    private double maxValue() {
        double max = 0D;
        for (double v : values) {
            max = Math.max(max, v);
        }
        return max <= 0 ? 1D : max;
    }

    /**
     * 数值显示格式：整数不带小数点。
     *
     * @param value 数值
     * @return 文本
     */
    private String format(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.001) {
            return String.valueOf((long) Math.rint(value));
        }
        return String.format("%.1f", value);
    }

    /**
     * 构造简单的键值对图表数据（辅助方法）。
     *
     * @param keys   键
     * @param values 值
     * @return 图表组件
     */
    public static ChartPanel of(Type type, String title, String[] keys, double[] values) {
        ChartPanel panel = new ChartPanel(type, title);
        panel.setData(Arrays.copyOf(keys, keys.length), values);
        return panel;
    }
}
