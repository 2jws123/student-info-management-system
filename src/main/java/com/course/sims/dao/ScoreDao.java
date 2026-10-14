package com.course.sims.dao;

import com.course.sims.entity.Score;
import com.course.sims.util.Db;
import com.course.sims.util.PageResult;
import com.course.sims.util.QueryBuilder;
import com.course.sims.util.RowMapper;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 成绩数据访问对象，对应数据表 {@code score}。
 *
 * <p>统计类方法（平均分、及格率、分数段分布、排名）均以 SQL 聚合在数据库端完成，
 * 避免把全表数据加载到内存中做计算，保证大数据量下的查询效率。</p>
 *
 * @author 课程设计第 2 小组 - 成员 C
 * @version 1.0.0
 */
public class ScoreDao {

    /** 结果集行映射器（仅主表字段） */
    public static final RowMapper<Score> MAPPER = rs -> {
        Score s = new Score();
        s.setId(Db.getInt(rs, "id"));
        s.setStudentId(Db.getInt(rs, "student_id"));
        s.setCourseId(Db.getInt(rs, "course_id"));
        s.setSemester(rs.getString("semester"));
        s.setUsualScore(Db.getDouble(rs, "usual_score"));
        s.setExamScore(Db.getDouble(rs, "exam_score"));
        s.setMakeupScore(Db.getDouble(rs, "makeup_score"));
        s.setTotalScore(Db.getDouble(rs, "total_score"));
        s.setIsRetake(rs.getBoolean("is_retake"));
        s.setRemark(rs.getString("remark"));
        s.setCreateTime(Db.getTimestamp(rs, "create_time"));
        return s;
    };

    /** 结果集行映射器（联表带出学生与课程信息） */
    public static final RowMapper<Score> JOIN_MAPPER = rs -> {
        Score s = MAPPER.map(rs);
        s.setSno(rs.getString("sno"));
        s.setStudentName(rs.getString("student_name"));
        s.setClassName(rs.getString("class_name"));
        s.setCourseCode(rs.getString("course_code"));
        s.setCourseName(rs.getString("course_name"));
        s.setCredit(Db.getDouble(rs, "credit"));
        s.setCourseType(rs.getString("course_type"));
        return s;
    };

    private static final String JOIN_SQL =
            "SELECT sc.id, sc.student_id, sc.course_id, sc.semester, sc.usual_score, sc.exam_score, "
                    + "sc.makeup_score, sc.total_score, sc.is_retake, sc.remark, sc.create_time, "
                    + "st.sno, st.name AS student_name, c.class_name, co.course_code, co.course_name, "
                    + "co.credit, co.course_type "
                    + "FROM score sc "
                    + "JOIN student st ON sc.student_id = st.id "
                    + "LEFT JOIN clazz c ON st.class_id = c.id "
                    + "JOIN course co ON sc.course_id = co.id ";

    /**
     * 分页查询成绩。
     *
     * @param keyword   关键字（学号 / 学生姓名 / 课程名称）
     * @param classId   班级过滤
     * @param courseId  课程过滤
     * @param semester  学期过滤
     * @param passState 及格状态：null 全部、true 及格、false 不及格
     * @param pageNo    页码
     * @param pageSize  每页条数
     * @return 分页结果
     */
    public PageResult<Score> findByPage(String keyword, Integer classId, Integer courseId, String semester,
                                        Boolean passState, int pageNo, int pageSize) {
        QueryBuilder countQb = new QueryBuilder();
        countQb.likeAny(keyword, "st.sno", "st.name", "co.course_name").eq("st.class_id", classId)
                .eq("sc.course_id", courseId).eq("sc.semester", semester);
        if (passState != null) {
            countQb.raw(passState ? "sc.total_score >= 60" : "(sc.total_score < 60 OR sc.total_score IS NULL)");
        }
        String countSql = "SELECT COUNT(*) FROM score sc JOIN student st ON sc.student_id = st.id "
                + "JOIN course co ON sc.course_id = co.id" + countQb.whereClause();
        long total = Db.queryInt(countSql, countQb.params().toArray());

        QueryBuilder qb = new QueryBuilder();
        qb.likeAny(keyword, "st.sno", "st.name", "co.course_name").eq("st.class_id", classId)
                .eq("sc.course_id", courseId).eq("sc.semester", semester);
        if (passState != null) {
            qb.raw(passState ? "sc.total_score >= 60" : "(sc.total_score < 60 OR sc.total_score IS NULL)");
        }
        List<Object> params = qb.params();
        params.add(pageSize);
        params.add((pageNo - 1) * pageSize);
        List<Score> rows = Db.query(JOIN_SQL + qb.whereClause()
                + " ORDER BY st.sno, co.course_code LIMIT ? OFFSET ?", JOIN_MAPPER, params.toArray());
        return new PageResult<>(rows, total, pageNo, pageSize);
    }

