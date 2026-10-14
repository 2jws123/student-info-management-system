package com.course.sims.service;

import com.course.sims.entity.Attendance;
import com.course.sims.entity.Score;
import com.course.sims.entity.Student;
import com.course.sims.util.ExcelUtil;

import java.io.File;
import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 数据导出与报表业务逻辑（模块 M8）。
 *
 * <p>把界面上的查询条件透传到 SQL 层，使"导出的内容 = 界面上看到的内容"，
 * 避免出现导出全表而界面只显示筛选结果的错位问题。</p>
 *
 * @author 课程设计第 2 小组 - 成员 E
 * @version 1.0.0
 */
public class ReportService {

    private final StudentService studentService = new StudentService();
    private final ScoreService scoreService = new ScoreService();
    private final AttendanceService attendanceService = new AttendanceService();
    private final StatsService statsService = new StatsService();

    /**
     * 导出学生名册。
     *
     * @param file     目标文件
     * @param keyword  关键字（与界面一致）
     * @param classId  班级过滤
     * @param status   学籍状态过滤
     * @return 导出的记录数
     * @throws BusinessException 导出失败
     */
    public int exportStudents(File file, String keyword, Integer classId, String status) {
        List<Student> list = studentService.findList(keyword, classId, status);
        String[] headers = {"学号", "姓名", "性别", "出生日期", "班级", "专业", "年级", "手机号", "邮箱",
                "家庭住址", "入学年份", "学籍状态"};
        List<Object[]> rows = new ArrayList<>();
        for (Student s : list) {
            rows.add(new Object[]{s.getSno(), s.getName(), s.getGender(),
                    s.getBirthday() == null ? "" : s.getBirthday().toString(),
                    s.getClassName(), s.getMajorName(), s.getGrade(), s.getPhone(), s.getEmail(),
                    s.getAddress(), s.getEnrollYear(), s.getStatus()});
        }
        try {
            ExcelUtil.export(file, "学生名册", "学生信息管理系统 - 学生名册（共 " + rows.size() + " 人）",
                    headers, rows);
        } catch (IOException e) {
            throw new BusinessException("ERR-601", "导出学生名册失败：" + e.getMessage());
        }
        AuthService.log("导出学生名册", file.getName() + "，共 " + rows.size() + " 条");
        return rows.size();
    }

    /**
     * 导出成绩表。
     *
     * @param file     目标文件
     * @param keyword  关键字
     * @param classId  班级过滤
     * @param courseId 课程过滤
     * @param semester 学期过滤
     * @return 导出的记录数
     * @throws BusinessException 导出失败
     */
    public int exportScores(File file, String keyword, Integer classId, Integer courseId, String semester) {
        List<Score> list = scoreService.findList(keyword, classId, courseId, semester);
        String[] headers = {"学号", "姓名", "班级", "课程代码", "课程名称", "课程类型", "学分", "学期",
                "平时成绩", "期末成绩", "补考成绩", "总评成绩", "绩点", "等级", "是否重修"};
        List<Object[]> rows = new ArrayList<>();
        for (Score s : list) {
            rows.add(new Object[]{s.getSno(), s.getStudentName(), s.getClassName(), s.getCourseCode(),
                    s.getCourseName(), s.getCourseType(), s.getCredit(), s.getSemester(), s.getUsualScore(),
                    s.getExamScore(), s.getMakeupScore(), s.getTotalScore(), s.getGradePoint(),
                    s.getScoreLevel(), Boolean.TRUE.equals(s.getIsRetake()) ? "是" : "否"});
        }
        try {
            ExcelUtil.export(file, "成绩表", "学生信息管理系统 - 成绩表（共 " + rows.size() + " 条）", headers, rows);
        } catch (IOException e) {
            throw new BusinessException("ERR-602", "导出成绩表失败：" + e.getMessage());
        }
        AuthService.log("导出成绩表", file.getName() + "，共 " + rows.size() + " 条");
        return rows.size();
    }

    /**
     * 导出单个学生的成绩单。
     *
     * @param file      目标文件
     * @param studentId 学生主键
     * @param semester  学期过滤
     * @return 导出的课程数
     * @throws BusinessException 导出失败
     */
    public int exportTranscript(File file, Integer studentId, String semester) {
        StudentTranscript transcript = scoreService.transcript(studentId, semester);
        Student student = transcript.getStudent();
        String[] headers = {"学期", "课程代码", "课程名称", "课程类型", "学分", "平时成绩", "期末成绩",
                "补考成绩", "总评成绩", "绩点", "等级"};
        List<Object[]> rows = new ArrayList<>();
        for (Score s : transcript.getScores()) {
            rows.add(new Object[]{s.getSemester(), s.getCourseCode(), s.getCourseName(), s.getCourseType(),
                    s.getCredit(), s.getUsualScore(), s.getExamScore(), s.getMakeupScore(), s.getTotalScore(),
                    s.getGradePoint(), s.getScoreLevel()});
        }
        String title = "学生成绩单 - " + student.getSno() + " " + student.getName()
                + "（" + student.getClassName() + "）  " + transcript.summary();
        try {
            ExcelUtil.export(file, "成绩单", title, headers, rows);
        } catch (IOException e) {
            throw new BusinessException("ERR-603", "导出成绩单失败：" + e.getMessage());
        }
        AuthService.log("导出成绩单", student.getSno() + " " + student.getName());
        return rows.size();
    }

