package com.course.sims.ui.dialog;

import com.course.sims.config.UiTheme;
import com.course.sims.entity.Score;
import com.course.sims.entity.Student;
import com.course.sims.service.BusinessException;
import com.course.sims.service.ScoreService;
import com.course.sims.service.StudentService;
import com.course.sims.ui.common.BaseDialog;
import com.course.sims.ui.common.UiUtil;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Window;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 某课程整班成绩批量录入对话框（模块 M5）。
 *
 * <p>对话框内嵌入一张可编辑表格（学号、姓名由系统带出且不可编辑，
 * 平时成绩、期末成绩、补考成绩与备注可编辑），打开时自动载入班级学生名单
 * 并预填已有成绩，点击【保存】后通过
 * {@link ScoreService#batchSave(String, Integer, List)} 一次性提交整班成绩。</p>
 *
 * @author 课程设计第 2 小组 - 成员 C
 * @version 1.0.0
 */
public class BatchScoreDialog extends BaseDialog {

    private static final long serialVersionUID = 1L;

    private final ScoreService scoreService = new ScoreService();
    private final StudentService studentService = new StudentService();

    private final Integer courseId;
    private final String semester;
    private final Integer classId;

    /** 批量录入表格模型 */
    private final BatchTableModel tableModel = new BatchTableModel();
    private final JTable table = new JTable(tableModel);
    /** 学生名单，与表格行一一对应（表格不排序，行号即模型行号） */
    private final List<Student> studentList = new ArrayList<>();

    /**
     * 构造批量录入对话框。
     *
     * @param owner    父窗口
     * @param courseId 课程主键
     * @param semester 学期
     * @param classId  班级主键
     */
    public BatchScoreDialog(Window owner, Integer courseId, String semester, Integer classId) {
        super(owner, "批量录入成绩");
        this.courseId = courseId;
        this.semester = semester;
        this.classId = classId;
        initDialog();
        setMinimumSize(new Dimension(700, 420));
        setSize(760, 520);
        setLocationRelativeTo(owner);
    }

    @Override
    protected void buildForm() {
        table.setRowHeight(26);
        table.setFont(UiTheme.pickChineseFont(13f));
        table.getTableHeader().setFont(UiTheme.boldFont(13f));
        table.setFillsViewportHeight(true);
        table.setGridColor(new Color(0xE5E7EB));
        table.getTableHeader().setReorderingAllowed(false);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(700, 320));
        scroll.setBorder(BorderFactory.createLineBorder(new Color(0xE5E7EB)));
        scroll.getViewport().setBackground(Color.WHITE);

        JLabel hint = new JLabel("提示：只需填写需要录入的成绩，三项成绩均留空的行不会被保存；"
                + "总评成绩 = 平时成绩 × 30% + 期末成绩 × 70%，填写补考成绩时以补考成绩为准。");
        hint.setFont(UiTheme.pickChineseFont(12f));
        hint.setForeground(new Color(0x6B7280));

        JPanel wrapper = new JPanel(new BorderLayout(0, 6));
        wrapper.setOpaque(false);
        wrapper.add(hint, BorderLayout.NORTH);
        wrapper.add(scroll, BorderLayout.CENTER);
        addFullRow(wrapper);
    }

    @Override
    protected void loadDataToForm() {
        tableModel.setRowCount(0);
        studentList.clear();
        if (classId == null) {
            return;
        }
        try {
            Map<Integer, Score> scoreMap = new HashMap<>();
            if (courseId != null) {
                for (Score score : scoreService.findByCourse(courseId, classId)) {
                    if (score.getStudentId() != null) {
                        scoreMap.put(score.getStudentId(), score);
                    }
                }
            }
            for (Student student : studentService.findByClass(classId)) {
                studentList.add(student);
                Score exist = scoreMap.get(student.getId());
                if (exist == null) {
                    tableModel.addRow(new Object[]{student.getSno(), student.getName(), "", "", "", ""});
                } else {
                    tableModel.addRow(new Object[]{student.getSno(), student.getName(),
                            UiUtil.formatScore(exist.getUsualScore()), UiUtil.formatScore(exist.getExamScore()),
                            UiUtil.formatScore(exist.getMakeupScore()),
                            exist.getRemark() == null ? "" : exist.getRemark()});
                }
            }
        } catch (BusinessException e) {
            UiUtil.warn(this, e.getMessage());
        } catch (RuntimeException e) {
            UiUtil.error(this, "加载学生成绩失败：" + e.getMessage());
        }
    }

    @Override
    protected void onSave() {
        stopEditing();
        try {
            if (courseId == null) {
                UiUtil.warn(this, "缺少课程信息，无法保存成绩");
                return;
            }
            if (studentList.isEmpty()) {
                UiUtil.warn(this, "没有可保存的学生名单，请检查班级是否有学生");
                return;
            }
            List<Object[]> rows = new ArrayList<>();
            for (int i = 0; i < tableModel.getRowCount() && i < studentList.size(); i++) {
                Student student = studentList.get(i);
                Object usualValue = tableModel.getValueAt(i, 2);
                Object examValue = tableModel.getValueAt(i, 3);
                Object makeupValue = tableModel.getValueAt(i, 4);
                if (isInvalidNumber(usualValue) || isInvalidNumber(examValue) || isInvalidNumber(makeupValue)) {
                    UiUtil.warn(this, "第 " + (i + 1) + " 行【" + student.getName() + "】的成绩必须为数字");
                    return;
                }
                Double usual = parseScore(usualValue);
                Double exam = parseScore(examValue);
                Double makeup = parseScore(makeupValue);
                if (!inRange(usual) || !inRange(exam) || !inRange(makeup)) {
                    UiUtil.warn(this, "第 " + (i + 1) + " 行【" + student.getName() + "】的成绩必须在 0~100 之间");
                    return;
                }
                if (usual == null && exam == null && makeup == null) {
                    continue; // 未录入成绩的学生跳过，避免触发业务校验失败
                }
                Object remark = tableModel.getValueAt(i, 5);
                rows.add(new Object[]{student.getId(), usual, exam, makeup,
                        remark == null ? "" : String.valueOf(remark).trim()});
            }
            if (rows.isEmpty()) {
                UiUtil.warn(this, "请至少录入一名学生的成绩");
                return;
            }
            int count = scoreService.batchSave(semester, courseId, rows);
            UiUtil.success(this, "批量保存成功，共 " + count + " 条成绩记录");
            markSavedAndClose();
        } catch (BusinessException e) {
            UiUtil.warn(this, e.getMessage());
        } catch (RuntimeException e) {
            UiUtil.error(this, "保存失败：" + e.getMessage());
        }
    }

    /**
     * 结束表格中正在进行的编辑，保证保存时取到最新输入值。
     */
    private void stopEditing() {
        if (table.isEditing() && table.getCellEditor() != null) {
            table.getCellEditor().stopCellEditing();
        }
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
     * 批量录入专用表格模型：学号与姓名只读，其余列可编辑。
     */
    private static final class BatchTableModel extends DefaultTableModel {

        private static final long serialVersionUID = 1L;

        private BatchTableModel() {
            super(new Object[0][0], new String[]{"学号", "姓名", "平时成绩", "期末成绩", "补考成绩", "备注"});
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return column >= 2;
        }
    }
}
