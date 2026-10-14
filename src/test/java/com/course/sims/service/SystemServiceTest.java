package com.course.sims.service;

import com.course.sims.BaseTest;
import com.course.sims.config.Session;
import com.course.sims.entity.Config;
import com.course.sims.entity.SysLog;
import com.course.sims.util.PageResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 系统日志与系统维护测试，覆盖模块 M9（系统日志）与模块 M10（系统维护与配置）。
 *
 * <p>注意：本类包含"重置演示数据"用例，会把测试数据库恢复为初始状态，
 * 因此每个用例内部都自带前置数据准备，不依赖其它用例的副作用。</p>
 *
 * @author 课程设计第 2 小组 - 成员 E
 * @version 1.0.0
 */
@DisplayName("模块 M9 / M10：系统日志、参数与数据库维护测试")
class SystemServiceTest extends BaseTest {

    private final SystemService systemService = new SystemService();

    // ------------------------------------------------------------------
    //  TC-H-01 ~ TC-H-04  系统参数
    // ------------------------------------------------------------------

    @Test
    @DisplayName("TC-H-01 系统参数：演示数据 8 项参数，按 key 读取正确")
    void testFindAllConfigs() {
        List<Config> configs = systemService.findAllConfigs();
        assertEquals(8, configs.size(), "演示数据应有 8 项系统参数");
        assertEquals("学生信息管理系统", systemService.getConfig(Config.KEY_SYSTEM_NAME, ""), "系统名称参数不正确");
        assertEquals(60, systemService.getIntConfig(Config.KEY_PASS_LINE, 0), "及格线参数应为 60");
        assertEquals(3, systemService.getIntConfig(Config.KEY_ATTENDANCE_WARN, 0), "缺勤预警阈值应为 3");
        assertEquals("1.0.0", systemService.getConfig(Config.KEY_SYSTEM_VERSION, ""), "版本号参数不正确");
        assertEquals("默认值", systemService.getConfig("not.exist.key", "默认值"), "不存在参数应返回默认值");
    }

    @Test
    @DisplayName("TC-H-02 系统参数：修改参数后持久化生效，并可恢复原值")
    void testSaveConfig() {
        Session.login(new AuthService().login("admin", "123456"));
        systemService.saveConfig(new Config(Config.KEY_ATTENDANCE_WARN, "5", "缺勤预警次数"));
        assertEquals(5, systemService.getIntConfig(Config.KEY_ATTENDANCE_WARN, 0), "参数修改未生效");
        systemService.saveConfig(new Config(Config.KEY_ATTENDANCE_WARN, "3", "缺勤预警次数"));
        assertEquals(3, systemService.getIntConfig(Config.KEY_ATTENDANCE_WARN, 0), "参数恢复失败");
    }

    @Test
    @DisplayName("TC-H-03 系统参数：越界数值与空值应被拒绝")
    void testSaveConfigValidation() {
        Session.login(new AuthService().login("admin", "123456"));
        BusinessException e1 = assertBizError("及格线越界应被拒绝",
                () -> systemService.saveConfig(new Config(Config.KEY_PASS_LINE, "200", "及格分数线")));
        assertTrue(e1.getMessage().contains("取值应在"), "提示应说明取值范围");

        assertBizError("权重越界应被拒绝",
                () -> systemService.saveConfig(new Config(Config.KEY_USUAL_RATIO, "2", "平时成绩占比")));
        assertBizError("非数值应被拒绝",
                () -> systemService.saveConfig(new Config(Config.KEY_PAGE_SIZE, "abc", "每页条数")));
        assertBizError("参数值为空应被拒绝",
                () -> systemService.saveConfig(new Config(Config.KEY_PAGE_SIZE, "", "每页条数")));
    }

    @Test
    @DisplayName("TC-H-04 系统参数：非管理员不能修改参数")
    void testSaveConfigPermission() {
        Session.login(new AuthService().login("teacher", "123456"));
        BusinessException e = assertBizError("教师修改参数应被拒绝",
                () -> systemService.saveConfig(new Config(Config.KEY_PASS_LINE, "70", "及格分数线")));
        assertTrue(e.getMessage().contains("权限不足"), "提示应说明权限不足");
        Session.login(new AuthService().login("admin", "123456"));
    }

    // ------------------------------------------------------------------
    //  TC-H-05 ~ TC-H-08  系统日志
    // ------------------------------------------------------------------

