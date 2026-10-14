package com.course.sims.service;

import com.course.sims.BaseTest;
import com.course.sims.entity.Clazz;
import com.course.sims.entity.Score;
import com.course.sims.entity.Student;
import com.course.sims.util.PageResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 学生信息管理测试（模块 M2）。
 *
 * @author 课程设计第 2 小组 - 成员 E
 * @version 1.0.0
 */
@DisplayName("模块 M2：学生信息管理测试")
class StudentServiceTest extends BaseTest {

    private final StudentService studentService = new StudentService();
    private final TeachingService teachingService = new TeachingService();

    // ------------------------------------------------------------------
    //  TC-B-01 ~ TC-B-05  查询功能
    // ------------------------------------------------------------------

    @Test
    @DisplayName("TC-B-01 学生列表：分页查询返回总记录数与首页数据")
    void testFindByPage() {
        PageResult<Student> page = studentService.findByPage(null, null, null, null, 1, 15);
        assertEquals(40L, page.getTotal(), "演示数据应有 40 名学生");
        assertEquals(15, page.getRows().size(), "首页应返回 15 条记录");
        assertEquals(3, page.getTotalPages(), "40 条记录每页 15 条应为 3 页");
        Student first = page.getRows().get(0);
        assertNotNull(first.getClassName(), "列表应联表带出班级名称");
        assertNotNull(first.getMajorName(), "列表应联表带出专业名称");
    }

    @Test
    @DisplayName("TC-B-02 学生列表：第二页数据与第一页不重复")
    void testFindByPageSecondPage() {
        PageResult<Student> page1 = studentService.findByPage(null, null, null, null, 1, 15);
        PageResult<Student> page2 = studentService.findByPage(null, null, null, null, 2, 15);
        assertEquals(15, page2.getRows().size(), "第二页应返回 15 条记录");
        assertTrue(!page1.getRows().get(0).getSno().equals(page2.getRows().get(0).getSno()),
                "分页数据出现重复");
    }

    @Test
    @DisplayName("TC-B-03 学生列表：关键字（学号/姓名/手机号）检索生效")
    void testSearchKeyword() {
        PageResult<Student> bySno = studentService.findByPage("2024001", null, null, null, 1, 15);
        assertTrue(bySno.getTotal() >= 1, "按学号检索应命中记录");
        PageResult<Student> byName = studentService.findByPage("张伟", null, null, null, 1, 15);
        assertTrue(byName.getTotal() >= 1, "按姓名检索应命中记录");
        assertEquals("张伟", byName.getRows().get(0).getName(), "按姓名检索结果不正确");
        PageResult<Student> byPhone = studentService.findByPage("13910000001", null, null, null, 1, 15);
        assertTrue(byPhone.getTotal() >= 1, "按手机号检索应命中记录");
        PageResult<Student> none = studentService.findByPage("不存在的关键字XYZ", null, null, null, 1, 15);
        assertEquals(0L, none.getTotal(), "不存在的关键字应返回 0 条");
    }

    @Test
    @DisplayName("TC-B-04 学生列表：按班级与学籍状态筛选生效")
    void testSearchFilter() {
        Clazz clazz = teachingService.findAllClasses().get(0);
        PageResult<Student> byClass = studentService.findByPage(null, clazz.getId(), null, null, 1, 50);
        for (Student s : byClass.getRows()) {
            assertEquals(clazz.getId(), s.getClassId(), "按班级筛选结果不正确");
        }
        PageResult<Student> suspended = studentService.findByPage(null, null, Student.STATUS_SUSPENDED, null, 1, 50);
        for (Student s : suspended.getRows()) {
            assertEquals(Student.STATUS_SUSPENDED, s.getStatus(), "按学籍状态筛选结果不正确");
        }
        assertTrue(suspended.getTotal() >= 1, "演示数据中应存在休学学生");
    }

    @Test
    @DisplayName("TC-B-05 学号生成：格式为 7 位数字且不与现有学号重复")
    void testGenerateSno() {
        String sno = studentService.generateSno(2030);
        assertTrue(sno.matches("^\\d{7}$"), "生成的学号应为 7 位数字，实际：" + sno);
        assertTrue(sno.startsWith("2030"), "学号应以入学年份开头");
        assertEquals(0, scalarInt("SELECT COUNT(*) FROM student WHERE sno = ?", sno),
                "生成的学号不应与现有学号重复");
    }

    // ------------------------------------------------------------------
    //  TC-B-06 ~ TC-B-09  新增与校验
    // ------------------------------------------------------------------

