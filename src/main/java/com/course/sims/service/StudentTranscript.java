package com.course.sims.service;

import com.course.sims.entity.Score;
import com.course.sims.entity.Student;

import java.util.ArrayList;
import java.util.List;

/**
 * 学生成绩单（数据传输对象）。
 *
 * <p>把散落在 score 表中的课程成绩、学分、绩点汇总为一张可直接展示或导出的成绩单。</p>
 *
 * @author 课程设计第 2 小组 - 成员 E
 * @version 1.0.0
 */
public class StudentTranscript {

    /** 学生基本信息 */
    private Student student;
    /** 成绩明细 */
    private final List<Score> scores = new ArrayList<>();
    /** 已修学分（及格课程学分之和） */
    private double earnedCredit;
    /** 加权平均分 */
    private double weightedAverage;
    /** 平均绩点 */
    private double gpa;
    /** 不及格门数 */
    private int failedCount;

    public StudentTranscript() {
    }

    public StudentTranscript(Student student) {
        this.student = student;
    }

    /**
     * 添加一条成绩并同步累加统计量。
     *
     * @param score 成绩记录
     */
    public void addScore(Score score) {
        scores.add(score);
        if (score.isPassed() && score.getCredit() != null) {
            earnedCredit += score.getCredit();
        }
    }

    /** @return 学生信息 */
    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    /** @return 成绩明细 */
    public List<Score> getScores() {
        return scores;
    }

    /** @return 已修学分 */
    public double getEarnedCredit() {
        return earnedCredit;
    }

    public void setEarnedCredit(double earnedCredit) {
        this.earnedCredit = earnedCredit;
    }

    /** @return 加权平均分 */
    public double getWeightedAverage() {
        return weightedAverage;
    }

    public void setWeightedAverage(double weightedAverage) {
        this.weightedAverage = weightedAverage;
    }

    /** @return 平均绩点 */
    public double getGpa() {
        return gpa;
    }

    public void setGpa(double gpa) {
        this.gpa = gpa;
    }

    /** @return 不及格门数 */
    public int getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(int failedCount) {
        this.failedCount = failedCount;
    }

    /**
     * 获取总学分绩点（GPA 计算明细，用于文档说明）。
     *
     * @return 加权平均分与已修学分
     */
    public String summary() {
        return String.format("已修学分 %.1f，加权平均分 %.1f，平均绩点 %.2f，不及格 %d 门",
                earnedCredit, weightedAverage, gpa, failedCount);
    }
}
