package com.course.sims.dao;

import com.course.sims.entity.Major;
import com.course.sims.util.Db;
import com.course.sims.util.PageResult;
import com.course.sims.util.QueryBuilder;
import com.course.sims.util.RowMapper;

import java.sql.Timestamp;
import java.util.List;

/**
 * 专业数据访问对象，对应数据表 {@code major}。
 *
 * @author 课程设计第 2 小组 - 成员 B
 * @version 1.0.0
 */
public class MajorDao {

    /** 结果集行映射器 */
    public static final RowMapper<Major> MAPPER = rs -> {
        Major m = new Major();
        m.setId(Db.getInt(rs, "id"));
        m.setMajorCode(rs.getString("major_code"));
        m.setMajorName(rs.getString("major_name"));
        m.setDeptName(rs.getString("dept_name"));
        m.setDuration(Db.getInt(rs, "duration"));
        m.setRemark(rs.getString("remark"));
        m.setCreateTime(Db.getTimestamp(rs, "create_time"));
        return m;
    };

    private static final String COLUMNS =
            "id, major_code, major_name, dept_name, duration, remark, create_time";

    /**
     * 分页查询专业，并附带该专业下的班级数量。
     *
     * @param keyword  关键字（专业代码 / 专业名称 / 院系）
     * @param pageNo   页码
     * @param pageSize 每页条数
     * @return 分页结果
     */
    public PageResult<Major> findByPage(String keyword, int pageNo, int pageSize) {
        // 关键字条件与主表 count 查询共用（不带别名），列表查询再显式加 m. 前缀
        QueryBuilder countQb = new QueryBuilder();
        countQb.likeAny(keyword, "major_code", "major_name", "dept_name");
        long total = Db.queryInt("SELECT COUNT(*) FROM major" + countQb.whereClause(), countQb.params().toArray());

        QueryBuilder qb = new QueryBuilder();
        qb.likeAny(keyword, "m.major_code", "m.major_name", "m.dept_name");
        List<Object> params = qb.params();
        params.add(pageSize);
        params.add((pageNo - 1) * pageSize);
        String sql = "SELECT m.id, m.major_code, m.major_name, m.dept_name, m.duration, m.remark, m.create_time, "
                + "(SELECT COUNT(*) FROM clazz c WHERE c.major_id = m.id) AS class_count "
                + "FROM major m" + qb.whereClause() + " ORDER BY m.id LIMIT ? OFFSET ?";
        List<Major> rows = Db.query(sql, rs -> {
            Major m = MAPPER.map(rs);
            m.setClassCount(rs.getInt("class_count"));
            return m;
        }, params.toArray());
        return new PageResult<>(rows, total, pageNo, pageSize);
    }

    /**
     * 查询全部专业（下拉框使用）。
     *
     * @return 专业列表
     */
    public List<Major> findAll() {
        return Db.query("SELECT " + COLUMNS + " FROM major ORDER BY id", MAPPER);
    }

    /**
     * 按主键查询。
     *
     * @param id 主键
     * @return 专业对象，不存在返回 null
     */
    public Major findById(Integer id) {
        return Db.queryOne("SELECT " + COLUMNS + " FROM major WHERE id = ?", MAPPER, id);
    }

    /**
     * 判断专业代码是否重复。
     *
     * @param code      专业代码
     * @param excludeId 排除的主键
     * @return 重复返回 true
     */
    public boolean existsCode(String code, Integer excludeId) {
        if (excludeId == null) {
            return Db.queryInt("SELECT COUNT(*) FROM major WHERE major_code = ?", code) > 0;
        }
        return Db.queryInt("SELECT COUNT(*) FROM major WHERE major_code = ? AND id <> ?", code, excludeId) > 0;
    }

    /**
     * 新增专业。
     *
     * @param major 专业对象
     * @return 主键
     */
    public int insert(Major major) {
        return Db.insertReturnKey(
                "INSERT INTO major (major_code, major_name, dept_name, duration, remark, create_time) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                major.getMajorCode(), major.getMajorName(), major.getDeptName(),
                major.getDuration() == null ? 4 : major.getDuration(),
                major.getRemark() == null ? "" : major.getRemark(),
                new Timestamp(System.currentTimeMillis()));
    }

    /**
     * 修改专业。
     *
     * @param major 专业对象
     * @return 受影响行数
     */
    public int update(Major major) {
        return Db.update(
                "UPDATE major SET major_code = ?, major_name = ?, dept_name = ?, duration = ?, remark = ? WHERE id = ?",
                major.getMajorCode(), major.getMajorName(), major.getDeptName(),
                major.getDuration(), major.getRemark(), major.getId());
    }

    /**
     * 删除专业。
     *
     * @param id 主键
     * @return 受影响行数
     */
    public int deleteById(Integer id) {
        return Db.update("DELETE FROM major WHERE id = ?", id);
    }

    /**
     * 统计专业总数。
     *
     * @return 专业数量
     */
    public int countAll() {
        return Db.queryInt("SELECT COUNT(*) FROM major");
    }
}