    /**
     * 查询成绩明细列表（导出场景使用，不分页）。
     *
     * @param keyword  关键字
     * @param classId  班级过滤
     * @param courseId 课程过滤
     * @param semester 学期过滤
     * @return 成绩列表
     */
    public List<Score> findList(String keyword, Integer classId, Integer courseId, String semester) {
        QueryBuilder qb = new QueryBuilder();
        qb.likeAny(keyword, "st.sno", "st.name", "co.course_name").eq("st.class_id", classId)
                .eq("sc.course_id", courseId).eq("sc.semester", semester);
        return Db.query(JOIN_SQL + qb.whereClause() + " ORDER BY st.sno, co.course_code", JOIN_MAPPER,
                qb.params().toArray());
    }

    /**
     * 查询某学生的全部成绩（成绩单使用）。
     *
     * @param studentId 学生主键
     * @param semester  学期过滤，可为 null
     * @return 成绩列表
     */
    public List<Score> findByStudent(Integer studentId, String semester) {
        QueryBuilder qb = new QueryBuilder();
        qb.eq("sc.student_id", studentId).eq("sc.semester", semester);
        return Db.query(JOIN_SQL + qb.whereClause() + " ORDER BY sc.semester, co.course_code", JOIN_MAPPER,
                qb.params().toArray());
    }

    /**
     * 查询某课程下全部学生成绩（批量录入与课程统计使用）。
     *
     * @param courseId 课程主键
     * @param classId  班级过滤，可为 null
     * @return 成绩列表
     */
    public List<Score> findByCourse(Integer courseId, Integer classId) {
        QueryBuilder qb = new QueryBuilder();
        qb.eq("sc.course_id", courseId).eq("st.class_id", classId);
        return Db.query(JOIN_SQL + qb.whereClause() + " ORDER BY st.sno", JOIN_MAPPER, qb.params().toArray());
    }

    /**
     * 按主键查询成绩。
     *
     * @param id 主键
     * @return 成绩对象，不存在返回 null
     */
    public Score findById(Integer id) {
        return Db.queryOne(JOIN_SQL + " WHERE sc.id = ?", JOIN_MAPPER, id);
    }

    /**
     * 查询某学生某门课程的成绩记录（判重使用）。
     *
     * @param studentId 学生主键
     * @param courseId  课程主键
     * @return 成绩对象，不存在返回 null
     */
    public Score findByStudentAndCourse(Integer studentId, Integer courseId) {
        return Db.queryOne("SELECT sc.id, sc.student_id, sc.course_id, sc.semester, sc.usual_score, sc.exam_score, "
                        + "sc.makeup_score, sc.total_score, sc.is_retake, sc.remark, sc.create_time FROM score sc "
                        + "WHERE sc.student_id = ? AND sc.course_id = ?",
                MAPPER, studentId, courseId);
    }

    /**
     * 新增成绩。
     *
     * @param s 成绩对象
     * @return 主键
     */
    public int insert(Score s) {
        return Db.insertReturnKey(
                "INSERT INTO score (student_id, course_id, semester, usual_score, exam_score, makeup_score, "
                        + "is_retake, remark, create_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                s.getStudentId(), s.getCourseId(), s.getSemester(), s.getUsualScore(), s.getExamScore(),
                s.getMakeupScore(), Boolean.TRUE.equals(s.getIsRetake()),
                s.getRemark() == null ? "" : s.getRemark(), new Timestamp(System.currentTimeMillis()));
    }

    /**
     * 修改成绩。
     *
     * @param s 成绩对象
     * @return 受影响行数
     */
    public int update(Score s) {
        return Db.update(
                "UPDATE score SET semester = ?, usual_score = ?, exam_score = ?, makeup_score = ?, is_retake = ?, "
                        + "remark = ? WHERE id = ?",
                s.getSemester(), s.getUsualScore(), s.getExamScore(), s.getMakeupScore(),
                Boolean.TRUE.equals(s.getIsRetake()), s.getRemark(), s.getId());
    }

