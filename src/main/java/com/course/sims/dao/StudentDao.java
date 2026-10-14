package com.course.sims.dao;

import com.course.sims.entity.Student;
import com.course.sims.util.Db;
import com.course.sims.util.PageResult;
import com.course.sims.util.QueryBuilder;
import com.course.sims.util.RowMapper;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 学生数据访问对象，对应数据表 {@code student}。
 *
 * <p>学号生成策略：以"入学年份 + 3 位流水号"生成，流水号从数据库现有最大学号
 * 继续递增，JVM 内使用 {@link AtomicInteger} 缓存避免重复查询（模板方法 + 单例缓存）。</p>
 *
 * @author 课程设计第 2 小组 - 成员 B
 * @version 1.0.0
 */
public class StudentDao {

    /** 结果集行映射器（仅主表字段） */
    public static final RowMapper<Student> MAPPER = rs -> {
        Student s = new Student();
        s.setId(Db.getInt(rs, "id"));
        s.setSno(rs.getString("sno"));
        s.setName(rs.getString("name"));
        s.setGender(rs.getString("gender"));
        s.setBirthday(Db.getDate(rs, "birthday"));
        s.setClassId(Db.getInt(rs, "class_id"));
        s.setPhone(rs.getString("phone"));
        s.setEmail(rs.getString("email"));
        s.setAddress(rs.getString("address"));
        s.setEnrollYear(Db.getInt(rs, "enroll_year"));
        s.setStatus(rs.getString("status"));
        s.setCreateTime(Db.getTimestamp(rs, "create_time"));
        return s;
    };

    /** 结果集行映射器（联表带出班级与专业信息） */
    public static final RowMapper<Student> JOIN_MAPPER = rs -> {
        Student s = MAPPER.map(rs);
        s.setClassName(rs.getString("class_name"));
        s.setClassCode(rs.getString("class_code"));
        s.setMajorName(rs.getString("major_name"));
        s.setGrade(Db.getInt(rs, "grade"));
        return s;
    };

    private static final String COLUMNS = "id, sno, name, gender, birthday, class_id, phone, email, address, "
            + "enroll_year, status, create_time";

    private static final String JOIN_SQL =
            "SELECT s.id, s.sno, s.name, s.gender, s.birthday, s.class_id, s.phone, s.email, s.address, "
                    + "s.enroll_year, s.status, s.create_time, c.class_name, c.class_code, c.grade, m.major_name "
                    + "FROM student s LEFT JOIN clazz c ON s.class_id = c.id LEFT JOIN major m ON c.major_id = m.id ";

    /** 各入学年份的学号流水号缓存 */
    private static final Map<Integer, AtomicInteger> SNO_SEQ = new ConcurrentHashMap<>();

    /**
     * 分页查询学生。
     *
     * @param keyword  关键字（学号 / 姓名 / 手机号）
     * @param classId  班级过滤
     * @param status   学籍状态过滤
     * @param gender   性别过滤
     * @param pageNo   页码
     * @param pageSize 每页条数
     * @return 分页结果
     */
    public PageResult<Student> findByPage(String keyword, Integer classId, String status, String gender,
                                          int pageNo, int pageSize) {
        QueryBuilder countQb = new QueryBuilder();
        countQb.likeAny(keyword, "sno", "name", "phone").eq("class_id", classId)
                .eq("status", status).eq("gender", gender);
        long total = Db.queryInt("SELECT COUNT(*) FROM student" + countQb.whereClause(), countQb.params().toArray());

        QueryBuilder qb = new QueryBuilder();
        qb.likeAny(keyword, "s.sno", "s.name", "s.phone").eq("s.class_id", classId)
                .eq("s.status", status).eq("s.gender", gender);
        List<Object> params = qb.params();
        params.add(pageSize);
        params.add((pageNo - 1) * pageSize);
        List<Student> rows = Db.query(JOIN_SQL + qb.whereClause() + " ORDER BY s.sno LIMIT ? OFFSET ?",
                JOIN_MAPPER, params.toArray());
        return new PageResult<>(rows, total, pageNo, pageSize);
    }

    /**
     * 查询满足条件的学生（不分页，导出场景使用）。
     *
     * @param keyword 关键字
     * @param classId 班级过滤
     * @param status  学籍状态过滤
     * @return 学生列表
     */
    public List<Student> findList(String keyword, Integer classId, String status) {
        QueryBuilder qb = new QueryBuilder();
        qb.likeAny(keyword, "s.sno", "s.name", "s.phone").eq("s.class_id", classId).eq("s.status", status);
        return Db.query(JOIN_SQL + qb.whereClause() + " ORDER BY s.sno", JOIN_MAPPER, qb.params().toArray());
    }

    /**
     * 按班级查询学生列表（成绩单、考勤批量登记使用）。
     *
     * @param classId 班级主键
     * @return 学生列表
     */
    public List<Student> findByClass(Integer classId) {
        QueryBuilder qb = new QueryBuilder();
        qb.eq("s.class_id", classId);
        return Db.query(JOIN_SQL + qb.whereClause() + " ORDER BY s.sno", JOIN_MAPPER, qb.params().toArray());
    }

    /**
     * 按主键查询学生。
     *
     * @param id 主键
     * @return 学生对象，不存在返回 null
     */
    public Student findById(Integer id) {
        return Db.queryOne(JOIN_SQL + " WHERE s.id = ?", JOIN_MAPPER, id);
    }

    /**
     * 按学号查询学生。
     *
     * @param sno 学号
     * @return 学生对象，不存在返回 null
     */
    public Student findBySno(String sno) {
        return Db.queryOne(JOIN_SQL + " WHERE s.sno = ?", JOIN_MAPPER, sno);
    }

