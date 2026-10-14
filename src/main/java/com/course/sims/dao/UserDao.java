package com.course.sims.dao;

import com.course.sims.entity.User;
import com.course.sims.util.Db;
import com.course.sims.util.PageResult;
import com.course.sims.util.QueryBuilder;
import com.course.sims.util.RowMapper;

import java.sql.Timestamp;
import java.util.List;

/**
 * 用户数据访问对象。
 * <p>负责 {@code sys_user} 表的全部持久化操作，不含任何业务规则判断。</p>
 *
 * @author 课程设计第 2 小组 - 成员 A
 * @version 1.0.0
 */
public class UserDao {

    /** 结果集行映射器 */
    public static final RowMapper<User> MAPPER = rs -> {
        User u = new User();
        u.setId(Db.getInt(rs, "id"));
        u.setUsername(rs.getString("username"));
        u.setPassword(rs.getString("password"));
        u.setRealName(rs.getString("real_name"));
        u.setRole(rs.getString("role"));
        u.setRelatedSno(rs.getString("related_sno"));
        u.setPhone(rs.getString("phone"));
        u.setStatus(Db.getInt(rs, "status"));
        u.setLastLogin(Db.getTimestamp(rs, "last_login"));
        u.setCreateTime(Db.getTimestamp(rs, "create_time"));
        return u;
    };

    private static final String COLUMNS =
            "id, username, password, real_name, role, related_sno, phone, status, last_login, create_time";

    /**
     * 按用户名查询用户（登录认证使用）。
     *
     * @param username 用户名
     * @return 用户对象，不存在返回 null
     */
    public User findByUsername(String username) {
        return Db.queryOne("SELECT " + COLUMNS + " FROM sys_user WHERE username = ?", MAPPER, username);
    }

    /**
     * 按主键查询用户。
     *
     * @param id 主键
     * @return 用户对象，不存在返回 null
     */
    public User findById(Integer id) {
        return Db.queryOne("SELECT " + COLUMNS + " FROM sys_user WHERE id = ?", MAPPER, id);
    }

    /**
     * 分页查询用户列表。
     *
     * @param keyword  关键字（匹配用户名或真实姓名）
     * @param role     角色过滤
     * @param status   状态过滤
     * @param pageNo   页码
     * @param pageSize 每页条数
     * @return 分页结果
     */
    public PageResult<User> findByPage(String keyword, String role, Integer status, int pageNo, int pageSize) {
        QueryBuilder qb = new QueryBuilder();
        qb.likeAny(keyword, "username", "real_name").eq("role", role).eq("status", status);
        long total = Db.queryInt("SELECT COUNT(*) FROM sys_user" + qb.whereClause(), qb.params().toArray());
        List<Object> params = qb.params();
        params.add(pageSize);
        params.add((pageNo - 1) * pageSize);
        List<User> rows = Db.query(
                "SELECT " + COLUMNS + " FROM sys_user" + qb.whereClause() + " ORDER BY id LIMIT ? OFFSET ?",
                MAPPER, params.toArray());
        return new PageResult<>(rows, total, pageNo, pageSize);
    }

    /**
     * 查询全部启用状态的用户。
     *
     * @return 用户列表
     */
    public List<User> findAllEnabled() {
        return Db.query("SELECT " + COLUMNS + " FROM sys_user WHERE status = 1 ORDER BY id", MAPPER);
    }

    /**
     * 统计指定角色的用户数量。
     *
     * @param role 角色
     * @return 用户数量
     */
    public int countByRole(String role) {
        return Db.queryInt("SELECT COUNT(*) FROM sys_user WHERE role = ?", role);
    }

    /**
     * 判断用户名是否已存在。
     *
     * @param username  用户名
     * @param excludeId 需排除的主键（修改场景传入自身 id，新增传 null）
     * @return 已存在返回 true
     */
    public boolean existsUsername(String username, Integer excludeId) {
        if (excludeId == null) {
            return Db.queryInt("SELECT COUNT(*) FROM sys_user WHERE username = ?", username) > 0;
        }
        return Db.queryInt("SELECT COUNT(*) FROM sys_user WHERE username = ? AND id <> ?", username, excludeId) > 0;
    }

    /**
     * 新增用户。
     *
     * @param user 用户对象
     * @return 新增记录主键
     */
    public int insert(User user) {
        return Db.insertReturnKey(
                "INSERT INTO sys_user (username, password, real_name, role, related_sno, phone, status, create_time) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                user.getUsername(), user.getPassword(), user.getRealName(), user.getRole(),
                user.getRelatedSno() == null ? "" : user.getRelatedSno(),
                user.getPhone() == null ? "" : user.getPhone(),
                user.getStatus() == null ? 1 : user.getStatus(), new Timestamp(System.currentTimeMillis()));
    }

    /**
     * 修改用户基本信息（不含密码）。
     *
     * @param user 用户对象
     * @return 受影响行数
     */
    public int update(User user) {
        return Db.update(
                "UPDATE sys_user SET real_name = ?, role = ?, related_sno = ?, phone = ?, status = ? WHERE id = ?",
                user.getRealName(), user.getRole(),
                user.getRelatedSno() == null ? "" : user.getRelatedSno(),
                user.getPhone() == null ? "" : user.getPhone(),
                user.getStatus(), user.getId());
    }

    /**
     * 修改密码。
     *
     * @param id          用户主键
     * @param newPassword 新密码（已加密）
     * @return 受影响行数
     */
    public int updatePassword(Integer id, String newPassword) {
        return Db.update("UPDATE sys_user SET password = ? WHERE id = ?", newPassword, id);
    }

    /**
     * 更新最后登录时间。
     *
     * @param id        用户主键
     * @param lastLogin 登录时间
     * @return 受影响行数
     */
    public int updateLastLogin(Integer id, Timestamp lastLogin) {
        return Db.update("UPDATE sys_user SET last_login = ? WHERE id = ?", lastLogin, id);
    }

    /**
     * 删除用户。
     *
     * @param id 用户主键
     * @return 受影响行数
     */
    public int deleteById(Integer id) {
        return Db.update("DELETE FROM sys_user WHERE id = ?", id);
    }

    /**
     * 统计用户总数。
     *
     * @return 用户总数
     */
    public int countAll() {
        return Db.queryInt("SELECT COUNT(*) FROM sys_user");
    }
}
