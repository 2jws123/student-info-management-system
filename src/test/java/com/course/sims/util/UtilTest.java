package com.course.sims.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 公共工具类单元测试。
 *
 * <p>覆盖：MD5 加密与校验、分页结果计算、动态 SQL 条件构造器。</p>
 *
 * @author 课程设计第 2 小组 - 成员 E
 * @version 1.0.0
 */
@DisplayName("公共工具类测试")
class UtilTest {

    // ------------------------------------------------------------------
    //  TC-U-01 ~ TC-U-04  MD5 加密工具
    // ------------------------------------------------------------------

    @Test
    @DisplayName("TC-U-01 MD5 加密：123456 应得到标准密文")
    void testMd5Encrypt() {
        assertEquals("e10adc3949ba59abbe56e057f20f883e", MD5Util.encrypt("123456"),
                "MD5 加密结果与标准值不一致");
    }

    @Test
    @DisplayName("TC-U-02 MD5 加密：学号 2024001 应得到对应密文")
    void testMd5StudentPassword() {
        assertEquals("f33c8bfc75fda54075373a387b06412c", MD5Util.encrypt("2024001"),
                "学生初始密码密文不一致");
    }

    @Test
    @DisplayName("TC-U-03 MD5 校验：正确密码通过、错误密码拒绝")
    void testMd5Matches() {
        String cipher = MD5Util.encrypt("admin123");
        assertTrue(MD5Util.matches("admin123", cipher), "正确密码应校验通过");
        assertFalse(MD5Util.matches("admin124", cipher), "错误密码应校验失败");
        assertFalse(MD5Util.matches(null, cipher), "空密码应校验失败");
    }

    @Test
    @DisplayName("TC-U-04 MD5 加密：null 输入返回 null")
    void testMd5Null() {
        assertNull(MD5Util.encrypt(null), "null 输入应返回 null");
    }

    // ------------------------------------------------------------------
    //  TC-U-05 ~ TC-U-08  分页结果
    // ------------------------------------------------------------------

    @Test
    @DisplayName("TC-U-05 分页计算：总页数与上下页判定")
    void testPageResult() {
        PageResult<String> page = new PageResult<>(java.util.List.of("a", "b"), 31L, 2, 15);
        assertEquals(3, page.getTotalPages(), "31 条记录每页 15 条应为 3 页");
        assertEquals(31L, page.getTotal(), "总记录数不正确");
        assertTrue(page.hasPrev(), "第 2 页应存在上一页");
        assertTrue(page.hasNext(), "第 2 页应存在下一页");
    }

    @Test
    @DisplayName("TC-U-06 分页计算：末页无下一页")
    void testPageResultLastPage() {
        PageResult<String> page = new PageResult<>(java.util.List.of("a"), 30L, 2, 15);
        assertEquals(2, page.getTotalPages(), "30 条记录每页 15 条应为 2 页");
        assertFalse(page.hasNext(), "末页不应存在下一页");
    }

    @Test
    @DisplayName("TC-U-07 分页计算：空数据总页数为 1")
    void testPageResultEmpty() {
        PageResult<String> page = PageResult.empty(1, 15);
        assertEquals(1, page.getTotalPages(), "空数据总页数应为 1");
        assertEquals(0, page.getRows().size(), "空数据行数应为 0");
    }

    @Test
    @DisplayName("TC-U-08 分页计算：非法页码被纠正为第 1 页")
    void testPageResultInvalidPage() {
        PageResult<String> page = new PageResult<>(java.util.List.of(), 0L, 0, 0);
        assertEquals(1, page.getPageNo(), "页码 0 应被纠正为 1");
        assertEquals(15, page.getPageSize(), "非法每页条数应回落为 15");
    }

    // ------------------------------------------------------------------
    //  TC-U-09 ~ TC-U-13  动态条件构造器
    // ------------------------------------------------------------------

    @Test
    @DisplayName("TC-U-09 条件构造：无条件时返回空串")
    void testQueryBuilderEmpty() {
        QueryBuilder qb = new QueryBuilder();
        assertEquals("", qb.whereClause(), "无有效条件时应返回空串");
        assertTrue(qb.isEmpty(), "isEmpty 应为 true");
        assertEquals("1=1", qb.conditionClause(), "无条件下 conditionClause 应为 1=1");
    }

    @Test
    @DisplayName("TC-U-10 条件构造：空值条件被自动忽略")
    void testQueryBuilderIgnoreBlank() {
        QueryBuilder qb = new QueryBuilder();
        qb.eq("class_id", null).eq("status", "  ").like("name", null).like("sno", "");
        assertEquals("", qb.whereClause(), "null 与空白条件应被忽略");
        assertEquals(0, qb.params().size(), "被忽略的条件不应产生参数");
    }

    @Test
    @DisplayName("TC-U-11 条件构造：等值与模糊查询参数正确")
    void testQueryBuilderConditions() {
        QueryBuilder qb = new QueryBuilder();
        qb.eq("s.class_id", 1).like("s.name", "张");
        assertEquals(" WHERE s.class_id = ? AND s.name LIKE ?", qb.whereClause(), "条件子句拼接不正确");
        assertEquals(2, qb.params().size(), "参数个数应为 2");
        assertEquals("%张%", qb.params().get(1), "模糊查询参数应自动加通配符");
    }

    @Test
    @DisplayName("TC-U-12 条件构造：likeAny 支持多列关键字检索")
    void testQueryBuilderLikeAny() {
        QueryBuilder qb = new QueryBuilder();
        qb.likeAny("2024001", "s.sno", "s.name", "s.phone");
        assertEquals(" WHERE (s.sno LIKE ? OR s.name LIKE ? OR s.phone LIKE ?)", qb.whereClause(),
                "多列关键字条件拼接不正确");
        assertEquals(3, qb.params().size(), "多列检索应产生 3 个参数");
    }

    @Test
    @DisplayName("TC-U-13 条件构造：占位符数量与参数数量必须一致（SQL 注入防护前提）")
    void testQueryBuilderPlaceholderCount() {
        QueryBuilder qb = new QueryBuilder();
        qb.eq("a", 1).ne("b", 2).like("c", "x").raw("d > 0");
        String where = qb.whereClause();
        long placeholders = where.chars().filter(ch -> ch == '?').count();
        assertEquals(placeholders, qb.params().size(),
                "占位符数量与参数数量不一致，可能导致 SQL 注入或执行失败");
    }
}
