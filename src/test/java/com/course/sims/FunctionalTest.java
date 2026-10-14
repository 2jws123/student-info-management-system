package com.course.sims;

import com.course.sims.config.Session;
import com.course.sims.entity.Attendance;
import com.course.sims.entity.Clazz;
import com.course.sims.entity.Config;
import com.course.sims.entity.Course;
import com.course.sims.entity.Score;
import com.course.sims.entity.Student;
import com.course.sims.entity.User;
import com.course.sims.service.AttendanceService;
import com.course.sims.service.AuthService;
import com.course.sims.service.DashboardStats;
import com.course.sims.service.ReportService;
import com.course.sims.service.ScoreService;
import com.course.sims.service.StatsService;
import com.course.sims.service.StudentService;
import com.course.sims.service.SystemService;
import com.course.sims.service.TeachingService;
import com.course.sims.util.Db;
import com.course.sims.util.MD5Util;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 系统整体功能性测试（端到端业务流程）。
 *
 * <p>本类按"业务闭环"串联多个模块，验证跨模块协作是否正确，例如：
 * 登录 → 建专业 → 建班级 → 建学生 → 建课程 → 录入成绩 → 登记考勤
 * → 统计报表 → 导出 Excel 的完整链路。这类用例用于课程设计的"系统测试"环节，
 * 与各模块的单元测试互为补充。</p>
 *
 * <p>由于测试会修改数据，每个用例开始前先把数据库恢复为初始演示数据，
 * 保证用例之间互不影响、可重复执行。</p>
 *
 * @author 课程设计第 2 小组 - 成员 E
 * @version 1.0.0
 */
