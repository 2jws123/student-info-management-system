package com.course.sims.entity;

import java.sql.Date;
import java.sql.Timestamp;

/**
 * 学生实体，对应数据表 {@code student}。
 *
 * @author 课程设计第 2 小组 - 成员 B
 * @version 1.0.0
 */
public class Student {

    /** 学籍状态常量 */
    public static final String STATUS_STUDYING = "在读";
    public static final String STATUS_SUSPENDED = "休学";
    public static final String STATUS_QUIT = "退学";
    public static final String STATUS_GRADUATED = "毕业";

    /** 全部学籍状态，供下拉框使用 */
    public static final String[] ALL_STATUS = {STATUS_STUDYING, STATUS_SUSPENDED, STATUS_QUIT, STATUS_GRADUATED};

    private Integer id;
    private String sno;
    private String name;
    private String gender;
    private Date birthday;
    private Integer classId;
    private String phone;
    private String email;
    private String address;
    private Integer enrollYear;
    private String status;
    private Timestamp createTime;

    /** 非数据库字段：班级名称 */
    private String className;
    /** 非数据库字段：班级代码 */
    private String classCode;
    /** 非数据库字段：所属专业名称 */
    private String majorName;
    /** 非数据库字段：年级 */
    private Integer grade;

    public Student() {
    }

    public Student(String sno, String name, String gender, Integer classId) {
        this.sno = sno;
        this.name = name;
        this.gender = gender;
        this.classId = classId;
        this.status = STATUS_STUDYING;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getSno() {
        return sno;
    }

    public void setSno(String sno) {
        this.sno = sno;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public Date getBirthday() {
        return birthday;
    }

    public void setBirthday(Date birthday) {
        this.birthday = birthday;
    }

    public Integer getClassId() {
        return classId;
    }

    public void setClassId(Integer classId) {
        this.classId = classId;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Integer getEnrollYear() {
        return enrollYear;
    }

    public void setEnrollYear(Integer enrollYear) {
        this.enrollYear = enrollYear;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Timestamp createTime) {
        this.createTime = createTime;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getClassCode() {
        return classCode;
    }

    public void setClassCode(String classCode) {
        this.classCode = classCode;
    }

    public String getMajorName() {
        return majorName;
    }

    public void setMajorName(String majorName) {
        this.majorName = majorName;
    }

    public Integer getGrade() {
        return grade;
    }

    public void setGrade(Integer grade) {
        this.grade = grade;
    }

    @Override
    public String toString() {
        return sno + " " + name;
    }
}
