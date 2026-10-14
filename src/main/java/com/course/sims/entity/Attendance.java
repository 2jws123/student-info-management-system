package com.course.sims.entity;

import java.sql.Date;
import java.sql.Timestamp;

/**
 * 考勤实体，对应数据表 {@code attendance}。
 *
 * @author 课程设计第 2 小组 - 成员 C
 * @version 1.0.0
 */
public class Attendance {

    /** 考勤状态常量 */
    public static final String PRESENT = "出勤";
    public static final String LATE = "迟到";
    public static final String EARLY_LEAVE = "早退";
    public static final String SICK_LEAVE = "病假";
    public static final String PERSONAL_LEAVE = "事假";
    public static final String ABSENT = "缺勤";

    /** 全部考勤状态，供下拉框使用 */
    public static final String[] ALL_STATUS = {PRESENT, LATE, EARLY_LEAVE, SICK_LEAVE, PERSONAL_LEAVE, ABSENT};

    private Integer id;
    private Integer studentId;
    private Integer courseId;
    private Date attDate;
    private String status;
    private String remark;
    private Timestamp createTime;

    /** 非数据库字段：学号 */
    private String sno;
    /** 非数据库字段：学生姓名 */
    private String studentName;
    /** 非数据库字段：课程名称 */
    private String courseName;

    public Attendance() {
    }

    public Attendance(Integer studentId, Integer courseId, Date attDate, String status) {
        this.studentId = studentId;
        this.courseId = courseId;
        this.attDate = attDate;
        this.status = status;
    }

    /**
     * 判断本次考勤是否为"未正常出勤"（迟到、早退、请假、缺勤均计入出勤率扣减）。
     *
     * @return 非出勤返回 true
     */
    public boolean isAbnormal() {
        return status != null && !PRESENT.equals(status);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getStudentId() {
        return studentId;
    }

    public void setStudentId(Integer studentId) {
        this.studentId = studentId;
    }

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    public Date getAttDate() {
        return attDate;
    }

    public void setAttDate(Date attDate) {
        this.attDate = attDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Timestamp getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Timestamp createTime) {
        this.createTime = createTime;
    }

    public String getSno() {
        return sno;
    }

    public void setSno(String sno) {
        this.sno = sno;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    @Override
    public String toString() {
        return (studentName == null ? "" : studentName) + " " + attDate + " " + status;
    }
}
