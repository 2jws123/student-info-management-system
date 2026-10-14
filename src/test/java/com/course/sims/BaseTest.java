package com.course.sims;

import com.course.sims.config.DatabaseInitializer;
import com.course.sims.util.Db;
import org.junit.jupiter.api.BeforeEach;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

/**
 * 单元测试公共基类。
 *
 * <p>职责：</p>
 * <ol>
 *   <li>把数据库指向 H2 内存库（{@code jdbc:h2:mem:sims_test}），
 *       执行建表与演示数据脚本，测试过程完全不触碰真实运行的 db/sims.mv.db；</li>
 *   <li>每个测试方法执行前把数据库恢复为初始演示数据（{@link #resetDatabase()}），
 *       使用例之间互不影响、可单独重复执行；</li>
 *   <li>提供若干查询辅助方法。</li>
 * </ol>
 *
 * @author 课程设计第 2 小组 - 成员 E
 * @version 1.0.0
 */
public abstract class BaseTest {

    /** 全部业务表（按外键依赖的逆序排列，便于清空） */
    private static final List<String> TABLES = Arrays.asList(
            "score", "attendance", "student", "clazz", "major", "course", "sys_user", "sys_log", "sys_config");

    static {
        System.setProperty("sims.db.path", "jdbc:h2:mem:sims_test");
        DatabaseInitializer.init();
    }

    /**
     * 每个测试方法执行前重置数据库为初始演示数据。
     *
     * <p>这样即使某个用例（例如"重置演示数据""批量导入"）修改或清空了数据，
     * 也不会影响其它用例，从根本上消除用例间的执行顺序依赖。</p>
     */
    @BeforeEach
    void resetDatabase() {
        resetDataOnly();
    }

    /**
     * 仅重置数据（不含登录），供子类在自定义前置逻辑中复用。
     */
    protected void resetDataOnly() {
        try (Connection conn = Db.getConnection(); Statement st = conn.createStatement()) {
            st.execute("SET REFERENTIAL_INTEGRITY FALSE");
            for (String table : TABLES) {
                st.execute("TRUNCATE TABLE " + table + " RESTART IDENTITY");
            }
            st.execute("SET REFERENTIAL_INTEGRITY TRUE");
            Db.executeScript(conn, "/db/schema.sql");
            Db.executeScript(conn, "/db/data.sql");
        } catch (Exception e) {
            throw new IllegalStateException("重置测试数据失败", e);
        }
        DatabaseInitializer.generateAttendance();
    }

    /**
     * 重置数据并以管理员身份登录，供需要管理员权限的用例在 {@code @BeforeEach} 中调用。
     */
    protected void resetDatabaseAndLoginAsAdmin() {
        resetDataOnly();
        new com.course.sims.service.AuthService().login("admin", "123456");
    }

    /**
     * 执行单值标量查询。
     *
     * @param sql    查询语句
     * @param params 参数
     * @return 整数值，无结果返回 0
     */
    protected int scalarInt(String sql, Object... params) {
        try (Connection conn = Db.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            Db.bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (Exception e) {
            throw new IllegalStateException("标量查询失败：" + sql, e);
        }
    }

    /**
     * 执行单值标量查询（双精度）。
     *
     * @param sql    查询语句
     * @param params 参数
     * @return 数值，无结果返回 0
     */
    protected double scalarDouble(String sql, Object... params) {
        try (Connection conn = Db.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            Db.bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0D;
            }
        } catch (Exception e) {
            throw new IllegalStateException("标量查询失败：" + sql, e);
        }
    }

    /**
     * 执行单值标量查询（字符串）。
     *
     * @param sql    查询语句
     * @param params 参数
     * @return 字符串值，无结果返回 null
     */
    protected String scalarString(String sql, Object... params) {
        try (Connection conn = Db.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            Db.bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        } catch (Exception e) {
            throw new IllegalStateException("标量查询失败：" + sql, e);
        }
    }

    /**
     * 清空成绩表（用例内构造前置条件使用）。
     */
    protected void clearScores() {
        try (Connection conn = Db.getConnection(); Statement st = conn.createStatement()) {
            st.execute("DELETE FROM score");
        } catch (Exception e) {
            throw new IllegalStateException("清空成绩表失败", e);
        }
    }

    /**
     * 断言业务异常被抛出。
     *
     * <p>说明：本类不再提供 assertEq / assertTrue 等断言别名。原因是基类方法会被子类继承，
     * 若别名参数顺序与 JUnit 5 的断言方法不同（例如 assertTrue(String, boolean) 与
     * assertTrue(boolean, String)），会造成重载解析冲突，编译期即报错。
     * 各测试类统一直接使用 {@code org.junit.jupiter.api.Assertions} 的标准断言。</p>
     *
     * @param message 说明
     * @param action  待执行动作
     * @return 捕获到的业务异常
     */
    protected com.course.sims.service.BusinessException assertBizError(String message, Runnable action) {
        return org.junit.jupiter.api.Assertions.assertThrows(
                com.course.sims.service.BusinessException.class, action::run, message);
    }
}
