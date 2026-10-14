package com.course.sims.dao;

import com.course.sims.entity.SysLog;
import com.course.sims.util.Db;
import com.course.sims.util.PageResult;
import com.course.sims.util.QueryBuilder;
import com.course.sims.util.RowMapper;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;

/**
 * 系统操作日志数据访问对象，对应数据表 {@code sys_log}。
 *
 * @author 课程设计第 2 小组 - 成员 D
 * @version 1.0.0
 */
public class SysLogDao {

    /** 结果集行映射器 */
    public static final RowMapper<SysLog> MAPPER = rs -> {
        SysLog log = new SysLog();
        log.setId(Db.getInt(rs, "id"));
        log.setUsername(rs.getString("username"));
        log.setOperation(rs.getString("operation"));
        log.setDetail(rs.getString("detail"));
        log.setLogTime(Db.getTimestamp(rs, "log_time"));
        return log;
    };

    private static final String COLUMNS = "id, username, operation, detail, log_time";

    /**
     * 写入操作日志。
     *
     * @param log 日志对象
     * @return 主键
     */
    public int insert(SysLog log) {
        return Db.insertReturnKey("INSERT INTO sys_log (username, operation, detail, log_time) VALUES (?, ?, ?, ?)",
                log.getUsername(), log.getOperation(),
                log.getDetail() == null ? "" : log.getDetail(), new Timestamp(System.currentTimeMillis()));
    }

    /**
     * 分页查询日志。
     *
     * @param keyword  关键字（操作人 / 操作类型 / 详情）
     * @param startDay 起始日期，可为 null
     * @param endDay   截止日期，可为 null
     * @param pageNo   页码
     * @param pageSize 每页条数
     * @return 分页结果
     */
    public PageResult<SysLog> findByPage(String keyword, Date startDay, Date endDay, int pageNo, int pageSize) {
        QueryBuilder qb = new QueryBuilder();
        qb.likeAny(keyword, "username", "operation", "detail");
        if (startDay != null) {
            qb.raw("log_time >= TIMESTAMP '" + startDay + " 00:00:00'");
        }
        if (endDay != null) {
            qb.raw("log_time <= TIMESTAMP '" + endDay + " 23:59:59'");
        }
        long total = Db.queryInt("SELECT COUNT(*) FROM sys_log" + qb.whereClause(), qb.params().toArray());
        List<Object> params = qb.params();
        params.add(pageSize);
        params.add((pageNo - 1) * pageSize);
        List<SysLog> rows = Db.query("SELECT " + COLUMNS + " FROM sys_log" + qb.whereClause()
                + " ORDER BY log_time DESC, id DESC LIMIT ? OFFSET ?", MAPPER, params.toArray());
        return new PageResult<>(rows, total, pageNo, pageSize);
    }

    /**
     * 查询最近若干条日志（首页动态使用）。
     *
     * @param limit 数量
     * @return 日志列表
     */
    public List<SysLog> findRecent(int limit) {
        return Db.query("SELECT " + COLUMNS + " FROM sys_log ORDER BY log_time DESC, id DESC LIMIT ?",
                MAPPER, limit);
    }

    /**
     * 清空全部日志。
     *
     * @return 删除的记录数
     */
    public int clearAll() {
        return Db.update("DELETE FROM sys_log");
    }

    /**
     * 统计日志总数。
     *
     * @return 日志数量
     */
    public int countAll() {
        return Db.queryInt("SELECT COUNT(*) FROM sys_log");
    }

    /**
     * 统计某操作人的日志数量。
     *
     * @param username 操作人
     * @return 日志数量
     */
    public int countByUser(String username) {
        return Db.queryInt("SELECT COUNT(*) FROM sys_log WHERE username = ?", username);
    }
}