    /**
     * 批量保存成绩（先删后插，用于整班批量录入）。
     *
     * @param list 成绩列表
     * @return 成功写入的记录数
     */
    public int batchSave(List<Score> list) {
        if (list == null || list.isEmpty()) {
            return 0;
        }
        List<Object[]> params = new ArrayList<>();
        for (Score s : list) {
            params.add(new Object[]{s.getStudentId(), s.getCourseId(), s.getSemester(), s.getUsualScore(),
                    s.getExamScore(), s.getMakeupScore(), Boolean.TRUE.equals(s.getIsRetake()),
                    s.getRemark() == null ? "" : s.getRemark(), new Timestamp(System.currentTimeMillis())});
        }
        return Db.batchUpdate(
                "INSERT INTO score (student_id, course_id, semester, usual_score, exam_score, makeup_score, "
                        + "is_retake, remark, create_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", params);
    }

    /**
     * 删除成绩。
     *
     * @param id 主键
     * @return 受影响行数
     */
    public int deleteById(Integer id) {
        return Db.update("DELETE FROM score WHERE id = ?", id);
    }

    /**
     * 删除某学生全部成绩（删除学生时级联清理）。
     *
     * @param studentId 学生主键
     * @return 受影响行数
     */
    public int deleteByStudent(Integer studentId) {
        return Db.update("DELETE FROM score WHERE student_id = ?", studentId);
    }

    /**
     * 删除某课程全部成绩（删除课程时级联清理）。
     *
     * @param courseId 课程主键
     * @return 受影响行数
     */
    public int deleteByCourse(Integer courseId) {
        return Db.update("DELETE FROM score WHERE course_id = ?", courseId);
    }

    /**
     * 统计某课程的成绩记录数（删除课程前的关联校验）。
     *
     * @param courseId 课程主键
     * @return 成绩记录数
     */
    public int countByCourse(Integer courseId) {
        return Db.queryInt("SELECT COUNT(*) FROM score WHERE course_id = ?", courseId);
    }

    /**
     * 统计某学生的成绩记录数（删除学生前的关联校验）。
     *
     * @param studentId 学生主键
     * @return 成绩记录数
     */
    public int countByStudent(Integer studentId) {
        return Db.queryInt("SELECT COUNT(*) FROM score WHERE student_id = ?", studentId);
    }

    /**
     * 统计成绩记录总数。
     *
     * @return 成绩数量
     */
    public int countAll() {
        return Db.queryInt("SELECT COUNT(*) FROM score");
    }

    /**
     * 统计某学生已修学分（仅统计及格课程）。
     *
     * @param studentId 学生主键
     * @return 已获学分
     */
    public double sumPassedCredit(Integer studentId) {
        return Db.queryDouble("SELECT COALESCE(SUM(co.credit), 0) FROM score sc "
                + "JOIN course co ON sc.course_id = co.id WHERE sc.student_id = ? AND sc.total_score >= 60", studentId);
    }

    /**
     * 计算某学生平均绩点（GPA）。
     * <p>公式：GPA = Σ(课程绩点 × 学分) / Σ学分。</p>
     *
     * @param studentId 学生主键
     * @return 平均绩点，保留 2 位小数
     */
    public double calcGpa(Integer studentId) {
        double sum = Db.queryDouble("SELECT COALESCE(SUM("
                + "CASE WHEN sc.total_score >= 90 THEN 4.0 WHEN sc.total_score >= 80 THEN 3.0 "
                + "WHEN sc.total_score >= 70 THEN 2.0 WHEN sc.total_score >= 60 THEN 1.0 ELSE 0 END * co.credit), 0) "
                + "FROM score sc JOIN course co ON sc.course_id = co.id "
                + "WHERE sc.student_id = ? AND sc.total_score IS NOT NULL", studentId);
        double credits = Db.queryDouble("SELECT COALESCE(SUM(co.credit), 0) FROM score sc "
                + "JOIN course co ON sc.course_id = co.id "
                + "WHERE sc.student_id = ? AND sc.total_score IS NOT NULL", studentId);
        if (credits <= 0) {
            return 0D;
        }
        return Math.round(sum / credits * 100D) / 100D;
    }