    /**
     * 导出考勤记录表。
     *
     * @param file     目标文件
     * @param classId  班级过滤
     * @param courseId 课程过滤
     * @param startDay 起始日期
     * @param endDay   截止日期
     * @return 导出的记录数
     * @throws BusinessException 导出失败
     */
    public int exportAttendance(File file, Integer classId, Integer courseId, Date startDay, Date endDay) {
        List<Attendance> list = attendanceService.findList(classId, courseId, startDay, endDay);
        String[] headers = {"日期", "学号", "姓名", "课程", "考勤状态", "备注"};
        List<Object[]> rows = new ArrayList<>();
        for (Attendance a : list) {
            rows.add(new Object[]{a.getAttDate() == null ? "" : a.getAttDate().toString(), a.getSno(),
                    a.getStudentName(), a.getCourseName(), a.getStatus(), a.getRemark()});
        }
        try {
            ExcelUtil.export(file, "考勤记录", "学生信息管理系统 - 考勤记录表（共 " + rows.size() + " 条）", headers, rows);
        } catch (IOException e) {
            throw new BusinessException("ERR-604", "导出考勤表失败：" + e.getMessage());
        }
        AuthService.log("导出考勤表", file.getName() + "，共 " + rows.size() + " 条");
        return rows.size();
    }

    /**
     * 导出综合统计报表（含班级平均分排名、课程统计、成绩分布、专业人数四个工作表内容）。
     *
     * @param file 目标文件
     * @return 导出的指标行数
     * @throws BusinessException 导出失败
     */
    public int exportStatistics(File file) {
        Object[] overview = statsService.overviewArray();
        String[] headers = {"指标名称", "指标数值"};
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{"学生总数", overview[0]});
        rows.add(new Object[]{"班级总数", overview[1]});
        rows.add(new Object[]{"课程总数", overview[2]});
        rows.add(new Object[]{"教师人数", overview[3]});
        rows.add(new Object[]{"平均分", overview[4]});
        rows.add(new Object[]{"及格率(%)", overview[5]});
        rows.add(new Object[]{"导出时间", LocalDate.now().toString()});

        List<Object[]> classRank = new ArrayList<>();
        for (Map<String, Object> m : statsService.classAverageRank(null)) {
            classRank.add(new Object[]{m.get("class_name"), m.get("student_count"), m.get("avg_score"),
                    m.get("pass_rate")});
        }
        List<Object[]> courseStat = new ArrayList<>();
        for (Map<String, Object> m : statsService.courseAverageStat(null)) {
            courseStat.add(new Object[]{m.get("course_name"), m.get("avg_score"), m.get("max_score"),
                    m.get("min_score"), m.get("pass_rate")});
        }
        List<Object[]> distribution = new ArrayList<>();
        for (Map<String, Object> m : statsService.scoreDistribution(null)) {
            distribution.add(new Object[]{m.get("level_name"), m.get("level_count")});
        }
        List<Object[]> majorStat = new ArrayList<>();
        for (Map<String, Object> m : statsService.majorDistribution()) {
            majorStat.add(new Object[]{m.get("major_name"), m.get("student_count")});
        }

        try {
            com.course.sims.util.MultiSheetExcel.export(file, "统计分析报表",
                    new String[]{"综合概览", "班级平均分排名", "课程成绩统计", "成绩等级分布", "专业人数分布"},
                    new String[][]{
                            headers,
                            {"班级名称", "人数", "平均分", "及格率(%)"},
                            {"课程名称", "平均分", "最高分", "最低分", "及格率(%)"},
                            {"成绩等级", "人数"},
                            {"专业名称", "人数"}
                    },
                    new List[]{rows, classRank, courseStat, distribution, majorStat});
        } catch (IOException e) {
            throw new BusinessException("ERR-605", "导出统计报表失败：" + e.getMessage());
        }
        AuthService.log("导出统计报表", file.getName());
        return rows.size();
    }

    /**
     * 导出首页看板汇总（单表）。
     *
     * @param file 目标文件
     * @return 导出行数
     * @throws BusinessException 导出失败
     */
    public int exportDashboard(File file) {
        DashboardStats stats = statsService.dashboard();
        String[] headers = {"统计项", "数值"};
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{"学生总数", stats.getStudentTotal()});
        rows.add(new Object[]{"在读学生", stats.getStudyingTotal()});
        rows.add(new Object[]{"班级总数", stats.getClassTotal()});
        rows.add(new Object[]{"专业总数", stats.getMajorTotal()});
        rows.add(new Object[]{"课程总数", stats.getCourseTotal()});
        rows.add(new Object[]{"成绩记录数", stats.getScoreTotal()});
        rows.add(new Object[]{"考勤记录数", stats.getAttendanceTotal()});
        rows.add(new Object[]{"平均分", stats.getAvgScore()});
        rows.add(new Object[]{"及格率(%)", stats.getPassRate()});
        rows.add(new Object[]{"优秀记录数", stats.getExcellentCount()});
        rows.add(new Object[]{"不及格记录数", stats.getFailedCount()});
        rows.add(new Object[]{"考勤预警人数", stats.getAttendanceWarningCount()});
        try {
            ExcelUtil.export(file, "数据看板", "学生信息管理系统 - 数据看板汇总", headers, rows);
        } catch (IOException e) {
            throw new BusinessException("ERR-606", "导出看板失败：" + e.getMessage());
        }
        AuthService.log("导出数据看板", file.getName());
        return rows.size();
    }
}
