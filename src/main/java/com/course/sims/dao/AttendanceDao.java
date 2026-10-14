package com.course.sims.dao;

import com.course.sims.entity.Attendance;
import com.course.sims.util.Db;
import com.course.sims.util.PageResult;
import com.course.sims.util.QueryBuilder;
import com.course.sims.util.RowMapper;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 考勤数据访问对象，对应数据表 {@code attendance}。
 *
 * @author 课程设计第 2 小组 - 成员 C
 * @version 1.0.0
 */
public class AttendanceDao {

    /** 结果集行映射器（仅主表字段） */
    public static final RowMapper<Attendance> MAPPER = rs -> {
        Attendance a = new Attendance();
        a.setId(Db.getInt(rs, "id"));
        a.setStudentId(Db.getInt(rs, "student_id"));
        a.setCourseId(Db.getInt(rs, "course_id"));
        a.setAttDate(Db.getDate(rs, "att_date"));
        a.setStatus(rs.getString("status"));
        a.setRemark(rs.getString("remark"));
        a.setCreateTime(Db.getTimestamp(rs, "create_time"));
        return a;
    };

    /** 结果集行映射器（联表带出学生与课程信息） */
    public static final RowMapper<Attendance> JOIN_MAPPER = rs -> {
        Attendance a = MAPPER.map(rs);
        a.setSno(rs.getString("sno"));
        a.setStudentName(rs.getString("student_name"));
        a.setCourseName(rs.getString("course_name"));
        return a;
    };

    private static final String JOIN_SQL =
            "SELECT a.id, a.student_id, a.course_id, a.att_date, a.status, a.remark, a.create_time, "
                    + "st.sno, st.name AS student_name, co.course_name "
                    + "FROM attendance a "
                    + "JOIN student st ON a.student_id = st.id "
                    + "JOIN course co ON a.course_id = co.id ";

    /**
     * 分页查询考勤记录。
     *
     * @param keyword  关键字（学号 / 学生姓名 / 课程名称）
     * @param classId  班级过滤
     * @param courseId 课程过滤
     * @param status   考勤状态过滤
     * @param startDay 起始日期
     * @param endDay   截止日期
     * @param pageNo   页码
     * @param pageSize 每页条数
     * @return 分页结果
     */
    public PageResult<Attendance> findByPage(String keyword, Integer classId, Integer courseId, String status,
                                             Date startDay, Date endDay, int pageNo, int pageSize) {
        QueryBuilder countQb = buildQuery(keyword, classId, courseId, status, startDay, endDay);
        long total = Db.queryInt("SELECT COUNT(*) FROM attendance a "
                + "JOIN student st ON a.student_id = st.id JOIN course co ON a.course_id = co.id"
                + countQb.whereClause(), countQb.params().toArray());

        QueryBuilder qb = buildQuery(keyword, classId, courseId, status, startDay, endDay);
        List<Object> params = qb.params();
        params.add(pageSize);
        params.add((pageNo - 1) * pageSize);
        List<Attendance> rows = Db.query(JOIN_SQL + qb.whereClause()
                + " ORDER BY a.att_date DESC, st.sno LIMIT ? OFFSET ?", JOIN_MAPPER, params.toArray());
        return new PageResult<>(rows, total, pageNo, pageSize);
    }

    /**
     * 构造查询条件（列表与计数查询共用，全部列名带表别名，避免多表联查时列名歧义）。
     */
    private QueryBuilder buildQuery(String keyword, Integer classId, Integer courseId, String status,
                                    Date startDay, Date endDay) {
        QueryBuilder qb = new QueryBuilder();
        qb.likeAny(keyword, "st.sno", "st.name", "co.course_name")
                .eq("st.class_id", classId)
                .eq("a.course_id", courseId)
                .eq("a.status", status);
        if (startDay != null) {
            qb.raw("a.att_date >= DATE '" + startDay + "'");
        }
        if (endDay != null) {
            qb.raw("a.att_date <= DATE '" + endDay + "'");
        }
        return qb;
    }

    /**
     * 查询考勤明细（导出使用，不分页）。
     *
     * @param classId  班级过滤
     * @param courseId 课程过滤
     * @param startDay 起始日期
     * @param endDay   截止日期
     * @return 考勤列表
     */
    public List<Attendance> findList(Integer classId, Integer courseId, Date startDay, Date endDay) {
        QueryBuilder qb = buildQuery(null, classId, courseId, null, startDay, endDay);
        return Db.query(JOIN_SQL + qb.whereClause() + " ORDER BY a.att_date DESC, st.sno", JOIN_MAPPER,
                qb.params().toArray());
    }

