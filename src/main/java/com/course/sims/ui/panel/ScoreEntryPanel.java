package com.course.sims.ui.panel;

import com.course.sims.config.UiTheme;
import com.course.sims.entity.Clazz;
import com.course.sims.entity.Course;
import com.course.sims.entity.Score;
import com.course.sims.entity.Student;
import com.course.sims.service.BusinessException;
import com.course.sims.service.ReportService;
import com.course.sims.service.ScoreService;
import com.course.sims.service.StudentService;
import com.course.sims.service.TeachingService;
import com.course.sims.ui.common.BasePanel;
import com.course.sims.ui.common.UiUtil;
import com.course.sims.util.ExcelUtil;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 成绩录入面板（模块 M5）。
 *
 * <p>以"整班批量录入工作台"的形式提供成绩录入能力：</p>
 * <ul>
 *   <li>顶部选择课程、班级与学期，并可查看该课程的统计指标（平均分 / 最高分 / 最低分 / 及格率）；</li>
 *   <li>点击【加载学生名单】按班级载入学生，并自动回填该课程已有成绩；</li>
 *   <li>表格中"平时成绩 / 期末成绩 / 补考成绩 / 备注"可直接编辑，
 *       总评成绩与等级随编辑自动重算（总评 = 平时 × 30% + 期末 × 70%，有补考时以补考为准）；</li>
 *   <li>支持一键填充及格线、Excel 导入成绩、导出成绩表。</li>
 * </ul>
 *
 * <p>说明：本面板不使用分页（基类的分页条仅作占位），因此不调用 {@code setPage}；
 * 同时由于基类的表格模型为只读模型，本面板在 {@code initUi()} 之后替换为可编辑模型。</p>
 *
 * @author 课程设计第 2 小组 - 成员 C
 * @version 1.0.0
 */
public class ScoreEntryPanel extends BasePanel {

    private static final long serialVersionUID = 1L;

    private final ScoreService scoreService = new ScoreService();
    private final StudentService studentService = new StudentService();
    private final TeachingService teachingService = new TeachingService();
    private final ReportService reportService = new ReportService();

    /** 课程下拉数据源（用于取课程所属学期） */
    private final List<Course> courseList = new ArrayList<>();
    private final JComboBox<Object> courseBox = new JComboBox<>();
    private final JComboBox<Object> classBox = new JComboBox<>();
    private final JComboBox<String> semesterBox = new JComboBox<>();
    private final JLabel statLabel = new JLabel("课程统计：-");
    /** 可编辑的成绩录入模型 */
    private final EntryTableModel entryTableModel = new EntryTableModel();

    /**
     * 构造成绩录入面板。
     */
    public ScoreEntryPanel() {
        super("成绩录入");
        initUi();
        // 基类表格模型禁止编辑，这里替换为支持成绩录入的可编辑模型
        table.setModel(entryTableModel);
        applyEntryTableStyle();
        entryTableModel.addTableModelListener(e -> onCellUpdated(e));
    }

    @Override
    protected void configureColumns() {
        // 本面板使用自定义的可编辑模型，此处仅为满足基类契约
        tableModel.setColumnIdentifiers(new String[]{"学生ID", "学号", "姓名", "平时成绩", "期末成绩",
                "补考成绩", "总评成绩", "等级", "备注"});
    }

    @Override
    protected void buildQueryBar() {
        loadOptions();

        courseBox.setFont(UiTheme.pickChineseFont(13f));
        courseBox.setPreferredSize(new Dimension(230, 28));
        courseBox.setRenderer(idNameRenderer());
        classBox.setFont(UiTheme.pickChineseFont(13f));
        classBox.setPreferredSize(new Dimension(170, 28));
        classBox.setRenderer(idNameRenderer());
        semesterBox.setFont(UiTheme.pickChineseFont(13f));
        semesterBox.setPreferredSize(new Dimension(130, 28));

        addQueryItem("课程", courseBox);
        addQueryItem("班级", classBox);
        addQueryItem("学期", semesterBox);

        JButton load = UiUtil.button("加载学生名单", UiTheme.PRIMARY);
        load.setFont(UiTheme.pickChineseFont(12f));
        load.addActionListener(e -> refreshData());
        queryBar.add(load);

        statLabel.setFont(UiTheme.pickChineseFont(12f));
        statLabel.setForeground(new Color(0x1F2937));
        queryBar.add(statLabel);
    }

