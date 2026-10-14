package com.course.sims.entity;

import java.sql.Timestamp;

/**
 * 班级实体，对应数据表 {@code clazz}。
 * <p>Class 为 Java 语言保留字，故实体与表名均采用 clazz 命名。</p>
 *
 * @author 课程设计第 2 小组 - 成员 B
 * @version 1.0.0
 */
public class Clazz {

    private Integer id;
    private String classCode;
    private String className;
    private Integer majorId;
    private Integer grade;
    private String headteacher;
    private Integer capacity;
    private Timestamp createTime;

    /** 非数据库字段：所属专业名称（联表查询展示用） */
    private String majorName;

    /** 非数据库字段：实际学生人数（统计展示用） */
    private int studentCount;

    public Clazz() {
    }

    public Clazz(String classCode, String className, Integer majorId, Integer grade) {
        this.classCode = classCode;
        this.className = className;
        this.majorId = majorId;
        this.grade = grade;
        this.capacity = 50;
    }

    /**
     * 判断班级是否已满。
     *
     * @return 实际人数达到或超过容量返回 true
     */
    public boolean isFull() {
        return capacity != null && studentCount >= capacity;
    }

    /**
     * 计算剩余容量。
     *
     * @return 剩余可容纳人数，不小于 0
     */
    public int getRemainCapacity() {
        if (capacity == null) {
            return 0;
        }
        return Math.max(capacity - studentCount, 0);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getClassCode() {
        return classCode;
    }

    public void setClassCode(String classCode) {
        this.classCode = classCode;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public Integer getMajorId() {
        return majorId;
    }

    public void setMajorId(Integer majorId) {
        this.majorId = majorId;
    }

    public Integer getGrade() {
        return grade;
    }

    public void setGrade(Integer grade) {
        this.grade = grade;
    }

    public String getHeadteacher() {
        return headteacher;
    }

    public void setHeadteacher(String headteacher) {
        this.headteacher = headteacher;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Timestamp getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Timestamp createTime) {
        this.createTime = createTime;
    }

    public String getMajorName() {
        return majorName;
    }

    public void setMajorName(String majorName) {
        this.majorName = majorName;
    }

    public int getStudentCount() {
        return studentCount;
    }

    public void setStudentCount(int studentCount) {
        this.studentCount = studentCount;
    }

    @Override
    public String toString() {
        return className;
    }
}