    /**
     * 查询某学生考勤记录（学生个人考勤查看使用）。
     *
     * @param studentId 学生主键
     * @return 考勤列表
     */
    public List<Attendance> findByStudent(Integer studentId) {
        return Db.query(JOIN_SQL + " WHERE a.student_id = ? ORDER BY a.att_date DESC", JOIN_MAPPER, studentId);
    }

    /**
     * 按主键查询。
     *
     * @param id 主键
     * @return 考勤对象，不存在返回 null
     */
    public Attendance findById(Integer id) {
        return Db.queryOne(JOIN_SQL + " WHERE a.id = ?", JOIN_MAPPER, id);
    }

    /**
     * 查询某学生某课程某日的考勤记录（批量登记时的更新判断）。
     *
     * @param studentId 学生主键
     * @param courseId  课程主键
     * @param attDate   考勤日期
     * @return 考勤记录，不存在返回 null
     */
    public Attendance findByStudentAndCourseDate(Integer studentId, Integer courseId, Date attDate) {
        return Db.queryOne(JOIN_SQL + " WHERE a.student_id = ? AND a.course_id = ? AND a.att_date = ?",
                JOIN_MAPPER, studentId, courseId, attDate);
    }

    /**
     * 判断某学生某课程某日是否已有考勤记录。
     *
     * @param studentId 学生主键
     * @param courseId  课程主键
     * @param attDate   考勤日期
     * @param excludeId 排除的主键
     * @return 已存在返回 true
     */
    public boolean exists(Integer studentId, Integer courseId, Date attDate, Integer excludeId) {
        if (excludeId == null) {
            return Db.queryInt("SELECT COUNT(*) FROM attendance WHERE student_id = ? AND course_id = ? "
                    + "AND att_date = ?", studentId, courseId, attDate) > 0;
        }
        return Db.queryInt("SELECT COUNT(*) FROM attendance WHERE student_id = ? AND course_id = ? "
                + "AND att_date = ? AND id <> ?", studentId, courseId, attDate, excludeId) > 0;
    }

    /**
     * 新增考勤记录。
     *
     * @param a 考勤对象
     * @return 主键
     */
    public int insert(Attendance a) {
        return Db.insertReturnKey(
                "INSERT INTO attendance (student_id, course_id, att_date, status, remark, create_time) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                a.getStudentId(), a.getCourseId(), a.getAttDate(), a.getStatus(),
                a.getRemark() == null ? "" : a.getRemark(), new Timestamp(System.currentTimeMillis()));
    }

    /**
     * 批量登记考勤（整班一次登记）。
     *
     * @param list 考勤列表
     * @return 成功写入的记录数
     */
    public int batchInsert(List<Attendance> list) {
        if (list == null || list.isEmpty()) {
            return 0;
        }
        List<Object[]> params = new ArrayList<>();
        for (Attendance a : list) {
            params.add(new Object[]{a.getStudentId(), a.getCourseId(), a.getAttDate(), a.getStatus(),
                    a.getRemark() == null ? "" : a.getRemark(), new Timestamp(System.currentTimeMillis())});
        }
        return Db.batchUpdate("INSERT INTO attendance (student_id, course_id, att_date, status, remark, create_time) "
                + "VALUES (?, ?, ?, ?, ?, ?)", params);
    }

    /**
     * 修改考勤记录。
     *
     * @param a 考勤对象
     * @return 受影响行数
     */
    public int update(Attendance a) {
        return Db.update("UPDATE attendance SET student_id = ?, course_id = ?, att_date = ?, status = ?, remark = ? "
                + "WHERE id = ?", a.getStudentId(), a.getCourseId(), a.getAttDate(), a.getStatus(),
                a.getRemark(), a.getId());
    }

    /**
     * 删除考勤记录。
     *
     * @param id 主键
     * @return 受影响行数
     */
    public int deleteById(Integer id) {
        return Db.update("DELETE FROM attendance WHERE id = ?", id);
    }

    /**
     * 删除某学生全部考勤记录（删除学生时级联清理）。
     *
     * @param studentId 学生主键
     * @return 受影响行数
     */
    public int deleteByStudent(Integer studentId) {
        return Db.update("DELETE FROM attendance WHERE student_id = ?", studentId);
    }

    /**
     * 删除某课程全部考勤记录（删除课程时级联清理）。
     *
     * @param courseId 课程主键
     * @return 受影响行数
     */
    public int deleteByCourse(Integer courseId) {
        return Db.update("DELETE FROM attendance WHERE course_id = ?", courseId);
    }