    @Override
    protected void buildButtons() {
        addButton("保存全部", UiTheme.PRIMARY, e -> saveAll());
        addButton("一键及格线", UiTheme.WARNING, e -> fillPassLine());
        addButton("导入 Excel", new Color(0x0EA5E9), e -> importExcel());
        addButton("导出成绩表", UiTheme.SUCCESS, e -> exportExcel());
        addButton("刷新", new Color(0x6B7280), e -> refreshData());
    }

    @Override
    protected void loadData() {
        entryTableModel.setRowCount(0);
        Integer courseId = comboId(courseBox);
        Integer classId = comboId(classBox);
        if (courseId == null || classId == null) {
            statusLabel.setText("请选择课程与班级后点击【加载学生名单】");
            return;
        }
        List<Student> students = studentService.findByClass(classId);
        Map<Integer, Score> scoreMap = new HashMap<>();
        for (Score score : scoreService.findByCourse(courseId, classId)) {
            if (score.getStudentId() != null) {
                scoreMap.put(score.getStudentId(), score);
            }
        }
        for (Student student : students) {
            Score exist = scoreMap.get(student.getId());
            if (exist == null) {
                entryTableModel.addRow(new Object[]{student.getId(), student.getSno(), student.getName(),
                        "", "", "", "", "", ""});
            } else {
                entryTableModel.addRow(new Object[]{student.getId(), student.getSno(), student.getName(),
                        UiUtil.formatScore(exist.getUsualScore()), UiUtil.formatScore(exist.getExamScore()),
                        UiUtil.formatScore(exist.getMakeupScore()), UiUtil.formatScore(exist.getTotalScore()),
                        exist.getScoreLevel(), exist.getRemark() == null ? "" : exist.getRemark()});
            }
        }
        if (students.isEmpty()) {
            statusLabel.setText("该班级暂无学生，请先到【学生信息管理】添加学生");
        } else {
            statusLabel.setText("已加载 " + students.size() + " 名学生，其中 " + scoreMap.size() + " 名已有成绩记录");
        }
        updateCourseStat();
    }

    // ------------------------------------------------------------------
    //  界面装配辅助
    // ------------------------------------------------------------------

    /**
     * 加载课程、班级、学期下拉选项。
     */
    private void loadOptions() {
        courseList.clear();
        courseList.addAll(teachingService.findAllCourses());
        courseBox.removeAllItems();
        courseBox.addItem("请选择课程");
        for (Course course : courseList) {
            courseBox.addItem(new Object[]{course.getId(),
                    course.getCourseName() + "（" + course.getCourseCode() + "）"});
        }
        classBox.removeAllItems();
        classBox.addItem("请选择班级");
        for (Clazz clazz : teachingService.findAllClasses()) {
            classBox.addItem(new Object[]{clazz.getId(), clazz.getClassName()});
        }
        semesterBox.removeAllItems();
        for (String semester : teachingService.findAllSemesters()) {
            semesterBox.addItem(semester);
        }
        courseBox.addActionListener(e -> updateCourseStat());
    }

