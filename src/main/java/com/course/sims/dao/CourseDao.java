package com.course.sims.dao;

import com.course.sims.entity.Course;
import com.course.sims.util.Db;
import com.course.sims.util.PageResult;
import com.course.sims.util.QueryBuilder;
import com.course.sims.util.RowMapper;

import java.sql.Timestamp;
import java.util.List;

/**
 * 课程数据访问对象，对应数据表 {@code course}。
 *
 * @author 课程设计第 2 小组 - 成员 C
 * @version 1.0.0
 */
public class CourseDao {

    /** 结果集行映射器 */
    public static final RowMapper<Course> MAPPER = rs -> {
        Course c = new Course();
        c.setId(Db.getInt(rs, "id"));
        c.setCourseCode(rs.getString("course_code"));
        c.setCourseName(rs.getString("course_name"));
        c.setCredit(Db.getDouble(rs, "credit"));
        c.setHours(Db.getInt(rs, "hours"));
        c.setCourseType(rs.getString("course_type"));
        c.setAssessType(rs.getString("assess_type"));
        c.setTeacher(rs.getString("teacher"));
        c.setSemester(rs.getString("semester"));
        c.setCreateTime(Db.getTimestamp(rs, "create_time"));
        return c;
    };

    private static final String COLUMNS = "id, course_code, course_name, credit, hours, course_type, assess_type, "
            + "teacher, semester, create_time";

    /**
     * 分页查询课程。
     *
     * @param keyword    关键字（课程代码 / 课程名称 / 授课教师）
     * @param courseType 课程类型过滤
     * @param semester   学期过滤
     * @param pageNo     页码
     * @param pageSize   每页条数
     * @return 分页结果
     */
    public PageResult<Course> findByPage(String keyword, String courseType, String semester,
                                         int pageNo, int pageSize) {
        QueryBuilder qb = new QueryBuilder();
        qb.likeAny(keyword, "course_code", "course_name", "teacher")
                .eq("course_type", courseType).eq("semester", semester);
        long total = Db.queryInt("SELECT COUNT(*) FROM course" + qb.whereClause(), qb.params().toArray());
        List<Object> params = qb.params();
        params.add(pageSize);
        params.add((pageNo - 1) * pageSize);
        List<Course> rows = Db.query("SELECT " + COLUMNS + " FROM course" + qb.whereClause()
                + " ORDER BY course_code LIMIT ? OFFSET ?", MAPPER, params.toArray());
        return new PageResult<>(rows, total, pageNo, pageSize);
    }

    /**
     * 查询全部课程（下拉框使用）。
     *
     * @return 课程列表
     */
    public List<Course> findAll() {
        return Db.query("SELECT " + COLUMNS + " FROM course ORDER BY course_code", MAPPER);
    }

    /**
     * 按学期查询课程。
     *
     * @param semester 学期
     * @return 课程列表
     */
    public List<Course> findBySemester(String semester) {
        return Db.query("SELECT " + COLUMNS + " FROM course WHERE semester = ? ORDER BY course_code",
                MAPPER, semester);
    }

    /**
     * 按主键查询。
     *
     * @param id 主键
     * @return 课程对象，不存在返回 null
     */
    public Course findById(Integer id) {
        return Db.queryOne("SELECT " + COLUMNS + " FROM course WHERE id = ?", MAPPER, id);
    }

    /**
     * 判断课程代码是否重复。
     *
     * @param code      课程代码
     * @param excludeId 排除的主键
     * @return 重复返回 true
     */
    public boolean existsCode(String code, Integer excludeId) {
        if (excludeId == null) {
            return Db.queryInt("SELECT COUNT(*) FROM course WHERE course_code = ?", code) > 0;
        }
        return Db.queryInt("SELECT COUNT(*) FROM course WHERE course_code = ? AND id <> ?", code, excludeId) > 0;
    }

    /**
     * 新增课程。
     *
     * @param c 课程对象
     * @return 主键
     */
    public int insert(Course c) {
        return Db.insertReturnKey(
                "INSERT INTO course (course_code, course_name, credit, hours, course_type, assess_type, teacher, "
                        + "semester, create_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                c.getCourseCode(), c.getCourseName(), c.getCredit(), c.getHours(), c.getCourseType(),
                c.getAssessType(), c.getTeacher() == null ? "" : c.getTeacher(),
                c.getSemester() == null ? "" : c.getSemester(), new Timestamp(System.currentTimeMillis()));
    }

    /**
     * 修改课程。
     *
     * @param c 课程对象
     * @return 受影响行数
     */
    public int update(Course c) {
        return Db.update(
                "UPDATE course SET course_code = ?, course_name = ?, credit = ?, hours = ?, course_type = ?, "
                        + "assess_type = ?, teacher = ?, semester = ? WHERE id = ?",
                c.getCourseCode(), c.getCourseName(), c.getCredit(), c.getHours(), c.getCourseType(),
                c.getAssessType(), c.getTeacher(), c.getSemester(), c.getId());
    }

    /**
     * 删除课程。
     *
     * @param id 主键
     * @return 受影响行数
     */
    public int deleteById(Integer id) {
        return Db.update("DELETE FROM course WHERE id = ?", id);
    }

    /**
     * 统计课程总数。
     *
     * @return 课程数量
     */
    public int countAll() {
        return Db.queryInt("SELECT COUNT(*) FROM course");
    }

    /**
     * 查询全部开课学期（下拉框使用）。
     *
     * @return 学期列表，按倒序排列
     */
    public List<String> findAllSemesters() {
        return Db.query("SELECT DISTINCT semester FROM course WHERE semester IS NOT NULL AND semester <> '' "
                + "ORDER BY semester DESC", rs -> rs.getString(1));
    }

    /**
     * 查询全部授课教师（下拉框使用）。
     *
     * @return 教师姓名列表
     */
    public List<String> findAllTeachers() {
        return Db.query("SELECT DISTINCT teacher FROM course WHERE teacher IS NOT NULL AND teacher <> '' "
                + "ORDER BY teacher", rs -> rs.getString(1));
    }

    /**
     * 统计某教师承担的课程数。
     *
     * @param teacher 教师姓名
     * @return 课程数量
     */
    public int countByTeacher(String teacher) {
        return Db.queryInt("SELECT COUNT(*) FROM course WHERE teacher = ?", teacher);
    }
}
