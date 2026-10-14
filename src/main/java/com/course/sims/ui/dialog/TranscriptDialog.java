package com.course.sims.ui.dialog;

import com.course.sims.config.UiTheme;
import com.course.sims.entity.Score;
import com.course.sims.entity.Student;
import com.course.sims.service.BusinessException;
import com.course.sims.service.ReportService;
import com.course.sims.service.ScoreService;
import com.course.sims.service.StudentTranscript;
import com.course.sims.service.TeachingService;
import com.course.sims.ui.common.UiUtil;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.io.File;
import java.util.List;

/**
 * 学生成绩单对话框（模块 M2 + M7 的组合功能）。
 *
 * <p>展示学生基本信息、全部课程成绩、已修学分、加权平均分与平均绩点，
 * 并支持按学期筛选与导出 Excel 成绩单。</p>
 *
 * @author 课程设计第 2 小组 - 成员 E
 * @version 1.0.0
 */
public class TranscriptDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private final ScoreService scoreService = new ScoreService();
    private final ReportService reportService = new ReportService();

    private final Integer studentId;
    private final DefaultTableModel tableModel = new DefaultTableModel() {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);
    private final JLabel summaryLabel = new JLabel(" ");
    private final JComboBox<Object> semesterBox = new JComboBox<>();

    /**
     * 构造成绩单对话框。
     *
     * @param owner     父窗口
     * @param studentId 学生主键
     */
    public TranscriptDialog(Window owner, Integer studentId) {
        super(owner, "学生成绩单", ModalityType.APPLICATION_MODAL);
        this.studentId = studentId;
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE);
        setSize(860, 560);
        setLocationRelativeTo(owner);
        buildUi();
        loadData();
    }

    /**
     * 装配界面。
     */
    private void buildUi() {
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(Color.WHITE);
        top.setBorder(BorderFactory.createEmptyBorder(12, 16, 6, 16));

        JLabel title = new JLabel("学生成绩单");
        title.setFont(UiTheme.boldFont(17f));
        top.add(title, BorderLayout.WEST);

        JPanel filter = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        filter.setOpaque(false);
        filter.add(UiUtil.formLabel("学期"));
        semesterBox.setFont(UiTheme.pickChineseFont(13f));
        semesterBox.addItem("全部");
        for (String semester : new TeachingService().findAllSemesters()) {
            semesterBox.addItem(semester);
        }
        semesterBox.addActionListener(e -> loadData());
        filter.add(semesterBox);
        top.add(filter, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        tableModel.setColumnIdentifiers(new String[]{"学期", "课程代码", "课程名称", "类型", "学分",
                "平时成绩", "期末成绩", "补考成绩", "总评成绩", "绩点", "等级"});
        UiUtil.styleTable(table);
        table.setRowHeight(28);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));
        add(scroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(Color.WHITE);
        bottom.setBorder(BorderFactory.createEmptyBorder(8, 16, 12, 16));
        summaryLabel.setFont(UiTheme.boldFont(13f));
        summaryLabel.setForeground(new Color(0x1F2937));
        bottom.add(summaryLabel, BorderLayout.WEST);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        JButton export = UiUtil.button("导出 Excel", UiTheme.SUCCESS);
        export.addActionListener(e -> exportExcel());
        JButton close = UiUtil.button("关闭", new Color(0x6B7280));
        close.addActionListener(e -> dispose());
        buttons.add(export);
        buttons.add(close);
        bottom.add(buttons, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);
    }

    /**
     * 加载成绩单数据。
     */
    private void loadData() {
        try {
            String semester = semesterBox.getSelectedItem() == null
                    || "全部".equals(semesterBox.getSelectedItem())
                    ? null : String.valueOf(semesterBox.getSelectedItem());
            StudentTranscript transcript = scoreService.transcript(studentId, semester);
            Student student = transcript.getStudent();
            tableModel.setRowCount(0);
            for (Score s : transcript.getScores()) {
                tableModel.addRow(new Object[]{s.getSemester(), s.getCourseCode(), s.getCourseName(),
                        s.getCourseType(), s.getCredit(), UiUtil.formatScore(s.getUsualScore()),
                        UiUtil.formatScore(s.getExamScore()), UiUtil.formatScore(s.getMakeupScore()),
                        UiUtil.formatScore(s.getTotalScore()), UiUtil.format2(s.getGradePoint()),
                        s.getScoreLevel()});
            }
            setTitle("学生成绩单 - " + student.getSno() + " " + student.getName());
            summaryLabel.setText("学生：" + student.getSno() + " " + student.getName()
                    + "（" + student.getClassName() + "）　" + transcript.summary());
        } catch (BusinessException e) {
            UiUtil.warn(this, e.getMessage());
        } catch (RuntimeException e) {
            UiUtil.error(this, "加载成绩单失败：" + e.getMessage());
        }
    }

    /**
     * 导出成绩单。
     */
    private void exportExcel() {
        File file = UiUtil.chooseSaveExcel(this, "成绩单.xlsx");
        if (file == null) {
            return;
        }
        try {
            String semester = semesterBox.getSelectedItem() == null
                    || "全部".equals(semesterBox.getSelectedItem())
                    ? null : String.valueOf(semesterBox.getSelectedItem());
            int count = reportService.exportTranscript(file, studentId, semester);
            UiUtil.success(this, "导出成功，共 " + count + " 门课程\n文件位置：" + file.getAbsolutePath());
        } catch (RuntimeException e) {
            UiUtil.error(this, "导出失败：" + e.getMessage());
        }
    }
}