    /**
     * 替换表格模型后重新应用列样式（隐藏学生 ID 列并设置列宽）。
     */
    private void applyEntryTableStyle() {
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(center);
        }
        if (table.getColumnCount() > 0) {
            table.getColumnModel().getColumn(0).setMinWidth(0);
            table.getColumnModel().getColumn(0).setMaxWidth(0);
            table.getColumnModel().getColumn(0).setWidth(0);
        }
        int[] widths = {0, 90, 80, 90, 90, 90, 90, 70, 180};
        for (int i = 1; i < Math.min(widths.length, table.getColumnCount()); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    /**
     * 创建"只显示名称"的下拉渲染器（选项元素为 [主键, 名称]）。
     *
     * @return 渲染器
     */
    private static DefaultListCellRenderer idNameRenderer() {
        return new DefaultListCellRenderer() {
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
        };
    }

    // ------------------------------------------------------------------
    //  单元格编辑与成绩重算
    // ------------------------------------------------------------------

    /**
     * 表格编辑监听：编辑平时 / 期末 / 补考成绩时自动重算总评与等级。
     *
     * @param event 表格模型事件
     */
    private void onCellUpdated(TableModelEvent event) {
        if (event.getType() != TableModelEvent.UPDATE) {
            return;
        }
        int column = event.getColumn();
        if (column == 3 || column == 4 || column == 5) {
            recalcRow(event.getFirstRow());
        }
    }

    /**
     * 重算指定行的总评成绩与等级（解析失败一律按未填写处理，不抛异常）。
     *
     * @param row 行号
     */
    private void recalcRow(int row) {
        if (row < 0 || row >= entryTableModel.getRowCount()) {
            return;
        }
        Double usual = parseScore(entryTableModel.getValueAt(row, 3));
        Double exam = parseScore(entryTableModel.getValueAt(row, 4));
        Double makeup = parseScore(entryTableModel.getValueAt(row, 5));
        Double total;
        if (makeup != null) {
            total = round1(makeup);
        } else if (usual == null && exam == null) {
            total = null;
        } else {
            double usualValue = usual == null ? 0D : usual;
            double examValue = exam == null ? 0D : exam;
            total = round1(usualValue * ScoreService.USUAL_RATIO + examValue * ScoreService.EXAM_RATIO);
        }
        entryTableModel.setValueAt(UiUtil.formatScore(total), row, 6);
        entryTableModel.setValueAt(levelOf(total), row, 7);
    }

    /**
     * 重新计算表格全部数据行。
     */
    private void recalcAllRows() {
        for (int i = 0; i < entryTableModel.getRowCount(); i++) {
            recalcRow(i);
        }
    }

    // ------------------------------------------------------------------
    //  按钮事件
    // ------------------------------------------------------------------

    /**
     * 保存表格中的全部成绩。
     */
    private void saveAll() {
        stopEditing();
        Integer courseId = comboId(courseBox);
        if (courseId == null) {
            UiUtil.warn(this, "请先选择课程");
            return;
        }
        if (entryTableModel.getRowCount() == 0) {
            UiUtil.warn(this, "请先点击【加载学生名单】载入学生");
            return;
        }
        String semester = resolveSemester();
        if (semester == null || semester.isBlank()) {
            UiUtil.warn(this, "请先选择学期");
            return;
        }
        List<Object[]> rows = new ArrayList<>();
        for (int i = 0; i < entryTableModel.getRowCount(); i++) {
            Integer studentId = toInteger(entryTableModel.getValueAt(i, 0));
            if (studentId == null) {
                continue;
            }
            String name = String.valueOf(entryTableModel.getValueAt(i, 2));
            Object usualValue = entryTableModel.getValueAt(i, 3);
            Object examValue = entryTableModel.getValueAt(i, 4);
            Object makeupValue = entryTableModel.getValueAt(i, 5);
            if (isInvalidNumber(usualValue) || isInvalidNumber(examValue) || isInvalidNumber(makeupValue)) {
                UiUtil.warn(this, "第 " + (i + 1) + " 行【" + name + "】的成绩必须为数字");
                return;
            }
            Double usual = parseScore(usualValue);
            Double exam = parseScore(examValue);
            Double makeup = parseScore(makeupValue);
            if (!inRange(usual) || !inRange(exam) || !inRange(makeup)) {
                UiUtil.warn(this, "第 " + (i + 1) + " 行【" + name + "】的成绩必须在 0~100 之间");
                return;
            }
            if (usual == null && exam == null && makeup == null) {
                continue; // 未录入成绩的学生跳过，避免触发校验失败
            }
            Object remark = entryTableModel.getValueAt(i, 8);
            rows.add(new Object[]{studentId, usual, exam, makeup,
                    remark == null ? "" : String.valueOf(remark).trim()});
        }
        if (rows.isEmpty()) {
            UiUtil.warn(this, "请至少录入一名学生的成绩");
            return;
        }
        try {
            int count = scoreService.batchSave(semester, courseId, rows);
            UiUtil.success(this, "保存成功，共 " + count + " 条成绩记录");
            refreshData();
        } catch (BusinessException e) {
            UiUtil.warn(this, e.getMessage());
        } catch (RuntimeException e) {
            UiUtil.error(this, "保存失败：" + e.getMessage());
        }
    }

    /**
     * 一键把空白的期末成绩填充为及格线 60 分。
     */
    private void fillPassLine() {
        stopEditing();
        if (entryTableModel.getRowCount() == 0) {
            UiUtil.warn(this, "请先点击【加载学生名单】载入学生");
            return;
        }
        if (!confirm("确认把所有空白的期末成绩统一填充为 60 分？")) {
            return;
        }
        int filled = 0;
        for (int i = 0; i < entryTableModel.getRowCount(); i++) {
            Object value = entryTableModel.getValueAt(i, 4);
            if (value == null || String.valueOf(value).trim().isEmpty()) {
                entryTableModel.setValueAt(UiUtil.formatScore(Score.PASS_LINE), i, 4);
                filled++;
            }
        }
        recalcAllRows();
        UiUtil.info(this, "已填充 " + filled + " 条期末成绩，请核对后点击【保存全部】保存。");
    }

    /**
     * 从 Excel 导入成绩（列顺序：学号、平时成绩、期末成绩、补考成绩）。
     */
    private void importExcel() {
        Integer courseId = comboId(courseBox);
        if (courseId == null) {
            UiUtil.warn(this, "请先选择课程");
            return;
        }
        File file = UiUtil.chooseOpenFile(this, "Excel 工作簿 (*.xlsx)", "xlsx");
        if (file == null) {
            return;
        }
        try {
            List<String[]> rows = ExcelUtil.read(file, true);
            Object[] result = scoreService.importScores(courseId, resolveSemester(), rows);
            int success = toInt(result[0]);
            int failed = toInt(result[1]);
            String errors = result[2] == null ? "" : String.valueOf(result[2]);
            showImportResult(success, failed, errors);
            refreshData();
        } catch (IOException e) {
            UiUtil.error(this, "读取 Excel 文件失败：" + e.getMessage());
        } catch (BusinessException e) {
            UiUtil.warn(this, e.getMessage());
        } catch (RuntimeException e) {
            UiUtil.error(this, "导入失败：" + e.getMessage());
        }
    }

    /**
     * 展示导入结果（错误明细可能很长，放入可滚动文本域展示）。
     *
     * @param success 成功条数
     * @param failed  失败条数
     * @param errors  错误明细
     */
    private void showImportResult(int success, int failed, String errors) {
        StringBuilder message = new StringBuilder();
        message.append("导入完成：成功 ").append(success).append(" 条，失败 ").append(failed).append(" 条。");
        if (errors != null && !errors.isBlank()) {
            message.append("\n\n错误明细：\n").append(errors);
        }
        if (errors == null || errors.isBlank()) {
            UiUtil.success(this, message.toString());
            return;
        }
        JTextArea area = new JTextArea(message.toString());
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(UiTheme.pickChineseFont(12f));
        area.setCaretPosition(0);
        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(480, 280));
        JOptionPane.showMessageDialog(this, scroll, "导入结果", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * 按当前筛选条件导出成绩表。
     */
    private void exportExcel() {
        Integer courseId = comboId(courseBox);
        Integer classId = comboId(classBox);
        File file = UiUtil.chooseSaveExcel(this, "成绩表.xlsx");
        if (file == null) {
            return;
        }
        try {
            int count = reportService.exportScores(file, null, classId, courseId, resolveSemester());
            UiUtil.success(this, "导出成功，共 " + count + " 条记录\n文件位置：" + file.getAbsolutePath());
        } catch (RuntimeException e) {
            UiUtil.error(this, "导出失败：" + e.getMessage());
        }
    }

    /**
     * 刷新课程统计信息（平均分 / 最高分 / 最低分 / 及格率）。
     */
    private void updateCourseStat() {
        Integer courseId = comboId(courseBox);
        if (courseId == null) {
            statLabel.setText("课程统计：-");
            return;
        }
        try {
            Map<String, Object> stat = scoreService.courseStat(courseId);
            statLabel.setText("课程统计：平均分 " + statText(value(stat, "avg_score"))
                    + " / 最高 " + statText(value(stat, "max_score"))
                    + " / 最低 " + statText(value(stat, "min_score"))
                    + " / 及格率 " + statText(value(stat, "pass_rate")) + "%");
        } catch (RuntimeException e) {
            statLabel.setText("课程统计：读取失败");
        }
    }

    // ------------------------------------------------------------------
    //  通用工具方法
    // ------------------------------------------------------------------

    /**
     * 结束表格中正在进行的编辑，保证保存时取到最新输入值。
     */
    private void stopEditing() {
        if (table.isEditing() && table.getCellEditor() != null) {
            table.getCellEditor().stopCellEditing();
        }
    }

    /**
     * 解析当前学期：优先取下拉选择值，未选择时回退为课程所属学期。
     *
     * @return 学期，无法确定返回 null
     */
    private String resolveSemester() {
        Object value = semesterBox.getSelectedItem();
        String semester = value == null ? "" : String.valueOf(value).trim();
        if (!semester.isEmpty()) {
            return semester;
        }
        Course course = findCourse(comboId(courseBox));
        return course == null ? null : course.getSemester();
    }

    /**
     * 按主键查找已加载的课程。
     *
     * @param courseId 课程主键
     * @return 课程对象，不存在返回 null
     */
    private Course findCourse(Integer courseId) {
        if (courseId == null) {
            return null;
        }
        for (Course course : courseList) {
            if (courseId.equals(course.getId())) {
                return course;
            }
        }
        return null;
    }

    /**
     * 安全读取统计 Map 中的值。
     *
     * @param stat 统计 Map
     * @param key  键
     * @return 值，不存在返回 null
     */
    private static Object value(Map<String, Object> stat, String key) {
        return stat == null ? null : stat.get(key);
    }

    /**
     * 统计值格式化（null 显示为 "-"）。
     *
     * @param value 统计值
     * @return 文本
     */
    private static String statText(Object value) {
        if (value == null) {
            return "-";
        }
        if (value instanceof Number) {
            return UiUtil.formatScore((Number) value);
        }
        String text = String.valueOf(value);
        return text.isBlank() ? "-" : text;
    }

    /**
     * 解析单元格分数：空值与非数字均返回 null。
     *
     * @param value 单元格值
     * @return 分数，无法解析返回 null
     */
    private static Double parseScore(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return Double.valueOf(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 判断单元格是否为"非空但不是数字"的非法输入。
     *
     * @param value 单元格值
     * @return 非法返回 true
     */
    private static boolean isInvalidNumber(Object value) {
        if (value == null) {
            return false;
        }
        String text = String.valueOf(value).trim();
        return !text.isEmpty() && parseScore(value) == null;
    }

    /**
     * 判断分数是否落在 0~100 之间（null 视为合法，表示未填写）。
     *
     * @param score 分数
     * @return 合法返回 true
     */
    private static boolean inRange(Double score) {
        return score == null || (score >= 0D && score <= 100D);
    }

    /**
     * 四舍五入保留 1 位小数。
     *
     * @param value 原值
     * @return 结果
     */
    private static double round1(double value) {
        return Math.round(value * 10D) / 10D;
    }

    /**
     * 按总评成绩判定等级。
     *
     * @param total 总评成绩，可为 null
     * @return 等级文本
     */
    private static String levelOf(Double total) {
        if (total == null) {
            return "未录入";
        }
        if (total >= 90D) {
            return "优";
        }
        if (total >= 80D) {
            return "良";
        }
        if (total >= 70D) {
            return "中";
        }
        if (total >= Score.PASS_LINE) {
            return "及格";
        }
        return "不及格";
    }

    /**
     * 对象转整数主键。
     *
     * @param value 值
     * @return 整数，无法转换返回 null
     */
    private static Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.valueOf(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 对象转整数（用于适配导入结果的 Object 返回值）。
     *
     * @param value 值
     * @return 整数，无法转换返回 0
     */
    private static int toInt(Object value) {
        Integer result = toInteger(value);
        return result == null ? 0 : result;
    }

    /**
     * 成绩录入专用表格模型：平时 / 期末 / 补考成绩与备注列可编辑。
     */
    private static final class EntryTableModel extends DefaultTableModel {

        private static final long serialVersionUID = 1L;

        private EntryTableModel() {
            super(new Object[0][0], new String[]{"学生ID", "学号", "姓名", "平时成绩", "期末成绩",
                    "补考成绩", "总评成绩", "等级", "备注"});
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return column == 3 || column == 4 || column == 5 || column == 8;
        }
    }
}
