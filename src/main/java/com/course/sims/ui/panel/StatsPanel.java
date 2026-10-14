package com.course.sims.ui.panel;

import com.course.sims.config.UiTheme;
import com.course.sims.service.ReportService;
import com.course.sims.service.StatsService;
import com.course.sims.service.TeachingService;
import com.course.sims.ui.common.ChartPanel;
import com.course.sims.ui.common.Refreshable;
import com.course.sims.ui.common.UiUtil;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 统计分析报表面板（模块 M7）。
 *
 * <p>与其它业务面板不同，本面板不使用基类的表格骨架，而是以"标签页 + 图表"的方式
 * 组织四类统计视图：成绩概览（分数段柱状图、等级饼图）、班级排名（表格 + 横向条形图）、
 * 课程统计（表格）、学生排名与分布（排名表格 + 专业分布饼图 + 教师任务柱状图）。</p>
 *
 * <p>所有统计均在数据库端聚合完成，界面只负责展示；学期下拉框用于切换统计范围，
 * "全部"表示不按学期过滤。</p>
 *
 * @author 课程设计第 2 小组 - 成员 C
 * @version 1.0.0
 */
public class StatsPanel extends JPanel implements Refreshable {

    private static final long serialVersionUID = 1L;

    /** 统计业务服务 */
    private final StatsService statsService = new StatsService();
    /** 教学基础数据服务（学期列表） */
    private final TeachingService teachingService = new TeachingService();
    /** 报表业务服务（导出使用） */
    private final ReportService reportService = new ReportService();

    /** 学期下拉框 */
    private final JComboBox<Object> semesterBox = new JComboBox<>();
    /** 标签页容器 */
    private final JTabbedPane tabs = new JTabbedPane();

    /** 班级排名表格模型 */
    private final DefaultTableModel classRankModel = readOnlyModel(
            new String[]{"排名", "班级", "人数", "平均分", "及格率(%)"});
    /** 班级排名表格 */
    private final JTable classRankTable = new JTable(classRankModel);
    /** 课程统计表格模型 */
    private final DefaultTableModel courseStatModel = readOnlyModel(
            new String[]{"课程", "平均分", "最高分", "最低分", "及格率(%)"});
    /** 课程统计表格 */
    private final JTable courseStatTable = new JTable(courseStatModel);
    /** 学生排名表格模型 */
    private final DefaultTableModel studentRankModel = readOnlyModel(
            new String[]{"排名", "学号", "姓名", "班级", "平均分", "课程数", "已获学分"});
    /** 学生排名表格 */
    private final JTable studentRankTable = new JTable(studentRankModel);

    /** 分数段人数分布（柱状图） */
    private final ChartPanel segmentChart = new ChartPanel(ChartPanel.Type.BAR, "分数段人数分布");
    /** 成绩等级分布（饼图） */
    private final ChartPanel levelChart = new ChartPanel(ChartPanel.Type.PIE, "成绩等级分布");
    /** 班级平均分排名（横向条形图） */
    private final ChartPanel classRankChart = new ChartPanel(ChartPanel.Type.H_BAR, "班级平均分排名");
    /** 专业人数分布（饼图） */
    private final ChartPanel majorChart = new ChartPanel(ChartPanel.Type.PIE, "专业人数分布");
    /** 教师授课任务（柱状图） */
    private final ChartPanel teacherChart = new ChartPanel(ChartPanel.Type.BAR, "教师授课任务");

    /**
     * 构造统计分析报表面板。
     */
    public StatsPanel() {
        setLayout(new BorderLayout());
        setBackground(UiTheme.PAGE_BG);
        setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        add(buildTopBar(), BorderLayout.NORTH);
        add(buildTabs(), BorderLayout.CENTER);
        refreshData();
    }

    /**
     * 构建顶部工具条：标题 + 学期下拉框 + 刷新 / 导出按钮。
     *
     * @return 工具条面板
     */
    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        bar.setOpaque(false);