    @Test
    @DisplayName("TC-B-06 新增学生：合法数据保存成功并可查询")
    void testAddStudent() {
        Clazz clazz = teachingService.findAllClasses().get(0);
        Student student = new Student();
        student.setSno("2099001");
        student.setName("测试学生甲");
        student.setGender("男");
        student.setBirthday(Date.valueOf("2006-01-01"));
        student.setClassId(clazz.getId());
        student.setPhone("13800001234");
        student.setEmail("test001@stu.edu.cn");
        student.setAddress("测试地址");
        student.setEnrollYear(2099);
        student.setStatus(Student.STATUS_STUDYING);
        studentService.save(student);
        assertTrue(student.getId() != null && student.getId() > 0, "新增学生应返回主键");
        assertEquals("测试学生甲",
                scalarString("SELECT name FROM student WHERE sno = '2099001'"), "新增学生未写入数据库");
        // 清理
        studentService.delete(student.getId());
        assertEquals(0, scalarInt("SELECT COUNT(*) FROM student WHERE sno = '2099001'"), "清理测试数据失败");
    }

    @Test
    @DisplayName("TC-B-07 新增学生：学号重复应被拒绝")
    void testAddDuplicateSno() {
        Clazz clazz = teachingService.findAllClasses().get(0);
        Student student = new Student();
        student.setSno("2024001");
        student.setName("重复学号学生");
        student.setGender("女");
        student.setClassId(clazz.getId());
        student.setEnrollYear(2024);
        BusinessException e = assertBizError("学号重复应被拒绝", () -> studentService.save(student));
        assertTrue(e.getMessage().contains("学号已存在"), "提示应说明学号已存在");
    }

    @Test
    @DisplayName("TC-B-08 新增学生：学号格式、手机号、邮箱格式非法应被拒绝")
    void testAddInvalidFormat() {
        Clazz clazz = teachingService.findAllClasses().get(0);
        Student badSno = new Student();
        badSno.setSno("2024");
        badSno.setName("学号非法");
        badSno.setGender("男");
        badSno.setClassId(clazz.getId());
        badSno.setEnrollYear(2024);
        assertTrue(assertBizError("学号格式非法应被拒绝", () -> studentService.save(badSno))
                .getMessage().contains("学号格式"), "提示应说明学号格式不正确");

        Student badPhone = new Student();
        badPhone.setSno("2099002");
        badPhone.setName("手机号非法");
        badPhone.setGender("男");
        badPhone.setClassId(clazz.getId());
        badPhone.setPhone("123");
        badPhone.setEnrollYear(2099);
        assertTrue(assertBizError("手机号格式非法应被拒绝", () -> studentService.save(badPhone))
                .getMessage().contains("手机号"), "提示应说明手机号格式不正确");

        Student badEmail = new Student();
        badEmail.setSno("2099003");
        badEmail.setName("邮箱非法");
        badEmail.setGender("男");
        badEmail.setClassId(clazz.getId());
        badEmail.setEmail("abc#def");
        badEmail.setEnrollYear(2099);
        assertTrue(assertBizError("邮箱格式非法应被拒绝", () -> studentService.save(badEmail))
                .getMessage().contains("邮箱"), "提示应说明邮箱格式不正确");

        Student noName = new Student();
        noName.setSno("2099004");
        noName.setName("");
        noName.setGender("男");
        noName.setClassId(clazz.getId());
        noName.setEnrollYear(2099);
        assertBizError("姓名为空应被拒绝", () -> studentService.save(noName));

        Student noClass = new Student();
        noClass.setSno("2099005");
        noClass.setName("无班级学生");
        noClass.setGender("男");
        noClass.setEnrollYear(2099);
        assertBizError("未选择班级应被拒绝", () -> studentService.save(noClass));
    }

    @Test
    @DisplayName("TC-B-09 新增学生：班级人数达到容量上限应被拒绝")
    void testClassCapacityLimit() {
        // 构造一个容量为 1 的测试班级
        Clazz clazz = new Clazz();
        clazz.setClassCode("TEST-CAP-01");
        clazz.setClassName("容量测试班");
        clazz.setMajorId(teachingService.findAllMajors().get(0).getId());
        clazz.setGrade(2099);
        clazz.setCapacity(1);
        teachingService.saveClazz(clazz);

        Student first = new Student();
        first.setSno("2099101");
        first.setName("容量测试学生1");
        first.setGender("男");
        first.setClassId(clazz.getId());
        first.setEnrollYear(2099);
        studentService.save(first);

        Student second = new Student();
        second.setSno("2099102");
        second.setName("容量测试学生2");
        second.setGender("女");
        second.setClassId(clazz.getId());
        second.setEnrollYear(2099);
        BusinessException e = assertBizError("班级满员时应拒绝新增学生", () -> studentService.save(second));
        assertTrue(e.getMessage().contains("容量"), "提示应说明容量已达上限");

        // 清理
        studentService.delete(first.getId());
        teachingService.deleteClazz(clazz.getId());
    }

    // ------------------------------------------------------------------
    //  TC-B-10 ~ TC-B-13  修改、删除与级联
    // ------------------------------------------------------------------