    /**
     * 判断学号是否重复。
     *
     * @param sno       学号
     * @param excludeId 排除的主键
     * @return 重复返回 true
     */
    public boolean existsSno(String sno, Integer excludeId) {
        if (excludeId == null) {
            return Db.queryInt("SELECT COUNT(*) FROM student WHERE sno = ?", sno) > 0;
        }
        return Db.queryInt("SELECT COUNT(*) FROM student WHERE sno = ? AND id <> ?", sno, excludeId) > 0;
    }

    /**
     * 新增学生。
     *
     * @param s 学生对象
     * @return 主键
     */
    public int insert(Student s) {
        return Db.insertReturnKey(
                "INSERT INTO student (sno, name, gender, birthday, class_id, phone, email, address, enroll_year, "
                        + "status, create_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                s.getSno(), s.getName(), s.getGender(), s.getBirthday(), s.getClassId(),
                s.getPhone() == null ? "" : s.getPhone(),
                s.getEmail() == null ? "" : s.getEmail(),
                s.getAddress() == null ? "" : s.getAddress(),
                s.getEnrollYear(), s.getStatus() == null ? Student.STATUS_STUDYING : s.getStatus(),
                new Timestamp(System.currentTimeMillis()));
    }

    /**
     * 修改学生。
     *
     * @param s 学生对象
     * @return 受影响行数
     */
    public int update(Student s) {
        return Db.update(
                "UPDATE student SET sno = ?, name = ?, gender = ?, birthday = ?, class_id = ?, phone = ?, "
                        + "email = ?, address = ?, enroll_year = ?, status = ? WHERE id = ?",
                s.getSno(), s.getName(), s.getGender(), s.getBirthday(), s.getClassId(), s.getPhone(),
                s.getEmail(), s.getAddress(), s.getEnrollYear(), s.getStatus(), s.getId());
    }

    /**
     * 删除学生。
     *
     * @param id 主键
     * @return 受影响行数
     */
    public int deleteById(Integer id) {
        return Db.update("DELETE FROM student WHERE id = ?", id);
    }

    /**
     * 生成新的学号：入学年份 + 3 位流水号，如 2025041。
     *
     * @param enrollYear 入学年份
     * @return 未被占用的学号
     */
    public String generateSno(int enrollYear) {
        String prefix = String.valueOf(enrollYear);
        AtomicInteger seq = SNO_SEQ.computeIfAbsent(enrollYear, y -> {
            String maxSno = Db.queryString("SELECT MAX(sno) FROM student WHERE sno LIKE ?", prefix + "%");
            int start = 1;
            if (maxSno != null && maxSno.length() > prefix.length()) {
                try {
                    start = Integer.parseInt(maxSno.substring(prefix.length())) + 1;
                } catch (NumberFormatException ignored) {
                    start = 1;
                }
            }
            return new AtomicInteger(start);
        });
        String sno;
        do {
            sno = prefix + String.format("%03d", seq.getAndIncrement());
        } while (existsSno(sno, null));
        return sno;
    }

    /**
     * 统计学生总数。
     *
     * @return 学生数量
     */
    public int countAll() {
        return Db.queryInt("SELECT COUNT(*) FROM student");
    }

    /**
     * 统计指定学籍状态的学生数量。
     *
     * @param status 学籍状态
     * @return 学生数量
     */
    public int countByStatus(String status) {
        return Db.queryInt("SELECT COUNT(*) FROM student WHERE status = ?", status);
    }

    /**
     * 统计各班人数（班级容量校验 / 人数分布统计使用）。
     *
     * @return 列表，每项含 class_id、class_name、student_count
     */
    public List<Map<String, Object>> countGroupByClass() {
        return Db.queryForList("SELECT c.id AS class_id, c.class_name, COUNT(s.id) AS student_count "
                + "FROM clazz c LEFT JOIN student s ON s.class_id = c.id "
                + "GROUP BY c.id, c.class_name ORDER BY c.class_code");
    }

    /**
     * 统计各专业人数（统计模块使用）。
     *
     * @return 列表，每项含 major_name、student_count
     */
    public List<Map<String, Object>> countGroupByMajor() {
        return Db.queryForList("SELECT m.major_name, COUNT(s.id) AS student_count "
                + "FROM major m LEFT JOIN clazz c ON c.major_id = m.id "
                + "LEFT JOIN student s ON s.class_id = c.id "
                + "GROUP BY m.major_name ORDER BY COUNT(s.id) DESC");
    }

    /**
     * 统计各年级人数（统计模块使用）。
     *
     * @return 列表，每项含 grade、student_count
     */
    public List<Map<String, Object>> countGroupByGrade() {
        return Db.queryForList("SELECT c.grade, COUNT(s.id) AS student_count FROM clazz c "
                + "LEFT JOIN student s ON s.class_id = c.id GROUP BY c.grade ORDER BY c.grade");
    }

    /**
     * 统计男女比例（统计模块使用）。
     *
     * @return 列表，每项含 gender、student_count
     */
    public List<Map<String, Object>> countGroupByGender() {
        return Db.queryForList("SELECT gender, COUNT(*) AS student_count FROM student GROUP BY gender");
    }

    /**
     * 删除班级前校验：统计某班级下的学生数量。
     *
     * @param classId 班级主键
     * @return 学生数量
     */
    public int countByClass(Integer classId) {
        return Db.queryInt("SELECT COUNT(*) FROM student WHERE class_id = ?", classId);
    }
}