    /**
     * 统计某课程的考勤记录数（删除课程前的关联校验）。
     *
     * @param courseId 课程主键
     * @return 考勤记录数
     */
    public int countByCourse(Integer courseId) {
        return Db.queryInt("SELECT COUNT(*) FROM attendance WHERE course_id = ?", courseId);
    }

    /**
     * 统计某学生的考勤记录数。
     *
     * @param studentId 学生主键
     * @return 考勤记录数
     */
    public int countByStudent(Integer studentId) {
        return Db.queryInt("SELECT COUNT(*) FROM attendance WHERE student_id = ?", studentId);
    }

    /**
     * 统计考勤记录总数。
     *
     * @return 记录数量
     */
    public int countAll() {
        return Db.queryInt("SELECT COUNT(*) FROM attendance");
    }

    /**
     * 按状态统计考勤次数（看板与饼图使用）。
     *
     * @return 列表：status、num
     */
    public List<Map<String, Object>> countGroupByStatus() {
        return Db.queryForList("SELECT status, COUNT(*) AS num FROM attendance GROUP BY status "
                + "ORDER BY COUNT(*) DESC");
    }

    /**
     * 计算某学生出勤率。
     *
     * @param studentId 学生主键
     * @return 出勤率百分比（保留 1 位小数），无记录返回 100
     */
    public double attendanceRate(Integer studentId) {
        int total = Db.queryInt("SELECT COUNT(*) FROM attendance WHERE student_id = ?", studentId);
        if (total == 0) {
            return 100D;
        }
        int present = Db.queryInt("SELECT COUNT(*) FROM attendance WHERE student_id = ? AND status = '出勤'",
                studentId);
        return Math.round(100D * present / total * 10D) / 10D;
    }

    /**
     * 统计某学生缺勤次数（预警判断使用）。
     *
     * @param studentId 学生主键
     * @return 缺勤次数
     */
    public int countAbsent(Integer studentId) {
        return Db.queryInt("SELECT COUNT(*) FROM attendance WHERE student_id = ? AND status = '缺勤'", studentId);
    }

    /**
     * 统计某学生异常考勤总次数（迟到、早退、请假、缺勤）。
     *
     * @param studentId 学生主键
     * @return 异常次数
     */
    public int countAbnormal(Integer studentId) {
        return Db.queryInt("SELECT COUNT(*) FROM attendance WHERE student_id = ? AND status <> '出勤'", studentId);
    }

    /**
     * 考勤预警名单：异常次数达到阈值的所有学生。
     *
     * @param threshold 预警阈值
     * @return 列表：sno、name、class_name、absent_count、abnormal_count、total_count、rate
     */
    public List<Map<String, Object>> warningList(int threshold) {
        return Db.queryForList("SELECT st.sno, st.name, c.class_name, "
                + "SUM(CASE WHEN a.status = '缺勤' THEN 1 ELSE 0 END) AS absent_count, "
                + "SUM(CASE WHEN a.status <> '出勤' THEN 1 ELSE 0 END) AS abnormal_count, "
                + "COUNT(*) AS total_count, "
                + "ROUND(100.0 * SUM(CASE WHEN a.status = '出勤' THEN 1 ELSE 0 END) / COUNT(*), 1) AS rate "
                + "FROM attendance a JOIN student st ON a.student_id = st.id "
                + "LEFT JOIN clazz c ON st.class_id = c.id "
                + "GROUP BY st.sno, st.name, c.class_name "
                + "HAVING SUM(CASE WHEN a.status <> '出勤' THEN 1 ELSE 0 END) >= ? "
                + "ORDER BY abnormal_count DESC", threshold);
    }

    /**
     * 按班级统计出勤率（统计模块使用）。
     *
     * @return 列表：class_name、total_count、present_count、rate
     */
    public List<Map<String, Object>> classAttendanceRate() {
        return Db.queryForList("SELECT c.class_name, COUNT(*) AS total_count, "
                + "SUM(CASE WHEN a.status = '出勤' THEN 1 ELSE 0 END) AS present_count, "
                + "ROUND(100.0 * SUM(CASE WHEN a.status = '出勤' THEN 1 ELSE 0 END) / COUNT(*), 1) AS rate "
                + "FROM attendance a JOIN student st ON a.student_id = st.id "
                + "LEFT JOIN clazz c ON st.class_id = c.id "
                + "GROUP BY c.class_name ORDER BY rate DESC");
    }

    /**
     * 查询最近若干条考勤记录（首页动态使用）。
     *
     * @param limit 数量
     * @return 考勤列表
     */
    public List<Attendance> findRecent(int limit) {
        return Db.query(JOIN_SQL + " ORDER BY a.att_date DESC, a.id DESC LIMIT ?", JOIN_MAPPER, limit);
    }
}