    @Test
    @DisplayName("TC-H-05 操作日志：登录与业务操作自动留痕")
    void testLogWritten() {
        int before = systemService.countLogs();
        new AuthService().login("admin", "123456");
        int after = systemService.countLogs();
        assertTrue(after > before, "登录后应新增一条操作日志，前=" + before + " 后=" + after);
        List<SysLog> recent = systemService.recentLogs(5);
        assertTrue(recent.size() > 0, "最近日志不应为空");
        assertEquals("admin", recent.get(0).getUsername(), "最新一条日志应为当前登录用户");
        assertEquals("登录系统", recent.get(0).getOperation(), "最新一条日志操作类型应为登录系统");
    }

    @Test
    @DisplayName("TC-H-06 操作日志：新增学生等业务操作会写入日志")
    void testBusinessLog() {
        Session.login(new AuthService().login("admin", "123456"));
        int before = systemService.countLogs();
        var clazz = new TeachingService().findAllClasses().get(0);
        var student = new com.course.sims.entity.Student();
        student.setSno("2098001");
        student.setName("日志测试学生");
        student.setGender("男");
        student.setClassId(clazz.getId());
        student.setEnrollYear(2098);
        new StudentService().save(student);
        int after = systemService.countLogs();
        assertTrue(after > before, "新增学生应写入操作日志");
        List<SysLog> recent = systemService.recentLogs(1);
        assertEquals("新增学生", recent.get(0).getOperation(), "日志操作类型不正确");
        assertTrue(recent.get(0).getDetail().contains("2098001"), "日志详情应包含学号");
        new StudentService().delete(student.getId());
    }

    @Test
    @DisplayName("TC-H-07 操作日志：分页与关键字检索生效")
    void testFindLogs() {
        PageResult<SysLog> page = systemService.findLogs(null, null, null, 1, 10);
        assertTrue(page.getRows().size() <= 10, "每页条数限制未生效");
        PageResult<SysLog> keyword = systemService.findLogs("登录系统", null, null, 1, 20);
        for (SysLog log : keyword.getRows()) {
            assertTrue(log.getOperation().contains("登录系统") || log.getDetail().contains("登录系统")
                            || log.getUsername().contains("登录系统"),
                    "关键字检索结果不匹配：" + log.getOperation());
        }
        Date today = Date.valueOf(java.time.LocalDate.now());
        Date weekAgo = Date.valueOf(java.time.LocalDate.now().minusDays(7));
        PageResult<SysLog> byDate = systemService.findLogs(null, weekAgo, today, 1, 50);
        assertTrue(byDate.getTotal() >= 0, "按日期区间查询日志不应报错");
    }

    @Test
    @DisplayName("TC-H-08 操作日志：非管理员不能清空日志")
    void testClearLogsPermission() {
        Session.login(new AuthService().login("teacher", "123456"));
        BusinessException e = assertBizError("教师清空日志应被拒绝", () -> systemService.clearLogs());
        assertTrue(e.getMessage().contains("权限不足"), "提示应说明权限不足");
        Session.login(new AuthService().login("admin", "123456"));
    }

    // ------------------------------------------------------------------
    //  TC-H-09 ~ TC-H-12  数据库维护
    // ------------------------------------------------------------------

    @Test
    @DisplayName("TC-H-09 数据库备份：生成非空 SQL 脚本文件且包含建表语句")
    void testBackupDatabase(@TempDir Path tempDir) throws Exception {
        Session.login(new AuthService().login("admin", "123456"));
        File file = tempDir.resolve("backup-test.sql").toFile();
        String path = systemService.backupDatabase(file);
        assertTrue(file.exists(), "备份文件未生成：" + path);
        assertTrue(file.length() > 1000, "备份文件内容过小，可能备份失败");
        String content = Files.readString(file.toPath(), java.nio.charset.StandardCharsets.UTF_8)
                .toUpperCase(java.util.Locale.ROOT);
        assertTrue(content.contains("CREATE TABLE") || content.contains("CREATE MEMORY TABLE")
                        || content.contains("CREATE CACHED TABLE"),
                "备份脚本应包含建表语句");
        assertTrue(content.contains("STUDENT"), "备份脚本应包含学生表数据");
        assertTrue(systemService.defaultBackupFileName().matches("backup-\\d{8}-\\d{6}\\.sql"),
                "默认备份文件名格式不正确：" + systemService.defaultBackupFileName());
    }

