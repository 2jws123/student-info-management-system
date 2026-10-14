package com.course.sims.util;

import java.util.ArrayList;
import java.util.List;

/**
 * 动态 SQL 条件构造器（建造者模式）。
 * <p>用于拼接带有可选查询条件的 WHERE 子句，全部使用"?"占位符 + 参数列表，
 * 从根本上杜绝 SQL 注入；同时避免在 DAO 中书写大量 if 判断。</p>
 *
 * <p>使用示例：</p>
 * <pre>
 * QueryBuilder qb = new QueryBuilder();
 * qb.like("s.name", keyword)
 *   .eq("s.class_id", classId)
 *   .eq("s.status", status);
 * String where = qb.whereClause();       // " WHERE s.name LIKE ? AND s.class_id = ?"
 * List&lt;Object&gt; params = qb.params();
 * </pre>
 *
 * @author 课程设计第 2 小组 - 成员 A（公共模块）
 * @version 1.0.0
 */
public class QueryBuilder {

    /** 单个条件片段，如 " AND s.name LIKE ?" */
    private final List<String> conditions = new ArrayList<>();
    /** 与条件片段一一对应的参数值 */
    private final List<Object> params = new ArrayList<>();

    /**
     * 追加等值条件；值为 null 或空串时忽略该条件。
     *
     * @param column 列名（含表别名）
     * @param value  条件值
     * @return 当前构造器，支持链式调用
     */
    public QueryBuilder eq(String column, Object value) {
        if (value == null || (value instanceof String && ((String) value).isBlank())) {
            return this;
        }
        conditions.add(column + " = ?");
        params.add(value);
        return this;
    }

    /**
     * 追加不等值条件；值为 null 或空串时忽略。
     *
     * @param column 列名
     * @param value  条件值
     * @return 当前构造器
     */
    public QueryBuilder ne(String column, Object value) {
        if (value == null || (value instanceof String && ((String) value).isBlank())) {
            return this;
        }
        conditions.add(column + " \u003c\u003e ?");
        params.add(value);
        return this;
    }

    /**
     * 追加模糊查询条件（LIKE '%值%'）；值为空时忽略。
     *
     * @param column  列名
     * @param keyword 关键字
     * @return 当前构造器
     */
    public QueryBuilder like(String column, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return this;
        }
        conditions.add(column + " LIKE ?");
        params.add("%" + keyword.trim() + "%");
        return this;
    }

    /**
     * 追加"任意字段模糊匹配"条件，适用于多列 OR 关键字检索。
     *
     * @param keyword 关键字
     * @param columns 参与匹配的列
     * @return 当前构造器
     */
    public QueryBuilder likeAny(String keyword, String... columns) {
        if (keyword == null || keyword.isBlank() || columns == null || columns.length == 0) {
            return this;
        }
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < columns.length; i++) {
            if (i > 0) {
                sb.append(" OR ");
            }
            sb.append(columns[i]).append(" LIKE ?");
            params.add("%" + keyword.trim() + "%");
        }
        sb.append(')');
        conditions.add(sb.toString());
        return this;
    }

    /**
     * 追加原生 SQL 条件（调用方自行保证不含用户输入）。
     *
     * @param rawCondition 原生条件片段
     * @return 当前构造器
     */
    public QueryBuilder raw(String rawCondition) {
        if (rawCondition != null && !rawCondition.isBlank()) {
            conditions.add(rawCondition);
        }
        return this;
    }

    /**
     * 生成以 WHERE 开头的条件子句。
     *
     * @return 无条件时返回空串
     */
    public String whereClause() {
        if (conditions.isEmpty()) {
            return "";
        }
        return " WHERE " + String.join(" AND ", conditions);
    }

    /**
     * 生成不带 WHERE 关键字的条件子句，便于拼接自定义 SQL。
     *
     * @return 条件表达式，无条件时返回 "1=1"
     */
    public String conditionClause() {
        if (conditions.isEmpty()) {
            return "1=1";
        }
        return String.join(" AND ", conditions);
    }

    /** @return 参数列表（顺序与占位符一致） */
    public List<Object> params() {
        return params;
    }

    /** @return 是否没有任何有效条件 */
    public boolean isEmpty() {
        return conditions.isEmpty();
    }
}
