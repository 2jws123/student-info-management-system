package com.course.sims.entity;

import java.sql.Timestamp;

/**
 * 专业实体，对应数据表 {@code major}。
 *
 * @author 课程设计第 2 小组 - 成员 B
 * @version 1.0.0
 */
public class Major {

    private Integer id;
    private String majorCode;
    private String majorName;
    private String deptName;
    private Integer duration;
    private String remark;
    private Timestamp createTime;

    /** 非数据库字段：该专业下的班级数量（统计展示用） */
    private int classCount;

    public Major() {
    }

    public Major(String majorCode, String majorName, String deptName) {
        this.majorCode = majorCode;
        this.majorName = majorName;
        this.deptName = deptName;
        this.duration = 4;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getMajorCode() {
        return majorCode;
    }

    public void setMajorCode(String majorCode) {
        this.majorCode = majorCode;
    }

    public String getMajorName() {
        return majorName;
    }

    public void setMajorName(String majorName) {
        this.majorName = majorName;
    }

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
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

    public int getClassCount() {
        return classCount;
    }

    public void setClassCount(int classCount) {
        this.classCount = classCount;
    }

    @Override
    public String toString() {
        return majorName;
    }
}