    @Test
    @DisplayName("TC-H-10 数据库状态：返回表数量与各表记录数")
    void testDatabaseStatus() {
        List<String> status = systemService.databaseStatus();
        assertTrue(status.size() >= 12, "状态信息应包含数据库路径、表数量与 9 张表的记录数");
        boolean hasTableCount = false;
        boolean hasStudentCount = false;
        for (String line : status) {
            if (line.contains("数据表数量")) {
                hasTableCount = true;
            }
            if (line.contains("student") && line.contains("40")) {
                hasStudentCount = true;
            }
        }
        assertTrue(hasTableCount, "状态信息应包含数据表数量");
        assertTrue(hasStudentCount, "状态信息应包含学生表记录数 40");
    }

    @Test
    @DisplayName("TC-H-11 重置演示数据：清空业务数据后重新灌入初始演示数据")
    void testResetDemoData() {
        Session.login(new AuthService().login("admin", "123456"));
        // 先制造脏数据
        var clazz = new TeachingService().findAllClasses().get(0);
        var student = new com.course.sims.entity.Student();
        student.setSno("2097001");
        student.setName("待清理学生");
        student.setGender("男");
        student.setClassId(clazz.getId());
        student.setEnrollYear(2097);
        new StudentService().save(student);
        assertEquals(41, scalarInt("SELECT COUNT(*) FROM student"), "脏数据准备失败");

        String message = systemService.resetDemoData();
        assertTrue(message.contains("重置"), "应返回重置结果说明");
        assertEquals(40, scalarInt("SELECT COUNT(*) FROM student"), "重置后学生数应恢复为 40");
        assertEquals(5, scalarInt("SELECT COUNT(*) FROM major"), "重置后专业数应为 5");
        assertEquals(10, scalarInt("SELECT COUNT(*) FROM clazz"), "重置后班级数应为 10");
        assertEquals(10, scalarInt("SELECT COUNT(*) FROM course"), "重置后课程数应为 10");
        assertEquals(75, scalarInt("SELECT COUNT(*) FROM score"), "重置后成绩数应为 75");
        assertEquals(900, scalarInt("SELECT COUNT(*) FROM attendance"), "重置后考勤数应为 900");
        assertEquals(0, scalarInt("SELECT COUNT(*) FROM student WHERE sno = '2097001'"),
                "重置后脏数据应被清除");
        assertEquals(4, scalarInt("SELECT COUNT(*) FROM sys_user"), "重置后系统用户应恢复为 4 个");
    }

    @Test
    @DisplayName("TC-H-12 重置演示数据：非管理员无权执行")
    void testResetPermission() {
        Session.login(new AuthService().login("teacher", "123456"));
        BusinessException e = assertBizError("教师重置数据应被拒绝", () -> systemService.resetDemoData());
        assertTrue(e.getMessage().contains("权限不足"), "提示应说明权限不足");
        Session.login(new AuthService().login("admin", "123456"));
    }

    @Test
    @DisplayName("TC-H-13 重置后演示数据完整性：外键关联与关键业务规则仍成立")
    void testDataIntegrityAfterReset() {
        Session.login(new AuthService().login("admin", "123456"));
        // 每名学生都必须归属一个存在的班级（外键完整性）
        assertEquals(0, scalarInt("SELECT COUNT(*) FROM student s LEFT JOIN clazz c ON s.class_id = c.id "
                + "WHERE c.id IS NULL"), "存在未关联有效班级的学生");
        // 每条成绩都必须关联有效的学生与课程
        assertEquals(0, scalarInt("SELECT COUNT(*) FROM score sc LEFT JOIN student s ON sc.student_id = s.id "
                + "LEFT JOIN course c ON sc.course_id = c.id WHERE s.id IS NULL OR c.id IS NULL"),
                "存在无效的成绩关联");
        // 总评成绩计算列与 Java 算法一致
        assertEquals(0, scalarInt("SELECT COUNT(*) FROM score WHERE total_score IS NULL "
                + "AND (usual_score IS NOT NULL OR exam_score IS NOT NULL)"),
                "存在总评成绩未计算的记录");
        // 用户密码全部为 MD5 密文（32 位十六进制）
        assertEquals(0, scalarInt("SELECT COUNT(*) FROM sys_user WHERE LENGTH(password) <> 32"),
                "存在非 MD5 密文的用户密码");
        assertEquals(40, new StudentService().countAll(), "重置后学生总数应为 40");
    }
}
