package com.course.sims.service;

import com.course.sims.BaseTest;
import com.course.sims.entity.User;
import com.course.sims.util.MD5Util;
import com.course.sims.util.PageResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 用户认证与权限管理测试（模块 M1）。
 *
 * @author 课程设计第 2 小组 - 成员 E
 * @version 1.0.0
 */
@DisplayName("模块 M1：用户认证与权限管理测试")
class AuthServiceTest extends BaseTest {

    private final AuthService authService = new AuthService();

    // ------------------------------------------------------------------
    //  TC-A-01 ~ TC-A-06  登录认证
    // ------------------------------------------------------------------

    @Test
    @DisplayName("TC-A-01 正常登录：管理员 admin / 123456 登录成功并建立会话")
    void testLoginSuccess() {
        User user = authService.login("admin", "123456");
        assertNotNull(user, "登录应返回用户对象");
        assertEquals("admin", user.getUsername(), "登录用户名不正确");
        assertEquals(User.ROLE_ADMIN, user.getRole(), "登录角色不正确");
        assertEquals(user.getUsername(), com.course.sims.config.Session.getUsername(), "会话未正确建立");
    }

    @Test
    @DisplayName("TC-A-02 异常登录：密码错误应抛出业务异常且不建立会话")
    void testLoginWrongPassword() {
        com.course.sims.config.Session.logout();
        BusinessException e = assertBizError("密码错误应抛出业务异常",
                () -> authService.login("admin", "wrong-password"));
        assertTrue(e.getMessage().contains("密码错误"), "提示信息应说明密码错误，实际：" + e.getMessage());
        assertEquals("", com.course.sims.config.Session.getRole(), "登录失败不应建立会话");
    }

    @Test
    @DisplayName("TC-A-03 异常登录：用户名不存在应抛出业务异常")
    void testLoginUnknownUser() {
        BusinessException e = assertBizError("用户名不存在应抛出业务异常",
                () -> authService.login("not-exist-user", "123456"));
        assertTrue(e.getMessage().contains("用户名不存在"), "提示信息应说明用户名不存在");
    }

    @Test
    @DisplayName("TC-A-04 异常登录：用户名为空应抛出业务异常")
    void testLoginBlankUsername() {
        assertBizError("用户名为空应抛出业务异常", () -> authService.login("   ", "123456"));
        assertBizError("密码为空应抛出业务异常", () -> authService.login("admin", ""));
    }

    @Test
    @DisplayName("TC-A-05 学生账号登录：t2024001 / 2024001（密码为学号）")
    void testStudentLogin() {
        User user = authService.login("t2024001", "2024001");
        assertEquals(User.ROLE_STUDENT, user.getRole(), "学生账号角色不正确");
        assertEquals("2024001", user.getRelatedSno(), "学生账号应关联学号");
        assertTrue(com.course.sims.config.Session.isStudent(), "会话角色应为学生");
    }

    @Test
    @DisplayName("TC-A-06 登录成功后最后登录时间被更新")
    void testLoginUpdatesLastLogin() {
        authService.login("teacher", "123456");
        String lastLogin = scalarString("SELECT last_login FROM sys_user WHERE username = 'teacher'");
        assertNotNull(lastLogin, "登录后应写入最后登录时间");
    }

    // ------------------------------------------------------------------
    //  TC-A-07 ~ TC-A-09  密码管理
    // ------------------------------------------------------------------

    @Test
    @DisplayName("TC-A-07 修改密码：原密码错误应被拒绝")
    void testChangePasswordWrongOld() {
        authService.login("teacher", "123456");
        Integer userId = scalarInt("SELECT id FROM sys_user WHERE username = 'teacher'");
        assertBizError("原密码错误应被拒绝",
                () -> authService.changePassword(userId, "wrong", "newpass123", "newpass123"));
    }

    @Test
    @DisplayName("TC-A-08 修改密码：两次新密码不一致应被拒绝")
    void testChangePasswordNotMatch() {
        authService.login("teacher", "123456");
        Integer userId = scalarInt("SELECT id FROM sys_user WHERE username = 'teacher'");
        assertBizError("两次输入不一致应被拒绝",
                () -> authService.changePassword(userId, "123456", "newpass123", "newpass124"));
    }

    @Test
    @DisplayName("TC-A-09 修改密码：新密码过短应被拒绝，合法密码可成功修改并可回滚")
    void testChangePasswordSuccessAndRollback() {
        authService.login("teacher", "123456");
        Integer userId = scalarInt("SELECT id FROM sys_user WHERE username = 'teacher'");
        assertBizError("新密码过短应被拒绝",
                () -> authService.changePassword(userId, "123456", "123", "123"));
        authService.changePassword(userId, "123456", "teacher888", "teacher888");
        String cipher = scalarString("SELECT password FROM sys_user WHERE username = 'teacher'");
        assertEquals(MD5Util.encrypt("teacher888"), cipher, "新密码未正确写入数据库");
        assertTrue(authService.login("teacher", "teacher888") != null, "新密码应能登录成功");
        // 回滚密码，保证其它用例可继续使用初始密码
        authService.changePassword(userId, "teacher888", "123456", "123456");
        assertEquals(MD5Util.encrypt("123456"),
                scalarString("SELECT password FROM sys_user WHERE username = 'teacher'"),
                "密码回滚失败");
    }

