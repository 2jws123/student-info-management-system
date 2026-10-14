package com.course.sims.service;

import com.course.sims.BaseTest;
import com.course.sims.entity.Clazz;
import com.course.sims.entity.Course;
import com.course.sims.entity.Major;
import com.course.sims.entity.Score;
import com.course.sims.util.PageResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 教学基础数据测试，覆盖模块 M3（班级与专业管理）与模块 M4（课程管理）。
 *
 * @author 课程设计第 2 小组 - 成员 E
 * @version 1.0.0
 */
@DisplayName("模块 M3 / M4：班级、专业与课程管理测试")
class TeachingServiceTest extends BaseTest {

    private final TeachingService teachingService = new TeachingService();

    /**
     * 构造一个测试专业。
     *
     * @param code 专业代码
     * @param name 专业名称
     * @return 已保存的专业
     */
    private Major newMajor(String code, String name) {
        Major major = new Major();
        major.setMajorCode(code);
        major.setMajorName(name);
        major.setDeptName("测试学院");
        major.setDuration(4);
        major.setRemark("单元测试数据");
        teachingService.saveMajor(major);
        return major;
    }

    /**
     * 构造一个测试班级。
     *
     * @param code    班级代码
     * @param name    班级名称
     * @param majorId 所属专业
     * @return 已保存的班级
     */
    private Clazz newClazz(String code, String name, Integer majorId) {
        Clazz clazz = new Clazz();
        clazz.setClassCode(code);
        clazz.setClassName(name);
        clazz.setMajorId(majorId);
        clazz.setGrade(2099);
        clazz.setHeadteacher("测试班主任");
        clazz.setCapacity(40);
        teachingService.saveClazz(clazz);
        return clazz;
    }

    /**
     * 构造一个测试课程。
     *
     * @param code 课程代码
     * @param name 课程名称
     * @return 已保存的课程
     */
    private Course newCourse(String code, String name) {
        Course course = new Course();
        course.setCourseCode(code);
        course.setCourseName(name);
        course.setCredit(3.0);
        course.setHours(48);
        course.setCourseType(Course.TYPE_REQUIRED);
        course.setAssessType(Course.ASSESS_EXAM);
        course.setTeacher("测试教师");
        course.setSemester("2099-2100-1");
        teachingService.saveCourse(course);
        return course;
    }

    // ------------------------------------------------------------------
    //  TC-C-01 ~ TC-C-05  专业管理
    // ------------------------------------------------------------------

    @Test
    @DisplayName("TC-C-01 专业列表：演示数据 5 个专业并可分页查询")
    void testFindMajors() {
        PageResult<Major> page = teachingService.findMajors(null, 1, 10);
        assertEquals(5L, page.getTotal(), "演示数据应有 5 个专业");
        assertTrue(page.getRows().get(0).getClassCount() >= 0, "应统计出专业下的班级数");
        PageResult<Major> keyword = teachingService.findMajors("软件", 1, 10);
        assertTrue(keyword.getTotal() >= 1, "按关键字检索专业应命中记录");
    }

    @Test
    @DisplayName("TC-C-02 新增专业：专业代码重复应被拒绝")
    void testAddDuplicateMajor() {
        Major exist = teachingService.findAllMajors().get(0);
        Major major = new Major();
        major.setMajorCode(exist.getMajorCode());
        major.setMajorName("重复代码专业");
        major.setDeptName("测试学院");
        major.setDuration(4);
        BusinessException e = assertBizError("专业代码重复应被拒绝", () -> teachingService.saveMajor(major));
        assertTrue(e.getMessage().contains("专业代码已存在"), "提示应说明专业代码已存在");
    }

    @Test
    @DisplayName("TC-C-03 新增专业：必填项与学制范围校验")
    void testAddMajorValidation() {
        Major noCode = new Major();
        noCode.setMajorName("缺代码专业");
        noCode.setDeptName("测试学院");
        noCode.setDuration(4);
        assertBizError("专业代码为空应被拒绝", () -> teachingService.saveMajor(noCode));

        Major badDuration = new Major();
        badDuration.setMajorCode("TEST-M-99");
        badDuration.setMajorName("学制非法专业");
        badDuration.setDeptName("测试学院");
        badDuration.setDuration(9);
        assertBizError("学制超出范围应被拒绝", () -> teachingService.saveMajor(badDuration));
    }