@DisplayName("系统整体功能性测试（端到端业务流程）")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FunctionalTest extends BaseTest {

    private static final AuthService AUTH = new AuthService();
    private static final StudentService STUDENT = new StudentService();
    private static final TeachingService TEACHING = new TeachingService();
    private static final ScoreService SCORE = new ScoreService();
    private static final AttendanceService ATTENDANCE = new AttendanceService();
    private static final StatsService STATS = new StatsService();
    private static final ReportService REPORT = new ReportService();
    private static final SystemService SYSTEM = new SystemService();

    /**
     * 每个用例前重置演示数据并以管理员身份登录。
     * <p>覆盖基类的 {@code @BeforeEach}：基类先重置数据，此处随后登录，
     * 保证会话对应的用户记录确实存在于当前数据库中（数据重置后旧会话即失效）。</p>
     */
    @Override
    @org.junit.jupiter.api.BeforeEach
    void resetDatabase() {
        resetDatabaseAndLoginAsAdmin();
    }

    @AfterAll
    static void restore() {
        // 测试结束后注销会话，避免影响后续手工演示
        Session.logout();
    }

    // ------------------------------------------------------------------
    //  TC-I-01  完整业务闭环
    // ------------------------------------------------------------------

    @Test
    @Order(1)
    @DisplayName("TC-I-01 业务流程闭环：建专业→建班级→建学生→建课程→录成绩→登记考勤→统计→导出")
    void testFullBusinessFlow(@TempDir Path tempDir) {
        resetDatabaseAndLoginAsAdmin();

        // ① 新建专业
        var major = new com.course.sims.entity.Major();
        major.setMajorCode("999999");
        major.setMajorName("系统测试专业");
        major.setDeptName("测试学院");
        major.setDuration(4);
        TEACHING.saveMajor(major);
        assertTrue(major.getId() > 0, "① 新建专业失败");

        // ② 新建班级（挂在新建专业下）
        Clazz clazz = new Clazz();
        clazz.setClassCode("TEST-2025-1");
        clazz.setClassName("系统测试2025级1班");
        clazz.setMajorId(major.getId());
        clazz.setGrade(2025);
        clazz.setHeadteacher("测试班主任");
        clazz.setCapacity(10);
        TEACHING.saveClazz(clazz);
        assertTrue(clazz.getId() > 0, "② 新建班级失败");

        // ③ 新生入学 5 人（学号自动生成）
        List<Student> students = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Student s = new Student();
            s.setSno(STUDENT.generateSno(2025));
            s.setName("测试学生" + (i + 1));
            s.setGender(i % 2 == 0 ? "男" : "女");
            s.setClassId(clazz.getId());
            s.setPhone("1380000100" + i);
            s.setEmail("test" + i + "@stu.edu.cn");
            s.setEnrollYear(2025);
            s.setStatus(Student.STATUS_STUDYING);
            STUDENT.save(s);
            students.add(s);
        }
        assertEquals(5, STUDENT.findByClass(clazz.getId()).size(), "③ 新生入学 5 人失败");

        // ④ 新建课程
        Course course = new Course();
        course.setCourseCode("TEST-CO-99");
        course.setCourseName("系统测试课程");
        course.setCredit(3.0);
        course.setHours(48);
        course.setCourseType(Course.TYPE_REQUIRED);
        course.setAssessType(Course.ASSESS_EXAM);
        course.setTeacher("测试教师");
        course.setSemester("2025-2026-1");
        TEACHING.saveCourse(course);
        assertTrue(course.getId() > 0, "④ 新建课程失败");

        // ⑤ 批量录入整班成绩
        List<Object[]> scoreRows = new ArrayList<>();
        for (int i = 0; i < students.size(); i++) {
            scoreRows.add(new Object[]{students.get(i).getId(), 80.0 + i, 70.0 + i * 2, null, "系统测试"});
        }
        assertEquals(5, SCORE.batchSave("2025-2026-1", course.getId(), scoreRows), "⑤ 批量录入成绩失败");
        List<Score> saved = SCORE.findByCourse(course.getId(), clazz.getId());
        assertEquals(5, saved.size(), "⑤ 成绩记录数不正确");
        for (Score s : saved) {
            assertNotNull(s.getTotalScore(), "⑤ 总评成绩未自动计算");
            assertEquals(s.calcTotal(), s.getTotalScore(), 0.05D, "⑤ 总评成绩与 Java 算法不一致");
        }

        // ⑥ 批量登记整班考勤（一次性全勤）
        var attRows = ATTENDANCE.buildDefaultRows(clazz.getId(), course.getId(),
                java.sql.Date.valueOf(java.time.LocalDate.now().minusDays(1)));
        assertEquals(5, ATTENDANCE.batchSave(course.getId(),
                java.sql.Date.valueOf(java.time.LocalDate.now().minusDays(1)), attRows), "⑥ 批量登记考勤失败");
        for (Student s : students) {
            assertEquals(100.0D, ATTENDANCE.attendanceRate(s.getId()), 0.001D, "⑥ 全勤学生出勤率应为 100%");
        }

        // ⑦ 统计与看板
        DashboardStats stats = STATS.dashboard();
        assertEquals(45, stats.getStudentTotal(), "⑦ 学生总数应为 40 + 5");
        assertEquals(11, stats.getClassTotal(), "⑦ 班级总数应为 10 + 1");
        assertEquals(6, stats.getMajorTotal(), "⑦ 专业总数应为 5 + 1");
        assertEquals(11, stats.getCourseTotal(), "⑦ 课程总数应为 10 + 1");
        assertEquals(80, stats.getScoreTotal(), "⑦ 成绩记录数应为 75 + 5");
        assertEquals(905, stats.getAttendanceTotal(), "⑦ 考勤记录数应为 900 + 5");
        List<Map<String, Object>> rank = STATS.studentRank(clazz.getId(), "2025-2026-1", 10);
        assertEquals(5, rank.size(), "⑦ 新班级成绩排名应包含 5 名学生");

        // ⑧ 导出报表（学生名册 + 成绩表 + 统计报表）
        File roster = tempDir.resolve("名册.xlsx").toFile();
        assertEquals(45, REPORT.exportStudents(roster, null, null, null), "⑧ 导出学生名册条数不正确");
        File scores = tempDir.resolve("成绩.xlsx").toFile();
        assertEquals(80, REPORT.exportScores(scores, null, null, null, null), "⑧ 导出成绩表条数不正确");
        File statsFile = tempDir.resolve("统计.xlsx").toFile();
        REPORT.exportStatistics(statsFile);
        assertTrue(roster.exists() && scores.exists() && statsFile.exists(), "⑧ 导出文件未全部生成");

        // ⑨ 数据清理与级联删除验证
        STUDENT.delete(students.get(0).getId());
        assertEquals(0, scalarInt("SELECT COUNT(*) FROM score WHERE student_id = ?", students.get(0).getId()),
                "⑨ 删除学生后成绩应被级联删除");
        assertEquals(4, SCORE.findByCourse(course.getId(), clazz.getId()).size(), "⑨ 剩余成绩数不正确");

        // ⑩ 恢复初始演示数据
        resetDatabaseAndLoginAsAdmin();
        assertEquals(40, STUDENT.countAll(), "⑩ 恢复演示数据失败");
    }

    // ------------------------------------------------------------------
    //  TC-I-02  权限矩阵端到端验证
    // ------------------------------------------------------------------

    @Test
    @Order(2)
    @DisplayName("TC-I-02 权限矩阵：四类角色可见模块与可执行操作符合设计")
    void testPermissionMatrix() {

        // 管理员：可见全部 10 个模块
        assertEquals(10, visibleModules().size(), "管理员应可见 10 个功能模块");
        assertTrue(Session.canMaintain(), "管理员应具备数据维护权限");
        assertTrue(Session.isAdmin(), "管理员角色判定错误");

        // 教师：可见 7 个模块（无班级管理、无用户管理、无系统维护）
        AUTH.login("teacher", "123456");
        List<String> teacherModules = visibleModules();
        assertEquals(7, teacherModules.size(), "教师应可见 7 个功能模块，实际：" + teacherModules);
        assertTrue(!teacherModules.contains("clazz"), "教师不应看到班级与专业管理");
        assertTrue(!teacherModules.contains("user"), "教师不应看到用户与权限管理");
        assertTrue(!teacherModules.contains("system"), "教师不应看到系统维护与日志");
        assertTrue(Session.isTeacher() && Session.canMaintain(), "教师角色判定错误");

        // 辅导员：可见 8 个模块
        AUTH.login("zs", "123456");
        List<String> counselorModules = visibleModules();
        assertEquals(8, counselorModules.size(), "辅导员应可见 8 个功能模块，实际：" + counselorModules);
        assertTrue(counselorModules.contains("clazz"), "辅导员应能看到班级与专业管理");
        assertTrue(!counselorModules.contains("user"), "辅导员不应看到用户管理");
        assertTrue(Session.isCounselor(), "辅导员角色判定错误");

        // 学生：仅可见 2 个模块（数据看板 + 成绩查询 + 考勤查询）
        AUTH.login("t2024001", "2024001");
        List<String> studentModules = visibleModules();
        assertTrue(studentModules.size() <= 3, "学生可见模块应不超过 3 个，实际：" + studentModules);
        assertTrue(!studentModules.contains("student"), "学生不应看到学生信息管理");
        assertTrue(!studentModules.contains("scoreEntry"), "学生不应看到成绩录入");
        assertTrue(!studentModules.contains("user"), "学生不应看到用户管理");
        assertTrue(!studentModules.contains("system"), "学生不应看到系统维护");
        assertTrue(!Session.canMaintain(), "学生不应具备数据维护权限");
        assertTrue(Session.isStudent(), "学生角色判定错误");
        assertEquals("2024001", Session.getRelatedSno(), "学生会话应关联本人学号");

        // 学生越权操作应被拒绝
        assertBizError("学生查询用户列表应被拒绝", () -> AUTH.findUsers(null, null, null, 1, 10));
        assertBizError("学生重置数据库应被拒绝", () -> SYSTEM.resetDemoData());

    }

    /**
     * 计算当前角色可见模块数量（与界面导航栏使用同一套规则）。
     *
     * @return 可见模块 Key 列表
     */
    private List<String> visibleModules() {
        List<String> keys = new ArrayList<>();
        for (Object[] module : com.course.sims.ui.MainFrame.MODULES) {
            String roles = String.valueOf(module[3]);
            if ("ALL".equals(roles) || roles.contains(Session.getRole())) {
                keys.add(String.valueOf(module[0]));
            }
        }
        return keys;
    }

    // ------------------------------------------------------------------
    //  TC-I-03  界面模块类可实例化性检查（无头环境跳过图形断言）
    // ------------------------------------------------------------------

    @Test
    @Order(3)
    @DisplayName("TC-I-03 界面装配：10 个功能模块类定义完整且面板类可被反射加载")
    void testModuleRegistry() {
        assertEquals(10, com.course.sims.ui.MainFrame.MODULES.length, "系统应定义 10 个功能模块");
        String[] expectedKeys = {"dashboard", "student", "clazz", "course", "scoreEntry", "scoreQuery",
                "attendance", "stats", "user", "system"};
        for (int i = 0; i < expectedKeys.length; i++) {
            assertEquals(expectedKeys[i], String.valueOf(com.course.sims.ui.MainFrame.MODULES[i][0]),
                    "第 " + (i + 1) + " 个模块 Key 不正确");
            Class<?> clazz = (Class<?>) com.course.sims.ui.MainFrame.MODULES[i][2];
            assertNotNull(clazz, "模块面板类不能为空");
            assertTrue(javax.swing.JPanel.class.isAssignableFrom(clazz),
                    "模块面板必须继承 JPanel：" + clazz.getName());
            try {
                clazz.getDeclaredConstructor();
            } catch (NoSuchMethodException e) {
                throw new AssertionError("模块面板缺少无参构造方法：" + clazz.getName());
            }
            assertTrue(clazz.getName().startsWith("com.course.sims.ui.panel."),
                    "模块面板应位于 ui.panel 包：" + clazz.getName());
        }
        // 会话相关辅助
        assertTrue(MD5Util.encrypt("123456").length() == 32, "密码密文应为 32 位");
    }

    /**
     * TC-I-08 界面装配冒烟测试：在无头环境下真实构造全部 10 个功能面板。
     *
     * <p>比"类可被反射加载"更强的验证：真正执行每个面板的构造方法（含 initUi →
     * configureColumns → buildQueryBar → loadData），从而验证界面层对 Service、
     * 实体 getter、公共组件的调用在运行期不会抛异常，且首次数据加载成功、
     * 表格中确实填充了演示数据。运行在 {@code java.awt.headless=true} 下，
     * 不弹出任何窗口。</p>
     */
    @Test
    @Order(8)
    @DisplayName("TC-I-08 界面装配冒烟：10 个面板真实构造 + 首次数据加载成功")
    void testPanelSmoke() {
        resetDatabaseAndLoginAsAdmin();
        StringBuilder report = new StringBuilder();
        int built = 0;
        int withData = 0;
        for (Object[] module : com.course.sims.ui.MainFrame.MODULES) {
            String key = String.valueOf(module[0]);
            String name = String.valueOf(module[1]);
            Class<?> clazz = (Class<?>) module[2];
            javax.swing.JPanel panel;
            try {
                panel = (javax.swing.JPanel) clazz.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new AssertionError("模块【" + name + "】(" + key + ") 面板构造失败："
                        + e.getClass().getSimpleName() + " - " + e.getMessage(), e);
            }
            assertNotNull(panel, "模块【" + name + "】面板为 null");
            assertTrue(panel.getComponentCount() > 0, "模块【" + name + "】面板未装配任何子组件");
            // 布局与绘制信息在无头环境下同样可计算，可用于发现布局装配错误
            panel.setSize(1280, 720);
            panel.doLayout();
            built++;

            // 统计面板中的表格行数：有数据的面板数量应不少于 70%
            int rows = countTableRows(panel, 0);
            if (rows > 0) {
                withData++;
            }
            report.append("\n  ").append(key).append("(").append(name).append(") 子组件 ")
                    .append(panel.getComponentCount()).append(" 个，表格数据行 ").append(rows).append(" 行");
        }
        assertEquals(10, built, "应成功构造 10 个功能面板");
        assertTrue(withData >= 7, "至少应有 7 个面板在首次加载后就显示数据，实际只有 " + withData + " 个");
        System.out.println("[界面冒烟测试] 10 个模块面板构造与首次加载结果：" + report);
    }

    /**
     * 递归统计容器内所有 JTable 的数据行数（用于界面冒烟测试）。
     *
     * @param container 容器
     * @param depth     递归深度保护
     * @return 表格数据行总数
     */
    private int countTableRows(java.awt.Container container, int depth) {
        if (depth > 12) {
            return 0;
        }
        int rows = 0;
        for (java.awt.Component component : container.getComponents()) {
            if (component instanceof javax.swing.JTable) {
                rows += ((javax.swing.JTable) component).getRowCount();
            } else if (component instanceof java.awt.Container) {
                rows += countTableRows((java.awt.Container) component, depth + 1);
            }
        }
        return rows;
    }

    /**
     * TC-I-09 学生角色只读界面冒烟测试。
     *
     * <p>验证学生角色登录后，其可见的每个面板都能真实构造并完成首次数据加载：
     * 成绩查询面板应只列出本人的成绩、考勤面板应只列出本人的考勤记录，
     * 且学生看不到任何管理类模块。</p>
     */
    @Test
    @Order(9)
    @DisplayName("TC-I-09 学生角色（只读）界面冒烟：可见面板可构造，成绩/考勤仅显示本人数据")
    void testStudentRolePanels() {
        resetDatabaseAndLoginAsAdmin();
        AUTH.login("t2024001", "2024001");
        assertTrue(Session.isStudent(), "会话应为学生角色");
        String sno = Session.getRelatedSno();
        assertEquals("2024001", sno, "学生会话应关联本人学号");

        List<String> keys = visibleModules();
        assertTrue(!keys.contains("student") && !keys.contains("scoreEntry")
                        && !keys.contains("user") && !keys.contains("system"),
                "学生不应看到管理类模块，实际：" + keys);

        boolean scoreChecked = false;
        boolean attendanceChecked = false;
        for (Object[] module : com.course.sims.ui.MainFrame.MODULES) {
            String key = String.valueOf(module[0]);
            if (!keys.contains(key)) {
                continue;
            }
            Class<?> clazz = (Class<?>) module[2];
            javax.swing.JPanel panel;
            try {
                panel = (javax.swing.JPanel) clazz.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new AssertionError("学生角色下模块【" + module[1] + "】构造失败："
                        + e.getClass().getSimpleName() + " - " + e.getMessage(), e);
            }
            assertTrue(panel.getComponentCount() > 0, "模块【" + module[1] + "】未装配子组件");
            panel.setSize(1280, 720);
            panel.doLayout();

            if ("scoreQuery".equals(key)) {
                List<javax.swing.JTable> tables = new ArrayList<>();
                collectTables(panel, tables);
                assertTrue(!tables.isEmpty(), "成绩查询面板应包含数据表格");
                javax.swing.JTable table = tables.get(0);
                assertTrue(table.getRowCount() > 0, "学生本人应有成绩记录");
                for (int i = 0; i < table.getRowCount(); i++) {
                    boolean found = false;
                    for (int c = 0; c < table.getColumnCount(); c++) {
                        Object v = table.getValueAt(i, c);
                        if (v != null && sno.equals(String.valueOf(v))) {
                            found = true;
                            break;
                        }
                    }
                    assertTrue(found, "成绩查询结果中出现了非本人学号的数据（第 " + (i + 1) + " 行）");
                }
                scoreChecked = true;
            }
            if ("attendance".equals(key)) {
                List<javax.swing.JTable> tables = new ArrayList<>();
                collectTables(panel, tables);
                assertTrue(!tables.isEmpty(), "考勤面板应包含数据表格");
                javax.swing.JTable table = tables.get(0);
                assertTrue(table.getRowCount() > 0, "学生本人应有考勤记录");
                for (int i = 0; i < table.getRowCount(); i++) {
                    boolean found = false;
                    for (int c = 0; c < table.getColumnCount(); c++) {
                        Object v = table.getValueAt(i, c);
                        if (v != null && sno.equals(String.valueOf(v))) {
                            found = true;
                            break;
                        }
                    }
                    assertTrue(found, "考勤查询结果中出现了非本人学号的数据（第 " + (i + 1) + " 行）");
                }
                attendanceChecked = true;
            }
        }
        assertTrue(scoreChecked, "学生角色应能看到成绩查询面板并完成数据加载");
        assertTrue(attendanceChecked, "学生角色应能看到考勤面板并完成数据加载");
        AUTH.login("admin", "123456");
    }

    /**
     * 递归收集容器内的全部 JTable（用于学生角色界面冒烟测试）。
     *
     * @param container 容器
     * @param result    结果列表
     */
    private void collectTables(java.awt.Container container, List<javax.swing.JTable> result) {
        for (java.awt.Component component : container.getComponents()) {
            if (component instanceof javax.swing.JTable) {
                result.add((javax.swing.JTable) component);
            } else if (component instanceof java.awt.Container) {
                collectTables((java.awt.Container) component, result);
            }
        }
    }

    // ------------------------------------------------------------------
    //  TC-I-04  数据一致性检查
    // ------------------------------------------------------------------

    @Test
    @Order(4)
    @DisplayName("TC-I-04 数据一致性：外键完整、唯一约束生效、统计口径自洽")
    void testDataConsistency() {
        resetDatabaseAndLoginAsAdmin();
        // 外键完整性
        assertEquals(0, scalarInt("SELECT COUNT(*) FROM student s LEFT JOIN clazz c ON s.class_id = c.id "
                + "WHERE c.id IS NULL"), "存在未关联有效班级的学生");
        assertEquals(0, scalarInt("SELECT COUNT(*) FROM clazz c LEFT JOIN major m ON c.major_id = m.id "
                + "WHERE m.id IS NULL"), "存在未关联有效专业的班级");
        assertEquals(0, scalarInt("SELECT COUNT(*) FROM score sc LEFT JOIN student s ON sc.student_id = s.id "
                + "LEFT JOIN course c ON sc.course_id = c.id WHERE s.id IS NULL OR c.id IS NULL"),
                "存在无效的成绩关联");
        // 唯一约束
        assertEquals(40, scalarInt("SELECT COUNT(DISTINCT sno) FROM student"), "学号存在重复");
        assertEquals(75, scalarInt("SELECT COUNT(*) FROM (SELECT student_id, course_id FROM score "
                + "GROUP BY student_id, course_id) t"), "存在重复的学生-课程成绩记录");
        // 统计口径自洽：看板数据与明细查询一致
        DashboardStats stats = STATS.dashboard();
        assertEquals(STUDENT.countAll(), stats.getStudentTotal(), "看板学生总数与明细不一致");
        assertEquals(SCORE.findList(null, null, null, null).size(), stats.getScoreTotal(),
                "看板成绩记录数与明细不一致");
        // 成绩与考勤的及格/出勤规则
        long failedByQuery = SCORE.findByPage(null, null, null, null, Boolean.FALSE, 1, 100).getTotal();
        assertEquals(failedByQuery, SCORE.failedList(null).size(), "不及格名单条数与筛选结果不一致");
        // 系统参数生效：及格线参与判定
        int passLine = SYSTEM.getIntConfig(Config.KEY_PASS_LINE, 60);
        assertEquals(60, passLine, "及格线参数应为 60");
        assertEquals(0, scalarInt("SELECT COUNT(*) FROM score WHERE total_score >= ? AND total_score < 60",
                passLine), "存在介于及格线以下却被统计为及格的记录");
    }

    // ------------------------------------------------------------------
    //  TC-I-05  异常与边界场景
    // ------------------------------------------------------------------

    @Test
    @Order(5)
    @DisplayName("TC-I-05 异常场景：核心业务规则被串联验证（重复学号/满员/重复成绩/重复考勤）")
    void testExceptionScenarios() {
        resetDatabaseAndLoginAsAdmin();
        Clazz clazz = TEACHING.findAllClasses().get(0);
        Course course = TEACHING.findAllCourses().get(0);
        Student student = STUDENT.findById(SCORE.findByPage(null, null, null, null, null, 1, 1)
                .getRows().get(0).getStudentId());

        // 重复学号
        Student dup = new Student();
        dup.setSno(student.getSno());
        dup.setName("重复学号");
        dup.setGender("男");
        dup.setClassId(clazz.getId());
        dup.setEnrollYear(student.getEnrollYear());
        assertBizError("重复学号应被拒绝", () -> STUDENT.save(dup));

        // 重复成绩
        Score dupScore = new Score(student.getId(), course.getId(), "2024-2025-1");
        dupScore.setUsualScore(90.0);
        dupScore.setExamScore(90.0);
        assertBizError("重复录入成绩应被拒绝", () -> SCORE.save(dupScore));

        // 重复考勤：以该学生真实存在的考勤记录为基准（日期与课程都必须一致才能触发唯一约束）
        List<Attendance> existing = ATTENDANCE.findByStudent(student.getId());
        assertTrue(!existing.isEmpty(), "演示学生应存在考勤记录，用例前置条件不成立");
        Attendance base = existing.get(0);
        Attendance dupAtt = new Attendance(student.getId(), base.getCourseId(), base.getAttDate(),
                Attendance.PRESENT);
        assertBizError("重复登记考勤应被拒绝", () -> ATTENDANCE.save(dupAtt));

        // 删除有学生的班级
        assertBizError("删除有学生的班级应被拒绝", () -> TEACHING.deleteClazz(clazz.getId()));
        // 删除有成绩的课程
        assertBizError("删除有成绩的课程应被拒绝", () -> TEACHING.deleteCourse(course.getId()));
        // 删除有班级的专业
        assertBizError("删除有班级的专业应被拒绝", () -> TEACHING.deleteMajor(clazz.getMajorId()));
        // 学生不存在
        assertBizError("查询不存在的学生应报错", () -> STUDENT.getTranscript(999999, null));
        // 参数越界
        assertBizError("及格线越界应被拒绝",
                () -> SYSTEM.saveConfig(new Config(Config.KEY_PASS_LINE, "150", "及格分数线")));
    }

    // ------------------------------------------------------------------
    //  TC-I-06  性能与容量
    // ------------------------------------------------------------------

    @Test
    @Order(6)
    @DisplayName("TC-I-06 性能验证：千级数据下列表查询、统计与导出响应时间达标")
    void testPerformance() {
        resetDatabaseAndLoginAsAdmin();
        // 造数据：为 40 名学生 × 10 门课程补齐成绩记录（跳过已存在的"学生-课程"组合，
        // 因为成绩表对 (student_id, course_id) 有唯一约束）
        long start = System.currentTimeMillis();
        List<Object[]> rows = new ArrayList<>();
        List<Student> students = STUDENT.findList(null, null, null);
        List<Course> courses = TEACHING.findAllCourses();
        for (Student s : students) {
            for (Course c : courses) {
                if (scalarInt("SELECT COUNT(*) FROM score WHERE student_id = ? AND course_id = ?",
                        s.getId(), c.getId()) > 0) {
                    continue;
                }
                rows.add(new Object[]{s.getId(), c.getId(), "2025-2026-1", 70.0 + (s.getId() % 20),
                        75.0 + (c.getId() % 20), null, Boolean.FALSE, "性能测试",
                        new java.sql.Timestamp(System.currentTimeMillis())});
            }
        }
        Db.batchUpdate("INSERT INTO score (student_id, course_id, semester, usual_score, exam_score, "
                + "makeup_score, is_retake, remark, create_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", rows);
        long insertCost = System.currentTimeMillis() - start;
        assertTrue(insertCost < 5000, "批量插入 400 条成绩耗时过长：" + insertCost + " ms");

        // 列表查询响应时间
        start = System.currentTimeMillis();
        var page = SCORE.findByPage(null, null, null, null, null, 1, 15);
        long queryCost = System.currentTimeMillis() - start;
        assertTrue(queryCost < 1000, "分页查询响应时间超标：" + queryCost + " ms");
        assertEquals(75L + rows.size(), page.getTotal(), "成绩总记录数应为 75 + 新增条数");

        // 统计查询响应时间
        start = System.currentTimeMillis();
        DashboardStats stats = STATS.dashboard();
        long statCost = System.currentTimeMillis() - start;
        assertTrue(statCost < 2000, "看板统计响应时间超标：" + statCost + " ms");
        assertTrue(stats.getAvgScore() > 0, "看板平均分应大于 0");

        // 排名与分布统计响应时间
        start = System.currentTimeMillis();
        assertTrue(STATS.studentRank(null, "2025-2026-1", 20).size() == 20, "排名查询应返回 20 条");
        assertTrue(STATS.courseAverageStat("2025-2026-1").size() == 10, "课程统计应覆盖 10 门课程");
        long rankCost = System.currentTimeMillis() - start;
        assertTrue(rankCost < 2000, "排名与分布统计响应时间超标：" + rankCost + " ms");

    }

    // ------------------------------------------------------------------
    //  TC-I-07  报表导出内容抽查（人工可复核的明细）
    // ------------------------------------------------------------------

    @Test
    @Order(7)
    @DisplayName("TC-I-07 导出内容抽查：Excel 内容与界面筛选条件一致（可人工复核）")
    void testExportContent(@TempDir Path tempDir) throws Exception {
        resetDatabaseAndLoginAsAdmin();
        File file = tempDir.resolve("抽查-软件技术2024级1班.xlsx").toFile();
        Clazz clazz = null;
        for (Clazz c : TEACHING.findAllClasses()) {
            if ("RJ2024-1".equals(c.getClassCode())) {
                clazz = c;
                break;
            }
        }
        assertNotNull(clazz, "未找到演示班级 RJ2024-1");
        int count = REPORT.exportStudents(file, null, clazz.getId(), null);
        assertEquals(10, count, "软件技术2024级1班应有 10 名学生");
        List<String[]> rows = com.course.sims.util.ExcelUtil.read(file, true);
        assertEquals(10, rows.size(), "导出明细行数不正确");
        assertEquals("2024001", rows.get(0)[0], "第一位学生学号应为 2024001");
        for (String[] row : rows) {
            assertEquals(clazz.getClassName(), row[4], "导出内容与筛选班级不一致");
        }
    }
}
