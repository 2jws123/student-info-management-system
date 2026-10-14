package com.course.sims.dao;

import com.course.sims.entity.Clazz;
import com.course.sims.util.Db;
import com.course.sims.util.PageResult;
import com.course.sims.util.QueryBuilder;
import com.course.sims.util.RowMapper;

import java.sql.Timestamp;
import java.util.List;

/**
 * 班级数据访问对象，对应数据表 {@code clazz}。
 *
 * @author 课程设计第 2 小组 - 成员 B
 * @version 1.0.0
 */
public class ClazzDao {

    /** 结果集行映射器 */
    public static final RowMapper<Clazz> MAPPER = rs -> {
        Clazz c = new Clazz();
        c.setId(Db.getInt(rs, "id"));
        c.setClassCode(rs.getString("class_code"));
        c.setClassName(rs.getString("class_name"));
        c.setMajorId(Db.getInt(rs, "major_id"));
        c.setGrade(Db.getInt(rs, "grade"));
        c.setHeadteacher(rs.getString("headteacher"));
        c.setCapacity(Db.getInt(rs, "capacity"));
        c.setCreateTime(Db.getTimestamp(rs, "create_time"));
        return c;
    };

    private static final String COLUMNS =
            "id, class_code, class_name, major_id, grade, headteacher, capacity, create_time";

    /**
     * 分页查询班级，联表带出专业名称与实际人数。
     *
     * @param keyword  关键字（班级代码 / 班级名称 / 班主任）
     * @param majorId  专业过滤
     * @param grade    年级过滤
     * @param pageNo   页码
     * @param pageSize 每页条数
     * @return 分页结果
     */
    public PageResult<Clazz> findByPage(String keyword, Integer majorId, Integer grade, int pageNo, int pageSize) {
        QueryBuilder countQb = new QueryBuilder();
        countQb.likeAny(keyword, "class_code", "class_name", "headteacher").eq("major_id", majorId).eq("grade", grade);
        long total = Db.queryInt("SELECT COUNT(*) FROM clazz" + countQb.whereClause(), countQb.params().toArray());

        QueryBuilder qb = new QueryBuilder();
        qb.likeAny(keyword, "c.class_code", "c.class_name", "c.headteacher")
                .eq("c.major_id", majorId).eq("c.grade", grade);
        List<Object> params = qb.params();
        params.add(pageSize);
        params.add((pageNo - 1) * pageSize);
        String sql = "SELECT c.id, c.class_code, c.class_name, c.major_id, c.grade, c.headteacher, c.capacity, "
                + "c.create_time, m.major_name, "
                + "(SELECT COUNT(*) FROM student s WHERE s.class_id = c.id) AS student_count "
                + "FROM clazz c LEFT JOIN major m ON c.major_id = m.id"
                + qb.whereClause() + " ORDER BY c.grade DESC, c.class_code LIMIT ? OFFSET ?";
        List<Clazz> rows = Db.query(sql, rs -> {
            Clazz c = MAPPER.map(rs);
            c.setMajorName(rs.getString("major_name"));
            c.setStudentCount(rs.getInt("student_count"));
            return c;
        }, params.toArray());
        return new PageResult<>(rows, total, pageNo, pageSize);
    }

    /**
     * 查询全部班级（下拉框与统计使用），带专业名称与实际人数。
     *
     * <p>注意：必须同时统计 student_count，否则调用方（如下拉框显示容量、
     * 班级容量校验、按班级统计人数）拿到的实际人数会恒为 0。</p>
     *
     * @return 班级列表
     */
    public List<Clazz> findAll() {
        String sql = "SELECT c.id, c.class_code, c.class_name, c.major_id, c.grade, c.headteacher, c.capacity, "
                + "c.create_time, m.major_name, "
                + "(SELECT COUNT(*) FROM student s WHERE s.class_id = c.id) AS student_count "
                + "FROM clazz c LEFT JOIN major m ON c.major_id = m.id "
                + "ORDER BY c.grade DESC, c.class_code";
        return Db.query(sql, rs -> {
            Clazz c = MAPPER.map(rs);
            c.setMajorName(rs.getString("major_name"));
            c.setStudentCount(rs.getInt("student_count"));
            return c;
        });
    }