    @Test
    @DisplayName("TC-C-04 删除专业：专业下存在班级时应被拒绝")
    void testDeleteMajorWithClass() {
        Major major = newMajor("TEST-M-01", "测试专业一");
        Clazz clazz = newClazz("TEST-C-01", "测试班级一", major.getId());
        BusinessException e = assertBizError("专业下有班级时应拒绝删除",
                () -> teachingService.deleteMajor(major.getId()));
        assertTrue(e.getMessage().contains("班级"), "提示应说明该专业下还有班级");
        teachingService.deleteClazz(clazz.getId());
        teachingService.deleteMajor(major.getId());
        assertEquals(0, scalarInt("SELECT COUNT(*) FROM major WHERE major_code = 'TEST-M-01'"),
                "清理测试专业失败");
    }

    @Test
    @DisplayName("TC-C-05 修改专业：信息更新成功")
    void testUpdateMajor() {
        Major major = newMajor("TEST-M-02", "测试专业二");
        major.setMajorName("测试专业二（已改名）");
        major.setDuration(3);
        teachingService.saveMajor(major);
        assertEquals("测试专业二（已改名）",
                scalarString("SELECT major_name FROM major WHERE id = ?", major.getId()), "专业名称未更新");
        assertEquals(3, scalarInt("SELECT duration FROM major WHERE id = ?", major.getId()), "学制未更新");
        teachingService.deleteMajor(major.getId());
    }

    // ------------------------------------------------------------------
    //  TC-C-06 ~ TC-C-10  班级管理
    // ------------------------------------------------------------------

    @Test
    @DisplayName("TC-C-06 班级列表：演示数据 10 个班级并可联表查询专业名称")
    void testFindClasses() {
        PageResult<Clazz> page = teachingService.findClasses(null, null, null, 1, 20);
        assertEquals(10L, page.getTotal(), "演示数据应有 10 个班级");
        Clazz first = page.getRows().get(0);
        assertTrue(first.getMajorName() != null && !first.getMajorName().isBlank(), "应带出专业名称");
        assertTrue(first.getStudentCount() >= 0, "应统计出班级实际人数");
        assertTrue(first.getRemainCapacity() >= 0, "剩余容量不应为负数");
    }

    @Test
    @DisplayName("TC-C-07 新增班级：班级代码重复应被拒绝")
    void testAddDuplicateClazz() {
        Clazz exist = teachingService.findAllClasses().get(0);
        Clazz clazz = new Clazz();
        clazz.setClassCode(exist.getClassCode());
        clazz.setClassName("重复代码班级");
        clazz.setMajorId(exist.getMajorId());
        clazz.setGrade(2099);
        clazz.setCapacity(40);
        BusinessException e = assertBizError("班级代码重复应被拒绝", () -> teachingService.saveClazz(clazz));
        assertTrue(e.getMessage().contains("班级代码已存在"), "提示应说明班级代码已存在");
    }

    @Test
    @DisplayName("TC-C-08 新增班级：年级与容量非法应被拒绝")
    void testAddClazzValidation() {
        Major major = teachingService.findAllMajors().get(0);
        Clazz badGrade = new Clazz();
        badGrade.setClassCode("TEST-C-90");
        badGrade.setClassName("年级非法班级");
        badGrade.setMajorId(major.getId());
        badGrade.setGrade(1999);
        badGrade.setCapacity(40);
        assertBizError("年级非法应被拒绝", () -> teachingService.saveClazz(badGrade));

        Clazz badCapacity = new Clazz();
        badCapacity.setClassCode("TEST-C-91");
        badCapacity.setClassName("容量非法班级");
        badCapacity.setMajorId(major.getId());
        badCapacity.setGrade(2099);
        badCapacity.setCapacity(0);
        assertBizError("容量非法应被拒绝", () -> teachingService.saveClazz(badCapacity));
    }

    @Test
    @DisplayName("TC-C-09 删除班级：班级下存在学生时应被拒绝")
    void testDeleteClazzWithStudent() {
        Clazz exist = teachingService.findAllClasses().get(0);
        assertTrue(exist.getStudentCount() > 0, "演示班级应有学生，用例前置条件不成立");
        BusinessException e = assertBizError("班级下有学生时应拒绝删除",
                () -> teachingService.deleteClazz(exist.getId()));
        assertTrue(e.getMessage().contains("学生"), "提示应说明该班级下还有学生");
        assertEquals(1, scalarInt("SELECT COUNT(*) FROM clazz WHERE id = ?", exist.getId()),
                "删除被拒绝后班级应仍然存在");
    }

    @Test
    @DisplayName("TC-C-10 班级容量：修改容量小于当前人数应被拒绝")
    void testUpdateClazzCapacity() {
        Clazz exist = teachingService.findAllClasses().get(0);
        exist.setCapacity(1);
        BusinessException e = assertBizError("容量小于当前人数时应被拒绝",
                () -> teachingService.saveClazz(exist));
        assertTrue(e.getMessage().contains("容量"), "提示应说明容量不能小于当前人数");
    }