    @Test
    @DisplayName("TC-B-10 修改学生：信息更新成功且学号唯一性校验排除自身")
    void testUpdateStudent() {
        Clazz clazz = teachingService.findAllClasses().get(0);
        Student student = new Student();
        student.setSno("2099006");
        student.setName("待修改学生");
        student.setGender("男");
        student.setClassId(clazz.getId());
        student.setEnrollYear(2099);
        studentService.save(student);

        student.setName("已修改学生");
        student.setGender("女");
        studentService.save(student);
        assertEquals("已修改学生", scalarString("SELECT name FROM student WHERE sno = '2099006'"),
                "修改后姓名未更新");
        assertEquals("女", scalarString("SELECT gender FROM student WHERE sno = '2099006'"),
                "修改后性别未更新");
        studentService.delete(student.getId());
    }

    @Test
    @DisplayName("TC-B-11 删除学生：级联删除其成绩记录")
    void testDeleteStudentCascade() {
        clearScores();
        Clazz clazz = teachingService.findAllClasses().get(0);
        var courses = teachingService.findAllCourses();
        Student student = new Student();
        student.setSno("2099007");
        student.setName("级联删除测试");
        student.setGender("男");
        student.setClassId(clazz.getId());
        student.setEnrollYear(2099);
        studentService.save(student);

        Score score = new Score(student.getId(), courses.get(0).getId(), "2099-2100-1");
        score.setUsualScore(80.0);
        score.setExamScore(90.0);
        new ScoreService().save(score);
        assertEquals(1, scalarInt("SELECT COUNT(*) FROM score WHERE student_id = ?", student.getId()),
                "成绩未正确写入");

        studentService.delete(student.getId());
        assertEquals(0, scalarInt("SELECT COUNT(*) FROM score WHERE student_id = ?", student.getId()),
                "删除学生后其成绩记录应被级联删除");
        assertEquals(0, scalarInt("SELECT COUNT(*) FROM attendance WHERE student_id = ?", student.getId()),
                "删除学生后其考勤记录应被级联删除");
    }

    @Test
    @DisplayName("TC-B-12 删除学生：记录不存在时应给出明确提示")
    void testDeleteNotExist() {
        BusinessException e = assertBizError("删除不存在的学生应抛出业务异常",
                () -> studentService.delete(999999));
        assertTrue(e.getMessage().contains("不存在"), "提示应说明记录不存在");
    }

    @Test
    @DisplayName("TC-B-13 学生成绩单：汇总已修学分、加权平均分、平均绩点")
    void testTranscript() {
        StudentTranscript transcript = studentService.getTranscript(1, null);
        assertNotNull(transcript.getStudent(), "成绩单应包含学生信息");
        assertTrue(transcript.getScores().size() > 0, "演示学生应有成绩记录");
        assertTrue(transcript.getEarnedCredit() > 0, "已修学分应大于 0");
        assertTrue(transcript.getWeightedAverage() > 0 && transcript.getWeightedAverage() <= 100,
                "加权平均分应在合理范围，实际：" + transcript.getWeightedAverage());
        assertTrue(transcript.getGpa() >= 0 && transcript.getGpa() <= 4.0,
                "平均绩点应在 0~4.0 之间，实际：" + transcript.getGpa());
        // 按学期过滤
        StudentTranscript oneTerm = studentService.getTranscript(1, "2024-2025-1");
        assertEquals(5, oneTerm.getScores().size(), "第一学期应有 5 门课程成绩");
    }

    @Test
    @DisplayName("TC-B-15 学生成绩单：兼容传入学号（学生角色会话中保存的是学号）")
    void testTranscriptBySno() {
        Student byNo = studentService.findBySno("2024001");
        assertNotNull(byNo, "演示学号 2024001 应存在");
        // 正常路径：传入主键
        StudentTranscript byId = studentService.getTranscript(byNo.getId(), null);
        // 兼容路径：传入学号（例如界面误把 related_sno 当作主键传入）
        StudentTranscript bySno = studentService.getTranscript(Integer.valueOf(byNo.getSno()), null);
        assertEquals(byId.getScores().size(), bySno.getScores().size(),
                "按主键与按学号查询的成绩单课程数应一致");
        assertEquals(byId.getStudent().getSno(), bySno.getStudent().getSno(), "成绩单学生应一致");
        assertBizError("既不是主键也不是学号时应抛出业务异常",
                () -> studentService.getTranscript(999999, null));
    }

    @Test
    @DisplayName("TC-B-14 统计功能：班级人数与专业人数统计结果与明细一致")
    void testCountGroup() {
        var byClass = studentService.countGroupByClass();
        assertTrue(byClass.size() >= 10, "应能统计出每个班级的人数");
        int total = 0;
        for (var row : byClass) {
            total += ((Number) row.get("student_count")).intValue();
        }
        assertEquals(studentService.countAll(), total, "各班人数之和应等于学生总数");

        var byMajor = studentService.countGroupByMajor();
        assertTrue(byMajor.size() >= 5, "应能统计出每个专业的人数");
        List<Student> all = studentService.findList(null, null, null);
        assertEquals(all.size(), studentService.countAll(), "列表查询与统计总数应一致");
    }
}
