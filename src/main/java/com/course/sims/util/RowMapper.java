package com.course.sims.util;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 结果集行映射接口（策略模式）。
 * <p>DAO 层把 {@link ResultSet} 的一行数据封装成实体对象，
 * 由调用方通过 Lambda 表达式提供映射规则，避免重复的 while(rs.next()) 样板代码。</p>
 *
 * @param <T> 映射目标类型
 * @author 课程设计第 2 小组 - 成员 A（公共模块）
 * @version 1.0.0
 */
@FunctionalInterface
public interface RowMapper<T> {

    /**
     * 将结果集当前行映射为实体对象。
     *
     * @param rs 结果集，游标已定位到有效行
     * @return 实体对象
     * @throws SQLException 读取字段异常
     */
    T map(ResultSet rs) throws SQLException;
}