    // ------------------------------------------------------------------
    //  TC-C-11 ~ TC-C-15  课程管理
    // ------------------------------------------------------------------

    @Test
    @DisplayName("TC-C-11 课程列表：演示数据 10 门课程并可检索")
    void testFindCourses() {
        PageResult<Course> page = teachingService.findCourses(null, null, null, 1, 20);
        assertEquals(10L, page.getTotal(), "演示数据应有 10 门课程");
        PageResult<Course> required = teachingService.findCourses(null, Course.TYPE_REQUIRED, null, 1, 20);
        for (Course c : required.getRows()) {
            assertEquals(Course.TYPE_REQUIRED, c.getCourseType(), "课程类型筛选结果不正确");
        }
        PageResult<Course> java = teachingService.findCourses("Java", null, null, 1, 20);
        assertTrue(java.getTotal() >= 1, "按课程名称检索应命中 Java 程序设计");
    }

    @Test
    @DisplayName("TC-C-12 新增课程：课程代码重复应被拒绝")
    void testAddDuplicateCourse() {
        Course exist = teachingService.findAllCourses().get(0);
        Course course = new Course();
        course.setCourseCode(exist.getCourseCode());
        course.setCourseName("重复代码课程");
        course.setCredit(3.0);
        course.setHours(48);
        course.setCourseType(Course.TYPE_REQUIRED);
        course.setAssessType(Course.ASSESS_EXAM);
        BusinessException e = assertBizError("课程代码重复应被拒绝", () -> teachingService.saveCourse(course));
        assertTrue(e.getMessage().contains("课程代码已存在"), "提示应说明课程代码已存在");
    }

    @Test
    @DisplayName("TC-C-13 新增课程：学分与学时范围校验")
    void testAddCourseValidation() {
        Course badCredit = new Course();
        badCredit.setCourseCode("TEST-CO-91");
        badCredit.setCourseName("学分非法课程");
        badCredit.setCredit(20.0);
        badCredit.setHours(48);
        badCredit.setCourseType(Course.TYPE_REQUIRED);
        badCredit.setAssessType(Course.ASSESS_EXAM);
        assertTrue(assertBizError("学分超出范围应被拒绝", () -> teachingService.saveCourse(badCredit))
                .getMessage().contains("学分"), "提示应说明学分范围");

        Course badHours = new Course();
        badHours.setCourseCode("TEST-CO-92");
        badHours.setCourseName("学时非法课程");
        badHours.setCredit(3.0);
        badHours.setHours(4);
        badHours.setCourseType(Course.TYPE_REQUIRED);
        badHours.setAssessType(Course.ASSESS_EXAM);
        assertTrue(assertBizError("学时不合法应被拒绝", () -> teachingService.saveCourse(badHours))
                .getMessage().contains("学时"), "提示应说明学时范围");
    }

    @Test
    @DisplayName("TC-C-14 删除课程：存在成绩记录时应被拒绝，清理后可删除")
    void testDeleteCourseWithScore() {
        Course course = newCourse("TEST-CO-01", "测试课程一");
        var student = new com.course.sims.service.StudentService()
                .findByPage(null, null, null, null, 1, 1).getRows().get(0);
        Score score = new Score(student.getId(), course.getId(), "2099-2100-1");
        score.setUsualScore(80.0);
        score.setExamScore(90.0);
        new ScoreService().save(score);

        BusinessException e = assertBizError("课程存在成绩时应拒绝删除",
                () -> teachingService.deleteCourse(course.getId()));
        assertTrue(e.getMessage().contains("成绩"), "提示应说明该课程已有成绩记录");

        new ScoreService().delete(score.getId());
        teachingService.deleteCourse(course.getId());
        assertEquals(0, scalarInt("SELECT COUNT(*) FROM course WHERE course_code = 'TEST-CO-01'"),
                "清理测试课程失败");
    }

    @Test
    @DisplayName("TC-C-15 课程统计：课程数、总学分、必修课程数汇总正确")
    void testCourseSummary() {
        double[] summary = teachingService.courseSummary();
        assertEquals(10.0D, summary[0], 0.001D, "课程数应为 10");
        assertEquals(33.0D, summary[1], 0.001D, "10 门课程总学分应为 33.0");
        assertEquals(8.0D, summary[2], 0.001D, "必修课程应为 8 门");
        assertTrue(teachingService.findAllSemesters().size() >= 2, "应能查询到多个开课学期");
    }
}