    /**
     * 按专业查询班级列表。
     *
     * @param majorId 专业主键
     * @return 班级列表
     */
    public List<Clazz> findByMajor(Integer majorId) {
        return Db.query("SELECT " + COLUMNS + " FROM clazz WHERE major_id = ? ORDER BY class_code", MAPPER, majorId);
    }

    /**
     * 按主键查询。
     *
     * @param id 主键
     * @return 班级对象，不存在返回 null
     */
    public Clazz findById(Integer id) {
        return Db.queryOne("SELECT " + COLUMNS + " FROM clazz WHERE id = ?", MAPPER, id);
    }

    /**
     * 判断班级代码是否重复。
     *
     * @param code      班级代码
     * @param excludeId 排除的主键
     * @return 重复返回 true
     */
    public boolean existsCode(String code, Integer excludeId) {
        if (excludeId == null) {
            return Db.queryInt("SELECT COUNT(*) FROM clazz WHERE class_code = ?", code) > 0;
        }
        return Db.queryInt("SELECT COUNT(*) FROM clazz WHERE class_code = ? AND id <> ?", code, excludeId) > 0;
    }

    /**
     * 新增班级。
     *
     * @param clazz 班级对象
     * @return 主键
     */
    public int insert(Clazz clazz) {
        return Db.insertReturnKey(
                "INSERT INTO clazz (class_code, class_name, major_id, grade, headteacher, capacity, create_time) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                clazz.getClassCode(), clazz.getClassName(), clazz.getMajorId(), clazz.getGrade(),
                clazz.getHeadteacher() == null ? "" : clazz.getHeadteacher(),
                clazz.getCapacity() == null ? 50 : clazz.getCapacity(),
                new Timestamp(System.currentTimeMillis()));
    }

    /**
     * 修改班级。
     *
     * @param clazz 班级对象
     * @return 受影响行数
     */
    public int update(Clazz clazz) {
        return Db.update(
                "UPDATE clazz SET class_code = ?, class_name = ?, major_id = ?, grade = ?, headteacher = ?, "
                        + "capacity = ? WHERE id = ?",
                clazz.getClassCode(), clazz.getClassName(), clazz.getMajorId(), clazz.getGrade(),
                clazz.getHeadteacher(), clazz.getCapacity(), clazz.getId());
    }

    /**
     * 删除班级。
     *
     * @param id 主键
     * @return 受影响行数
     */
    public int deleteById(Integer id) {
        return Db.update("DELETE FROM clazz WHERE id = ?", id);
    }

    /**
     * 统计某专业下的班级数量（删除专业前的关联校验）。
     *
     * @param majorId 专业主键
     * @return 班级数量
     */
    public int countByMajor(Integer majorId) {
        return Db.queryInt("SELECT COUNT(*) FROM clazz WHERE major_id = ?", majorId);
    }

    /**
     * 统计班级总数。
     *
     * @return 班级数量
     */
    public int countAll() {
        return Db.queryInt("SELECT COUNT(*) FROM clazz");
    }

    /**
     * 统计指定年级的班级数量。
     *
     * @param grade 年级
     * @return 班级数量
     */
    public int countByGrade(Integer grade) {
        return Db.queryInt("SELECT COUNT(*) FROM clazz WHERE grade = ?", grade);
    }

    /**
     * 查询班级中实际学生人数。
     *
     * @param classId 班级主键
     * @return 学生人数
     */
    public int countStudents(Integer classId) {
        return Db.queryInt("SELECT COUNT(*) FROM student WHERE class_id = ?", classId);
    }
}