        JLabel titleLabel = new JLabel("统计分析报表");
        titleLabel.setFont(UiTheme.boldFont(18f));
        titleLabel.setForeground(new Color(0x1F2937));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(2, 2, 8, 12));

        loadSemesterOptions();
        semesterBox.setFont(UiTheme.pickChineseFont(13f));
        semesterBox.setPreferredSize(new Dimension(150, 28));
        semesterBox.addActionListener(e -> refreshData());

        JButton refresh = UiUtil.button("刷新统计", UiTheme.PRIMARY);
        refresh.addActionListener(e -> refreshData());
        JButton export = UiUtil.button("导出统计报表", UiTheme.SUCCESS);
        export.addActionListener(e -> exportReport());

        bar.add(titleLabel);
        bar.add(UiUtil.formLabel("学期"));
        bar.add(semesterBox);
        bar.add(refresh);
        bar.add(export);
        return bar;
    }

    /**
     * 加载学期下拉选项，第一项为"全部"。
     */
    private void loadSemesterOptions() {
        semesterBox.addItem("全部");
        for (String semester : teachingService.findAllSemesters()) {
            semesterBox.addItem(semester);
        }
    }

    /**
     * 构建四个统计标签页。
     *
     * @return 标签页面板
     */
    private JTabbedPane buildTabs() {
        tabs.setFont(UiTheme.pickChineseFont(13f));
        tabs.addTab("成绩概览", buildOverviewTab());
        tabs.addTab("班级排名", buildClassRankTab());
        tabs.addTab("课程统计", buildCourseTab());
        tabs.addTab("学生排名 / 分布", buildStudentTab());
        return tabs;
    }

    /**
     * 构建"成绩概览"标签页：分数段柱状图 + 成绩等级饼图。
     *
     * @return 标签页面板
     */
    private JPanel buildOverviewTab() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 10, 10));
        panel.setBackground(UiTheme.PAGE_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 4, 4, 4));
        panel.add(segmentChart);
        panel.add(levelChart);
        return panel;
    }

    /**
     * 构建"班级排名"标签页：排名表格 + 班级平均分横向条形图。
     *
     * @return 标签页面板
     */
    private JPanel buildClassRankTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(UiTheme.PAGE_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 4, 4, 4));
        panel.add(wrapTable(classRankTable), BorderLayout.CENTER);
        classRankChart.setPreferredSize(new Dimension(0, 220));
        panel.add(classRankChart, BorderLayout.SOUTH);
        return panel;
    }

    /**
     * 构建"课程统计"标签页：课程成绩统计表格。
     *
     * @return 标签页面板
     */
    private JPanel buildCourseTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UiTheme.PAGE_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 4, 4, 4));
        panel.add(wrapTable(courseStatTable), BorderLayout.CENTER);
        return panel;
    }

    /**
     * 构建"学生排名 / 分布"标签页：学生排名表格 + 专业分布饼图 + 教师任务柱状图。
     *
     * @return 标签页面板
     */
    private JPanel buildStudentTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(UiTheme.PAGE_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 4, 4, 4));
        panel.add(wrapTable(studentRankTable), BorderLayout.CENTER);

        JPanel chartRow = new JPanel(new GridLayout(1, 2, 10, 10));
        chartRow.setBackground(UiTheme.PAGE_BG);
        chartRow.setPreferredSize(new Dimension(0, 240));
        chartRow.add(majorChart);
        chartRow.add(teacherChart);
        panel.add(chartRow, BorderLayout.SOUTH);
        return panel;
    }

    /**
     * 把表格放入滚动面板并统一底色。
     *
     * @param table 表格
     * @return 滚动面板
     */
    private JScrollPane wrapTable(JTable table) {
        UiUtil.styleTable(table);
        table.setFillsViewportHeight(true);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(0xE5E7EB)));
        scrollPane.getViewport().setBackground(Color.WHITE);
        return scrollPane;
    }

    @Override
    public void refreshData() {
        try {
            String semester = selectedSemester();
            refreshScoreOverview();
            refreshClassRank(semester);
            refreshCourseStat(semester);
            refreshStudentRank(semester);
            refreshDistribution();
        } catch (RuntimeException e) {
            UiUtil.error(this, "统计失败：" + e.getMessage());
        }
    }

    /**
     * 获取当前选中的学期。
     *
     * @return 学期，选择"全部"返回 null
     */
    private String selectedSemester() {
        Object value = semesterBox.getSelectedItem();
        if (value == null || "全部".equals(value)) {
            return null;
        }
        return String.valueOf(value);
    }

    /**
     * 刷新"成绩概览"页签的两张图表。
     */
    private void refreshScoreOverview() {
        List<String> segmentLabels = new ArrayList<>();
        List<Double> segmentValues = new ArrayList<>();
        for (Map<String, Object> row : statsService.scoreSegment()) {
            segmentLabels.add(str(row.get("score_range")));
            segmentValues.add(num(row.get("num")));
        }
        segmentChart.setData(segmentLabels, segmentValues);

        List<String> levelLabels = new ArrayList<>();
        List<Double> levelValues = new ArrayList<>();
        for (Map<String, Object> row : statsService.scoreDistribution(null)) {
            levelLabels.add(str(row.get("level_name")));
            levelValues.add(num(row.get("level_count")));
        }
        levelChart.setData(levelLabels, levelValues);
    }

    /**
     * 刷新"班级排名"页签的表格与图表。
     *
     * @param semester 学期，null 表示全部
     */
    private void refreshClassRank(String semester) {
        classRankModel.setRowCount(0);
        List<String> labels = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        int rank = 1;
        for (Map<String, Object> row : statsService.classAverageRank(semester)) {
            String className = str(row.get("class_name"));
            double avgScore = num(row.get("avg_score"));
            classRankModel.addRow(new Object[]{rank++, className, str(row.get("student_count")),
                    str(row.get("avg_score")), str(row.get("pass_rate"))});
            labels.add(className);
            values.add(avgScore);
        }
        classRankChart.setData(labels, values);
    }

    /**
     * 刷新"课程统计"页签的表格。
     *
     * @param semester 学期，null 表示全部
     */
    private void refreshCourseStat(String semester) {
        courseStatModel.setRowCount(0);
        for (Map<String, Object> row : statsService.courseAverageStat(semester)) {
            courseStatModel.addRow(new Object[]{str(row.get("course_name")), str(row.get("avg_score")),
                    str(row.get("max_score")), str(row.get("min_score")), str(row.get("pass_rate"))});
        }
    }

    /**
     * 刷新"学生排名"页签的表格（取前 20 名）。
     *
     * @param semester 学期，null 表示全部
     */
    private void refreshStudentRank(String semester) {
        studentRankModel.setRowCount(0);
        int rank = 1;
        for (Map<String, Object> row : statsService.studentRank(null, semester, 20)) {
            studentRankModel.addRow(new Object[]{rank++, str(row.get("sno")), str(row.get("name")),
                    str(row.get("class_name")), str(row.get("avg_score")), str(row.get("course_count")),
                    str(row.get("total_credit"))});
        }
    }

    /**
     * 刷新"学生排名 / 分布"页签的两张图表（专业分布、教师授课任务）。
     */
    private void refreshDistribution() {
        List<String> majorLabels = new ArrayList<>();
        List<Double> majorValues = new ArrayList<>();
        for (Map<String, Object> row : statsService.majorDistribution()) {
            majorLabels.add(str(row.get("major_name")));
            majorValues.add(num(row.get("student_count")));
        }
        majorChart.setData(majorLabels, majorValues);

        List<String> teacherLabels = new ArrayList<>();
        List<Double> teacherValues = new ArrayList<>();
        for (Map<String, Object> row : statsService.teacherWorkload()) {
            teacherLabels.add(str(row.get("teacher")));
            teacherValues.add(num(row.get("course_count")));
        }
        teacherChart.setData(teacherLabels, teacherValues);
    }

    /**
     * 导出综合统计报表（多工作表 Excel）。
     */
    private void exportReport() {
        File file = UiUtil.chooseSaveExcel(this, "统计分析报表.xlsx");
        if (file == null) {
            return;
        }
        try {
            int count = reportService.exportStatistics(file);
            UiUtil.success(this, "导出成功，共 " + count + " 行概览指标\n文件位置：" + file.getAbsolutePath());
        } catch (RuntimeException e) {
            UiUtil.error(this, "导出失败：" + e.getMessage());
        }
    }

    /**
     * 创建不可编辑的表格模型。
     *
     * @param columns 表头
     * @return 表格模型
     */
    private static DefaultTableModel readOnlyModel(String[] columns) {
        return new DefaultTableModel(columns, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    /**
     * Map 取值转字符串，null 转为空串（避免表格出现 "null"）。
     *
     * @param value 值
     * @return 字符串
     */
    private static String str(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    /**
     * Map 取值转数值，null 或非数字转为 0。
     *
     * @param value 值
     * @return 数值
     */
    private static double num(Object value) {
        if (value == null) {
            return 0D;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0D;
        }
    }
}
