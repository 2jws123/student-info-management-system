package com.course.sims.entity;

import java.sql.Timestamp;

/**
 * 成绩实体，对应数据表 {@code score}。
 *
 * <p>总评成绩计算规则（本类与数据库计算列保持一致）：</p>
 * <ul>
 *   <li>存在补考成绩时，总评成绩 = 补考成绩；</li>
 *   <li>否则总评成绩 = 平时成绩 × 30% + 期末成绩 × 70%，保留 1 位小数。</li>
 * </ul>
 *
 * @author 课程设计第 2 小组 - 成员 C
 * @version 1.0.0
 */
public class Score {

    /** 及格分数线（默认 60，可由系统参数覆盖） */
    public static final double PASS_LINE = 60.0;

    private Integer id;
    private Integer studentId;
    private Integer courseId;
    private String semester;
    private Double usualScore;
    private Double examScore;
    private Double makeupScore;
    private Double totalScore;
    private Boolean isRetake;
    private String remark;
    private Timestamp createTime;

    /** 非数据库字段：学号 */
    private String sno;
    /** 非数据库字段：学生姓名 */
    private String studentName;
    /** 非数据库字段：班级名称 */
    private String className;
    /** 非数据库字段：课程名称 */
    private String courseName;
    /** 非数据库字段：课程代码 */
    private String courseCode;
    /** 非数据库字段：学分 */
    private Double credit;
    /** 非数据库字段：课程类型 */
    private String courseType;

    public Score() {
    }

    public Score(Integer studentId, Integer courseId, String semester) {
        this.studentId = studentId;
        this.courseId = courseId;
        this.semester = semester;
        this.isRetake = Boolean.FALSE;
    }

    /**
     * 按规则计算总评成绩（不依赖数据库计算列，供业务层校验与单元测试使用）。
     *
     * @return 总评成绩；平时与期末成绩均缺失时返回 null
     */
    public Double calcTotal() {
        if (makeupScore != null) {
            return round1(makeupScore);
        }
        if (usualScore == null && examScore == null) {
            return null;
        }
        double usual = usualScore == null ? 0D : usualScore;
        double exam = examScore == null ? 0D : examScore;
        return round1(usual * 0.3 + exam * 0.7);
    }

    /**
     * 计算绩点（四等五级制，4.0 制）。
     * <p>90 分及以上 4.0；80~89 为 3.0；70~79 为 2.0；60~69 为 1.0；60 以下 0。</p>
     *
     * @return 绩点，无总评成绩返回 0
     */
    public double getGradePoint() {
        Double total = totalScore != null ? totalScore : calcTotal();
        if (total == null) {
            return 0D;
        }
        if (total >= 90) {
            return 4.0D;
        }
        if (total >= 80) {
            return 3.0D;
        }
        if (total >= 70) {
            return 2.0D;
        }
        if (total >= PASS_LINE) {
            return 1.0D;
        }
        return 0D;
    }

    /**
     * 获取成绩等级描述。
     *
     * @return 优 / 良 / 中 / 及格 / 不及格 / 未录入
     */
    public String getScoreLevel() {
        Double total = totalScore != null ? totalScore : calcTotal();
        if (total == null) {
            return "未录入";
        }
        if (total >= 90) {
            return "优";
        }
        if (total >= 80) {
            return "良";
        }
        if (total >= 70) {
            return "中";
        }
        if (total >= PASS_LINE) {
            return "及格";
        }
        return "不及格";
    }

    /**
     * 判断是否及格。
     *
     * @return 总评成绩不低于 60 返回 true
     */
    public boolean isPassed() {
        Double total = totalScore != null ? totalScore : calcTotal();
        return total != null && total >= PASS_LINE;
    }

    /**
     * 四舍五入保留 1 位小数。
     *
     * @param value 原值
     * @return 保留 1 位小数的结果
     */
    private static double round1(double value) {
        return Math.round(value * 10D) / 10D;
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

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public Double getUsualScore() {
        return usualScore;
    }

    public void setUsualScore(Double usualScore) {
        this.usualScore = usualScore;
    }

    public Double getExamScore() {
        return examScore;
    }

    public void setExamScore(Double examScore) {
        this.examScore = examScore;
    }

    public Double getMakeupScore() {
        return makeupScore;
    }

    public void setMakeupScore(Double makeupScore) {
        this.makeupScore = makeupScore;
    }

    public Double getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(Double totalScore) {
        this.totalScore = totalScore;
    }

    public Boolean getIsRetake() {
        return isRetake;
    }

    public void setIsRetake(Boolean isRetake) {
        this.isRetake = isRetake;
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

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public Double getCredit() {
        return credit;
    }

    public void setCredit(Double credit) {
        this.credit = credit;
    }

    public String getCourseType() {
        return courseType;
    }

    public void setCourseType(String courseType) {
        this.courseType = courseType;
    }

    @Override
    public String toString() {
        return (studentName == null ? "" : studentName) + " - "
                + (courseName == null ? "" : courseName) + " : " + getTotalScore();
    }
}