    /**
     * 计算某学生加权平均分。
     *
     * @param studentId 学生主键
     * @return 加权平均分，无成绩返回 0
     */
    public double calcWeightedAverage(Integer studentId) {
        double sum = Db.queryDouble("SELECT COALESCE(SUM(sc.total_score * co.credit), 0) FROM score sc "
                + "JOIN course co ON sc.course_id = co.id "
                + "WHERE sc.student_id = ? AND sc.total_score IS NOT NULL", studentId);
        double credits = Db.queryDouble("SELECT COALESCE(SUM(co.credit), 0) FROM score sc "
                + "JOIN course co ON sc.course_id = co.id "
                + "WHERE sc.student_id = ? AND sc.total_score IS NOT NULL", studentId);
        if (credits <= 0) {
            return 0D;
        }
        return Math.round(sum / credits * 100D) / 100D;
    }

    /**
     * 统计某学生不及格课程门数。
     *
     * @param studentId 学生主键
     * @return 不及格门数
     */
    public int countFailed(Integer studentId) {
        return Db.queryInt("SELECT COUNT(*) FROM score WHERE student_id = ? AND total_score < 60", studentId);
    }

    /**
     * 课程成绩统计（平均分 / 最高分 / 最低分 / 及格率）。
     *
     * @param courseId 课程主键
     * @return Map：avg_score、max_score、min_score、total_count、pass_count、pass_rate
     */
    public Map<String, Object> courseStat(Integer courseId) {
        return Db.queryForMap("SELECT ROUND(AVG(total_score), 1) AS avg_score, MAX(total_score) AS max_score, "
                + "MIN(total_score) AS min_score, COUNT(*) AS total_count, "
                + "SUM(CASE WHEN total_score >= 60 THEN 1 ELSE 0 END) AS pass_count, "
                + "ROUND(100.0 * SUM(CASE WHEN total_score >= 60 THEN 1 ELSE 0 END) / NULLIF(COUNT(*), 0), 1) AS pass_rate "
                + "FROM score WHERE course_id = ? AND total_score IS NOT NULL", courseId);
    }

    /**
     * 班级平均分统计并排名（统计模块使用）。
     *
     * @param semester 学期过滤，可为 null
     * @return 列表：class_name、student_count、avg_score、pass_rate
     */
    public List<Map<String, Object>> classAverageRank(String semester) {
        QueryBuilder qb = new QueryBuilder();
        qb.eq("sc.semester", semester);
        return Db.queryForList("SELECT c.class_name, COUNT(DISTINCT sc.student_id) AS student_count, "
                + "ROUND(AVG(sc.total_score), 1) AS avg_score, "
                + "ROUND(100.0 * SUM(CASE WHEN sc.total_score >= 60 THEN 1 ELSE 0 END) / COUNT(*), 1) AS pass_rate "
                + "FROM score sc JOIN student st ON sc.student_id = st.id "
                + "JOIN clazz c ON st.class_id = c.id " + qb.whereClause()
                + " GROUP BY c.class_name ORDER BY avg_score DESC", qb.params().toArray());
    }

    /**
     * 课程平均分统计（统计模块使用）。
     *
     * @param semester 学期过滤，可为 null
     * @return 列表：course_name、avg_score、max_score、min_score、pass_rate
     */
    public List<Map<String, Object>> courseAverageStat(String semester) {
        QueryBuilder qb = new QueryBuilder();
        qb.eq("sc.semester", semester);
        return Db.queryForList("SELECT co.course_name, ROUND(AVG(sc.total_score), 1) AS avg_score, "
                + "MAX(sc.total_score) AS max_score, MIN(sc.total_score) AS min_score, "
                + "ROUND(100.0 * SUM(CASE WHEN sc.total_score >= 60 THEN 1 ELSE 0 END) / COUNT(*), 1) AS pass_rate "
                + "FROM score sc JOIN course co ON sc.course_id = co.id " + qb.whereClause()
                + " GROUP BY co.course_name ORDER BY avg_score DESC", qb.params().toArray());
    }

