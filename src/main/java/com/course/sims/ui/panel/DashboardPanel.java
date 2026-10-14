package com.course.sims.ui.panel;

import com.course.sims.config.UiTheme;
import com.course.sims.entity.SysLog;
import com.course.sims.service.DashboardStats;
import com.course.sims.service.ReportService;
import com.course.sims.service.StatsService;
import com.course.sims.service.SystemService;
import com.course.sims.ui.common.ChartPanel;
import com.course.sims.ui.common.Refreshable;
import com.course.sims.ui.common.UiUtil;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 数据看板面板（模块 M1 首页概览 + 模块 M7 统计可视化）。
 *
 * <p>本面板不使用 {@link com.course.sims.ui.common.BasePanel} 的"查询 + 表格 + 分页"骨架，
 * 因为看板关注的是聚合指标而不是明细列表；因此直接继承 {@link JPanel} 并按
 * {@link BorderLayout} 组织三段式布局：</p>
 * <ul>
 *   <li>顶部：面板标题与「刷新数据」「导出看板」按钮；</li>
 *   <li>中部：8 张统计卡片（2 行 4 列）与 3 个自绘图表（班级平均分排名、成绩等级分布、考勤状态统计）；</li>
 *   <li>底部：系统动态卡片，展示最近 8 条操作日志。</li>
 * </ul>
 *
 * <p>实现 {@link Refreshable} 接口，主窗体切换模块或点击刷新时统一调用 {@link #refreshData()}；
 * 全部取数逻辑包裹在 {@code try-catch RuntimeException} 中，保证异常不会抛到 EDT。</p>
 *
 * @author 课程设计第 2 小组 - 成员 B/C
 * @version 1.0.0
 */
public class DashboardPanel extends JPanel implements Refreshable {

    private static final long serialVersionUID = 1L;

    /** 统计卡片容器：2 行 4 列 */
    private final JPanel statCardGrid = new JPanel(new GridLayout(2, 4, 10, 10));
    /** 图表容器：1 行 3 列 */
    private final JPanel chartGrid = new JPanel(new GridLayout(1, 3, 10, 10));

    /** 统计卡片数值标签：学生总数 */
    private final JLabel studentTotalLabel = addStatCard(statCardGrid, "学生总数", UiTheme.PRIMARY);
    /** 统计卡片数值标签：在读学生 */
    private final JLabel studyingTotalLabel = addStatCard(statCardGrid, "在读学生", UiTheme.SUCCESS);
    /** 统计卡片数值标签：班级总数 */
    private final JLabel classTotalLabel = addStatCard(statCardGrid, "班级总数", new Color(0x0EA5E9));
    /** 统计卡片数值标签：专业总数 */
    private final JLabel majorTotalLabel = addStatCard(statCardGrid, "专业总数", new Color(0x8B5CF6));
    /** 统计卡片数值标签：课程总数 */
    private final JLabel courseTotalLabel = addStatCard(statCardGrid, "课程总数", UiTheme.WARNING);
    /** 统计卡片数值标签：成绩记录数 */
    private final JLabel scoreTotalLabel = addStatCard(statCardGrid, "成绩记录数", new Color(0xEC4899));
    /** 统计卡片数值标签：平均分 */
    private final JLabel avgScoreLabel = addStatCard(statCardGrid, "平均分", new Color(0x14B8A6));
    /** 统计卡片数值标签：及格率 */
    private final JLabel passRateLabel = addStatCard(statCardGrid, "及格率(%)", UiTheme.DANGER);

    /** 图表：班级平均分排名（横向条形图，最多 8 个班级） */
    private final ChartPanel classRankChart = new ChartPanel(ChartPanel.Type.H_BAR, "班级平均分排名");
    /** 图表：成绩等级分布（饼图） */
    private final ChartPanel scoreDistChart = new ChartPanel(ChartPanel.Type.PIE, "成绩等级分布");
    /** 图表：考勤状态统计（柱状图） */
    private final ChartPanel attendanceChart = new ChartPanel(ChartPanel.Type.BAR, "考勤状态统计");

    /** 系统动态表格模型（只读） */
    private final DefaultTableModel logModel = new DefaultTableModel() {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    /** 系统动态表格 */
    private final JTable logTable = new JTable(logModel);

    /**
     * 构造数据看板面板。
     */
    public DashboardPanel() {
        setLayout(new BorderLayout());
        setBackground(UiTheme.PAGE_BG);
        setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        statCardGrid.setOpaque(false);
        chartGrid.setOpaque(false);
        chartGrid.add(classRankChart);
        chartGrid.add(scoreDistChart);
        chartGrid.add(attendanceChart);

        add(buildHeader(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);

        refreshData();
    }

    /**
     * 构建顶部标题与操作按钮。
     *
     * @return 顶部面板
     */
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel titleLabel = new JLabel("数据看板");
        titleLabel.setFont(UiTheme.boldFont(18f));
        titleLabel.setForeground(new Color(0x1F2937));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(2, 2, 8, 2));
        header.add(titleLabel, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 2));
        right.setOpaque(false);
        JButton refresh = UiUtil.button("刷新数据", UiTheme.PRIMARY);
        refresh.setFont(UiTheme.pickChineseFont(12f));
        refresh.addActionListener(e -> refreshData());
        JButton export = UiUtil.button("导出看板", UiTheme.SUCCESS);
        export.setFont(UiTheme.pickChineseFont(12f));
        export.addActionListener(e -> exportDashboard());
        right.add(refresh);
        right.add(export);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    /**
     * 构建中部区域：统计卡片 + 图表 + 系统动态。
     *
     * @return 中部面板
     */
    private JPanel buildCenter() {
        JPanel center = new JPanel(new BorderLayout(0, 10));
        center.setOpaque(false);
        center.add(statCardGrid, BorderLayout.NORTH);
        center.add(chartGrid, BorderLayout.CENTER);
        center.add(buildLogCard(), BorderLayout.SOUTH);
        return center;
    }

    /**
     * 构建底部「系统动态」卡片（最近 8 条操作日志）。
     *
     * @return 卡片面板
     */
    private JPanel buildLogCard() {
        JPanel card = UiUtil.card("系统动态");
        card.setPreferredSize(new Dimension(0, 180));

        logModel.setColumnIdentifiers(new String[]{"时间", "操作人", "操作", "详情"});
        UiUtil.styleTable(logTable);
        logTable.setFillsViewportHeight(true);
        logTable.getTableHeader().setReorderingAllowed(false);
        logTable.getColumnModel().getColumn(0).setPreferredWidth(150);
        logTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        logTable.getColumnModel().getColumn(2).setPreferredWidth(130);
        logTable.getColumnModel().getColumn(3).setPreferredWidth(600);

        JScrollPane scrollPane = new JScrollPane(logTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);
        card.add(scrollPane, BorderLayout.CENTER);
        return card;
    }

    /**
     * 刷新看板全部数据：统计卡片、三个图表与系统动态。
     * <p>任何运行时异常都在此捕获并以对话框提示，避免异常抛到事件分发线程。</p>
     */
    @Override
    public void refreshData() {
        try {
            StatsService statsService = new StatsService();
            DashboardStats stats = statsService.dashboard();

            // 1. 统计卡片
            studentTotalLabel.setText(String.valueOf(stats.getStudentTotal()));
            studyingTotalLabel.setText(String.valueOf(stats.getStudyingTotal()));
            classTotalLabel.setText(String.valueOf(stats.getClassTotal()));
            majorTotalLabel.setText(String.valueOf(stats.getMajorTotal()));
            courseTotalLabel.setText(String.valueOf(stats.getCourseTotal()));
            scoreTotalLabel.setText(String.valueOf(stats.getScoreTotal()));
            avgScoreLabel.setText(UiUtil.format2(stats.getAvgScore()));
            passRateLabel.setText(UiUtil.format2(stats.getPassRate()));

            // 2. 班级平均分排名（最多显示前 8 个班级）
            List<String> rankLabels = new ArrayList<>();
            List<Double> rankValues = new ArrayList<>();
            for (String[] row : stats.getClassRank()) {
                if (rankLabels.size() >= 8) {
                    break;
                }
                if (row == null || row.length < 2) {
                    continue;
                }
                rankLabels.add(row[0]);
                rankValues.add(toDouble(row[1]));
            }
            classRankChart.setData(rankLabels, rankValues);

            // 3. 成绩等级分布
            List<String> levelLabels = new ArrayList<>();
            List<Double> levelValues = new ArrayList<>();
            for (String[] row : stats.getScoreDistribution()) {
                if (row == null || row.length < 2) {
                    continue;
                }
                levelLabels.add(row[0]);
                levelValues.add(toDouble(row[1]));
            }
            scoreDistChart.setData(levelLabels, levelValues);

            // 4. 考勤状态统计（人数为 0 的状态不显示）
            List<String> attendLabels = new ArrayList<>();
            List<Double> attendValues = new ArrayList<>();
            for (Map<String, Object> item : statsService.attendanceStat()) {
                if (item == null) {
                    continue;
                }
                Object status = item.get("status");
                double num = toDouble(String.valueOf(item.get("num")));
                if (status == null || num <= 0) {
                    continue;
                }
                attendLabels.add(String.valueOf(status));
                attendValues.add(num);
            }
            attendanceChart.setData(attendLabels, attendValues);

            // 5. 系统动态：最近 8 条操作日志
            logModel.setRowCount(0);
            for (SysLog log : new SystemService().recentLogs(8)) {
                logModel.addRow(new Object[]{UiUtil.formatDateTime(log.getLogTime()), log.getUsername(),
                        log.getOperation(), log.getDetail()});
            }
        } catch (RuntimeException e) {
            UiUtil.error(this, "加载看板数据失败：" + e.getMessage());
        }
    }

    /**
     * 导出看板汇总数据到 Excel。
     */
    private void exportDashboard() {
        File file = UiUtil.chooseSaveExcel(this, "数据看板.xlsx");
        if (file == null) {
            return;
        }
        try {
            int count = new ReportService().exportDashboard(file);
            UiUtil.success(this, "导出成功，共 " + count + " 项指标\n文件位置：" + file.getAbsolutePath());
        } catch (RuntimeException e) {
            UiUtil.error(this, "导出失败：" + e.getMessage());
        }
    }

    /**
     * 在统计卡片容器中追加一张卡片。
     *
     * @param container 卡片容器
     * @param title     卡片标题
     * @param color     数值颜色
     * @return 数值标签，供后续动态更新
     */
    private static JLabel addStatCard(JPanel container, String title, Color color) {
        Object[] card = UiUtil.statCardWithLabel(title, color);
        container.add((Component) card[0]);
        return (JLabel) card[1];
    }

    /**
     * 安全地把字符串解析为 double。
     *
     * @param text 文本
     * @return 数值，解析失败返回 0
     */
    private static double toDouble(String text) {
        try {
            return Double.parseDouble(text == null ? "" : text.trim());
        } catch (NumberFormatException e) {
            return 0D;
        }
    }
}