    // ------------------------------------------------------------------
    //  TC-A-10 ~ TC-A-15  账号维护与权限
    // ------------------------------------------------------------------

    /**
     * 保证测试账号 test_teacher_01 存在（先删除再新增），使"新增成功"与
     * "重复被拒"两个用例都有确定的初始状态，互不依赖执行顺序。
     */
    private void prepareTestUser() {
        Integer exist = scalarInt("SELECT COUNT(*) FROM sys_user WHERE username = 'test_teacher_01'");
        if (exist > 0) {
            try (var conn = com.course.sims.util.Db.getConnection(); var st = conn.createStatement()) {
                st.execute("DELETE FROM sys_user WHERE username = 'test_teacher_01'");
            } catch (Exception e) {
                throw new IllegalStateException("清理测试账号失败", e);
            }
        }
    }

    @Test
    @DisplayName("TC-A-10 新增用户：密码以 MD5 密文存储，不出现明文")
    void testSaveUser() {
        authService.login("admin", "123456");
        prepareTestUser();
        User user = new User();
        user.setUsername("test_teacher_01");
        user.setRealName("测试教师");
        user.setRole(User.ROLE_TEACHER);
        user.setPhone("13700000001");
        authService.saveUser(user, "test123456");
        String cipher = scalarString("SELECT password FROM sys_user WHERE username = 'test_teacher_01'");
        assertEquals(MD5Util.encrypt("test123456"), cipher, "密码未以 MD5 密文存储");
        assertFalse("test123456".equals(cipher), "数据库中不应出现明文密码");
    }

    @Test
    @DisplayName("TC-A-11 新增用户：用户名重复应被拒绝")
    void testSaveUserDuplicate() {
        authService.login("admin", "123456");
        prepareTestUser();
        // 先建一个已存在账号，再尝试用同名新增，验证唯一性校验
        User exist = new User();
        exist.setUsername("test_teacher_01");
        exist.setRealName("已存在用户");
        exist.setRole(User.ROLE_TEACHER);
        authService.saveUser(exist, "test123456");

        User duplicate = new User();
        duplicate.setUsername("test_teacher_01");
        duplicate.setRealName("重复用户");
        duplicate.setRole(User.ROLE_TEACHER);
        BusinessException e = assertBizError("用户名重复应被拒绝",
                () -> authService.saveUser(duplicate, "test123456"));
        assertTrue(e.getMessage().contains("已存在"), "提示应说明用户名已存在");
        assertEquals(1, scalarInt("SELECT COUNT(*) FROM sys_user WHERE username = ?", "test_teacher_01"),
                "重复新增被拒绝后，同名账号应仍只有 1 个");
    }

    @Test
    @DisplayName("TC-A-12 新增用户：必填项为空与密码过短应被拒绝")
    void testSaveUserValidation() {
        authService.login("admin", "123456");
        User noName = new User();
        noName.setUsername("test_teacher_02");
        noName.setRealName("");
        noName.setRole(User.ROLE_TEACHER);
        assertBizError("真实姓名为空应被拒绝", () -> authService.saveUser(noName, "test123456"));
        User shortPwd = new User();
        shortPwd.setUsername("test_teacher_03");
        shortPwd.setRealName("短密码用户");
        shortPwd.setRole(User.ROLE_TEACHER);
        assertBizError("密码过短应被拒绝", () -> authService.saveUser(shortPwd, "123"));
    }

    @Test
    @DisplayName("TC-A-13 权限控制：非管理员不能查询用户列表")
    void testFindUsersPermission() {
        authService.login("teacher", "123456");
        BusinessException e = assertBizError("教师查询用户列表应被拒绝",
                () -> authService.findUsers(null, null, null, 1, 15));
        assertTrue(e.getMessage().contains("权限不足"), "提示应说明权限不足");
        authService.login("admin", "123456");
        PageResult<User> page = authService.findUsers(null, null, null, 1, 15);
        assertTrue(page.getTotal() >= 4, "管理员应能查询到用户列表");
    }

    @Test
    @DisplayName("TC-A-14 用户列表：分页与角色筛选生效")
    void testFindUsersPaging() {
        authService.login("admin", "123456");
        PageResult<User> page = authService.findUsers(null, User.ROLE_TEACHER, null, 1, 2);
        assertTrue(page.getRows().size() <= 2, "每页条数限制未生效");
        for (User u : page.getRows()) {
            assertEquals(User.ROLE_TEACHER, u.getRole(), "角色筛选结果不正确");
        }
        PageResult<User> keyword = authService.findUsers("admin", null, null, 1, 15);
        assertTrue(keyword.getTotal() >= 1, "关键字检索应能命中 admin");
    }

    @Test
    @DisplayName("TC-A-15 删除用户：不能删除当前登录账号，可删除其它测试账号")
    void testDeleteUser() {
        authService.login("admin", "123456");
        Integer adminId = scalarInt("SELECT id FROM sys_user WHERE username = 'admin'");
        assertBizError("不能删除当前登录账号", () -> authService.deleteUser(adminId));

        Integer testId = scalarInt("SELECT id FROM sys_user WHERE username = 'test_teacher_01'");
        if (testId > 0) {
            authService.deleteUser(testId);
            assertEquals(0, scalarInt("SELECT COUNT(*) FROM sys_user WHERE username = 'test_teacher_01'"),
                    "测试用户应已被删除");
        }
        authService.login("admin", "123456");
    }
}
