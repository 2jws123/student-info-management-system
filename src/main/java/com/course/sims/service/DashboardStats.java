package com.course.sims.service;

import java.util.ArrayList;
import java.util.List;

/**
 * 首页数据看板统计结果（数据传输对象）。
 *
 * @author 课程设计第 2 小组 - 成员 E
 * @version 1.0.0
 */
public class DashboardStats {

    /** 学生总数 */
    private int studentTotal;
    /** 在读学生数 */
    private int studyingTotal;
    /** 班级总数 */
    private int classTotal;
    /** 专业总数 */
    private int majorTotal;
    /** 课程总数 */
    private int courseTotal;
    /** 系统用户数 */
    private int userTotal;
    /** 成绩记录数 */
    private int scoreTotal;
    /** 考勤记录数 */
    private int attendanceTotal;
    /** 本学期平均分 */
    private double avgScore;
    /** 及格率（%） */
    private double passRate;
    /** 优秀（90 分以上）记录数 */
    private int excellentCount;
    /** 不及格记录数 */
    private int failedCount;
    /** 考勤预警人数 */
    private int attendanceWarningCount;
    /** 班级平均分排名（班级名 / 平均分） */
    private final List<String[]> classRank = new ArrayList<>();
    /** 成绩五档分布（等级名 / 人数） */
    private final List<String[]> scoreDistribution = new ArrayList<>();

    public int getStudentTotal() {
        return studentTotal;
    }

    public void setStudentTotal(int studentTotal) {
        this.studentTotal = studentTotal;
    }

    public int getStudyingTotal() {
        return studyingTotal;
    }

    public void setStudyingTotal(int studyingTotal) {
        this.studyingTotal = studyingTotal;
    }

    public int getClassTotal() {
        return classTotal;
    }

    public void setClassTotal(int classTotal) {
        this.classTotal = classTotal;
    }

    public int getMajorTotal() {
        return majorTotal;
    }

    public void setMajorTotal(int majorTotal) {
        this.majorTotal = majorTotal;
    }

    public int getCourseTotal() {
        return courseTotal;
    }

    public void setCourseTotal(int courseTotal) {
        this.courseTotal = courseTotal;
    }

    public int getUserTotal() {
        return userTotal;
    }

    public void setUserTotal(int userTotal) {
        this.userTotal = userTotal;
    }

    public int getScoreTotal() {
        return scoreTotal;
    }

    public void setScoreTotal(int scoreTotal) {
        this.scoreTotal = scoreTotal;
    }

    public int getAttendanceTotal() {
        return attendanceTotal;
    }

    public void setAttendanceTotal(int attendanceTotal) {
        this.attendanceTotal = attendanceTotal;
    }

    public double getAvgScore() {
        return avgScore;
    }

    public void setAvgScore(double avgScore) {
        this.avgScore = avgScore;
    }

    public double getPassRate() {
        return passRate;
    }

    public void setPassRate(double passRate) {
        this.passRate = passRate;
    }

    public int getExcellentCount() {
        return excellentCount;
    }

    public void setExcellentCount(int excellentCount) {
        this.excellentCount = excellentCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(int failedCount) {
        this.failedCount = failedCount;
    }

    public int getAttendanceWarningCount() {
        return attendanceWarningCount;
    }

    public void setAttendanceWarningCount(int attendanceWarningCount) {
        this.attendanceWarningCount = attendanceWarningCount;
    }

    public List<String[]> getClassRank() {
        return classRank;
    }

    public List<String[]> getScoreDistribution() {
        return scoreDistribution;
    }
}