    /**
     * 成绩五档分布统计（优 / 良 / 中 / 及格 / 不及格）。
     *
     * @param courseId 课程过滤，可为 null
     * @return 列表：level_name、level_count
     */
    public List<Map<String, Object>> scoreDistribution(Integer courseId) {
        QueryBuilder qb = new QueryBuilder();
        qb.eq("course_id", courseId);
        String sql = "SELECT CASE WHEN total_score >= 90 THEN '优(90-100)' WHEN total_score >= 80 THEN '良(80-89)' "
                + "WHEN total_score >= 70 THEN '中(70-79)' WHEN total_score >= 60 THEN '及格(60-69)' "
                + "ELSE '不及格(<60)' END AS level_name, COUNT(*) AS level_count "
                + "FROM score WHERE total_score IS NOT NULL"
                + (qb.isEmpty() ? "" : " AND " + qb.conditionClause())
                + " GROUP BY level_name ORDER BY MIN(total_score) DESC";
        return Db.queryForList(sql, qb.params().toArray());
    }

    /**
     * 分数段人数分布（每 10 分一段）。
     *
     * @return 列表：score_range、num
     */
    public List<Map<String, Object>> scoreSegment() {
        // H2 要求 ORDER BY 表达式必须出现在 GROUP BY 中，因此先按分数计算分段再在外层排序
        return Db.queryForList("SELECT score_range, COUNT(*) AS num FROM ("
                + "SELECT CONCAT(FLOOR(total_score / 10) * 10, '-', FLOOR(total_score / 10) * 10 + 9) AS score_range, "
                + "FLOOR(total_score / 10) AS seg FROM score WHERE total_score IS NOT NULL) t "
                + "GROUP BY score_range ORDER BY score_range DESC");
    }

    /**
     * 学生成绩排名（按加权平均分）。
     *
     * @param classId  班级过滤，可为 null 表示全校
     * @param semester 学期过滤，可为 null
     * @param topN     取前 N 名
     * @return 列表：sno、name、class_name、avg_score、total_credit
     */
    public List<Map<String, Object>> studentRank(Integer classId, String semester, int topN) {
        QueryBuilder qb = new QueryBuilder();
        qb.eq("st.class_id", classId).eq("sc.semester", semester);
        String sql = "SELECT st.sno, st.name, c.class_name, ROUND(AVG(sc.total_score), 1) AS avg_score, "
                + "COUNT(*) AS course_count, COALESCE(SUM(CASE WHEN sc.total_score >= 60 THEN co.credit ELSE 0 END), 0) "
                + "AS total_credit FROM score sc JOIN student st ON sc.student_id = st.id "
                + "JOIN clazz c ON st.class_id = c.id JOIN course co ON sc.course_id = co.id "
                + "WHERE sc.total_score IS NOT NULL AND " + qb.conditionClause()
                + " GROUP BY st.sno, st.name, c.class_name ORDER BY avg_score DESC LIMIT ?";
        List<Object> params = qb.params();
        params.add(topN);
        return Db.queryForList(sql, params.toArray());
    }

    /**
     * 全校成绩概览统计（首页看板使用）。
     *
     * @return Map：avg_score、pass_rate、excellent_count、failed_count、total_count
     */
    public Map<String, Object> overviewStat() {
        return Db.queryForMap("SELECT ROUND(AVG(total_score), 1) AS avg_score, "
                + "ROUND(100.0 * SUM(CASE WHEN total_score >= 60 THEN 1 ELSE 0 END) / NULLIF(COUNT(*), 0), 1) AS pass_rate, "
                + "SUM(CASE WHEN total_score >= 90 THEN 1 ELSE 0 END) AS excellent_count, "
                + "SUM(CASE WHEN total_score < 60 THEN 1 ELSE 0 END) AS failed_count, "
                + "COUNT(*) AS total_count FROM score WHERE total_score IS NOT NULL");
    }

    /**
     * 不及格学生名单（补考管理使用）。
     *
     * @param courseId 课程过滤，可为 null
     * @return 列表：sno、name、class_name、course_name、total_score
     */
    public List<Map<String, Object>> failedList(Integer courseId) {
        QueryBuilder qb = new QueryBuilder();
        qb.eq("sc.course_id", courseId);
        return Db.queryForList("SELECT st.sno, st.name, c.class_name, co.course_name, sc.total_score "
                + "FROM score sc JOIN student st ON sc.student_id = st.id "
                + "LEFT JOIN clazz c ON st.class_id = c.id JOIN course co ON sc.course_id = co.id "
                + "WHERE sc.total_score < 60" + (qb.isEmpty() ? "" : " AND " + qb.conditionClause())
                + " ORDER BY sc.total_score", qb.params().toArray());
    }
}
