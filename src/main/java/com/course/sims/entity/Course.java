package com.course.sims.entity;

import java.sql.Timestamp;

/**
 * 课程实体，对应数据表 {@code course}。
 *
 * @author 课程设计第 2 小组 - 成员 C
 * @version 1.0.0
 */
public class Course {

    /** 课程类型常量 */
    public static final String TYPE_REQUIRED = "必修";
    public static final String TYPE_ELECTIVE = "选修";
    /** 考核方式常量 */
    public static final String ASSESS_EXAM = "考试";
    public static final String ASSESS_CHECK = "考查";

    /** 课程类型下拉数据 */
    public static final String[] ALL_TYPES = {TYPE_REQUIRED, TYPE_ELECTIVE};
    /** 考核方式下拉数据 */
    public static final String[] ALL_ASSESS = {ASSESS_EXAM, ASSESS_CHECK};

    private Integer id;
    private String courseCode;
    private String courseName;
    private Double credit;
    private Integer hours;
    private String courseType;
    private String assessType;
    private String teacher;
    private String semester;
    private Timestamp createTime;

    public Course() {
    }

    public Course(String courseCode, String courseName, Double credit) {
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.credit = credit;
        this.courseType = TYPE_REQUIRED;
        this.assessType = ASSESS_EXAM;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public Double getCredit() {
        return credit;
    }

    public void setCredit(Double credit) {
        this.credit = credit;
    }

    public Integer getHours() {
        return hours;
    }

    public void setHours(Integer hours) {
        this.hours = hours;
    }

    public String getCourseType() {
        return courseType;
    }

    public void setCourseType(String courseType) {
        this.courseType = courseType;
    }

    public String getAssessType() {
        return assessType;
    }

    public void setAssessType(String assessType) {
        this.assessType = assessType;
    }

    public String getTeacher() {
        return teacher;
    }

    public void setTeacher(String teacher) {
        this.teacher = teacher;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public Timestamp getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Timestamp createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return courseName + "(" + courseCode + ")";
    }
}
