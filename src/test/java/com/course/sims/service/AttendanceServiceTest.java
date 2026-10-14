package com.course.sims.service;

import com.course.sims.BaseTest;
import com.course.sims.entity.Attendance;
import com.course.sims.entity.Course;
import com.course.sims.entity.Student;
import com.course.sims.util.PageResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 考勤管理测试（模块 M6）。
 *
 * @author 课程设计第 2 小组 - 成员 E
 * @version 1.0.0
 */
@DisplayName("模块 M6：考勤管理测试")
class AttendanceServiceTest extends BaseTest {

    private final AttendanceService attendanceService = new AttendanceService();
    private final StudentService studentService = new StudentService();
    private final TeachingService teachingService = new TeachingService();

    /**
     * 取一名演示学生。
     *
     * @return 学生对象
     */
    /**
     * 取第一名"有考勤记录"的学生，避免用例依赖学号排序。
     *
     * <p>演示数据只为前 15 名学生生成了考勤，直接取学生列表第一人可能没有考勤记录，
     * 因此统一从考勤表中反查学生，保证用例与演示数据的具体分布解耦。</p>
     *
     * @return 学生对象
     */
    private Student anyStudent() {
        Attendance first = attendanceService.findByPage(null, null, null, null, null, null, 1, 1)
                .getRows().get(0);
        return studentService.findById(first.getStudentId());
    }

    /**
     * 取一门演示课程。
     *
     * @return 课程对象
     */
    private Course anyCourse() {
        return teachingService.findAllCourses().get(0);
    }

    /**
     * 取第一个"确实有学生"的班级。
     *
     * <p>班级列表按 年级倒序 + 班级代码 排列，演示数据中部分班级没有学生，
     * 因此用例统一使用有学生的班级，避免依赖具体排序。</p>
     *
     * @return 班级对象
     */
    private com.course.sims.entity.Clazz anyClassWithStudents() {
        for (com.course.sims.entity.Clazz clazz : teachingService.findAllClasses()) {
            if (clazz.getStudentCount() > 0) {
                return clazz;
            }
        }
        throw new IllegalStateException("演示数据中不存在有学生的班级");
    }

    // ------------------------------------------------------------------
    //  TC-E-01 ~ TC-E-04  查询与统计
    // ------------------------------------------------------------------

    @Test
    @DisplayName("TC-E-01 考勤列表：初始化自动生成考勤记录，分页查询正常")
    void testFindByPage() {
        PageResult<Attendance> page = attendanceService.findByPage(null, null, null, null, null, null, 1, 20);
        assertEquals(900L, page.getTotal(), "按 15 名学生 × 3 门课程 × 20 周应生成 900 条考勤记录");
        assertEquals(20, page.getRows().size(), "首页应返回 20 条");
        Attendance first = page.getRows().get(0);
        assertNotNull(first.getSno(), "应联表带出学号");
        assertNotNull(first.getStudentName(), "应联表带出学生姓名");
        assertNotNull(first.getCourseName(), "应联表带出课程名称");
    }

    @Test
    @DisplayName("TC-E-02 考勤列表：按状态、课程、日期区间筛选生效")
    void testFilter() {
        PageResult<Attendance> absent = attendanceService.findByPage(null, null, null,
                Attendance.ABSENT, null, null, 1, 50);
        for (Attendance a : absent.getRows()) {
            assertEquals(Attendance.ABSENT, a.getStatus(), "状态筛选结果不正确");
        }
        Course course = anyCourse();
        PageResult<Attendance> byCourse = attendanceService.findByPage(null, null, course.getId(),
                null, null, null, 1, 50);
        for (Attendance a : byCourse.getRows()) {
            assertEquals(course.getId(), a.getCourseId(), "课程筛选结果不正确");
        }
        Date start = Date.valueOf(LocalDate.now().minusWeeks(2));
        Date end = Date.valueOf(LocalDate.now());
        PageResult<Attendance> byDate = attendanceService.findByPage(null, null, null, null, start, end, 1, 50);
        for (Attendance a : byDate.getRows()) {
            assertTrue(!a.getAttDate().before(start) && !a.getAttDate().after(end),
                    "日期区间筛选结果不正确：" + a.getAttDate());
        }
    }

    @Test
    @DisplayName("TC-E-03 出勤率计算：结果在 0~100 之间且与明细一致")
    void testAttendanceRate() {
        Student student = anyStudent();
        double rate = attendanceService.attendanceRate(student.getId());
        assertTrue(rate >= 0 && rate <= 100, "出勤率应在 0~100 之间，实际：" + rate);
        int total = scalarInt("SELECT COUNT(*) FROM attendance WHERE student_id = ?", student.getId());
        int present = scalarInt("SELECT COUNT(*) FROM attendance WHERE student_id = ? AND status = '出勤'",
                student.getId());
        double expected = Math.round(100D * present / total * 10D) / 10D;
        assertEquals(expected, rate, 0.001D, "出勤率与明细计算不一致");
    }

    @Test
    @DisplayName("TC-E-04 出勤率计算：无考勤记录的学生返回 100%")
    void testAttendanceRateNoRecord() {
        assertEquals(100.0D, attendanceService.attendanceRate(999999), 0.001D,
                "无考勤记录时应默认为 100%");
    }

    @Test
    @DisplayName("TC-E-05 考勤状态统计：各状态次数之和等于考勤总数")
    void testCountGroupByStatus() {
        List<Map<String, Object>> stat = attendanceService.countGroupByStatus();
        assertTrue(stat.size() >= 3, "演示数据中至少应有出勤、迟到、缺勤等状态");
        int sum = 0;
        for (Map<String, Object> row : stat) {
            sum += ((Number) row.get("num")).intValue();
            assertNotNull(row.get("status"), "统计结果应包含状态名称");
        }
        assertEquals(attendanceService.countAll(), sum, "各状态次数之和应等于考勤总记录数");
    }

    @Test
    @DisplayName("TC-E-06 班级出勤率统计：每个班级的百分比均在合理范围")
    void testClassAttendanceRate() {
        List<Map<String, Object>> list = attendanceService.classAttendanceRate();
        assertTrue(list.size() > 0, "应能按班级统计出勤率");
        for (Map<String, Object> row : list) {
            double rate = ((Number) row.get("rate")).doubleValue();
            assertTrue(rate >= 0 && rate <= 100, "班级出勤率超出范围：" + rate);
        }
    }

    // ------------------------------------------------------------------
    //  TC-E-07 ~ TC-E-12  登记与校验
    // ------------------------------------------------------------------

    @Test
    @DisplayName("TC-E-07 登记考勤：合法数据保存成功")
    void testSaveAttendance() {
        Student student = anyStudent();
        Course course = anyCourse();
        Date date = Date.valueOf(LocalDate.now().minusDays(1));
        // 先清理可能存在的同键记录
        try (var conn = com.course.sims.util.Db.getConnection(); var st = conn.createStatement()) {
            st.execute("DELETE FROM attendance WHERE student_id = " + student.getId()
                    + " AND course_id = " + course.getId() + " AND att_date = '" + date + "'");
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        Attendance a = new Attendance(student.getId(), course.getId(), date, Attendance.SICK_LEAVE);
        a.setRemark("单元测试病假");
        attendanceService.save(a);
        assertTrue(a.getId() > 0, "登记考勤应返回主键");
        assertEquals(Attendance.SICK_LEAVE,
                scalarString("SELECT status FROM attendance WHERE id = ?", a.getId()), "考勤状态未正确写入");
        attendanceService.delete(a.getId());
    }

    @Test
    @DisplayName("TC-E-08 登记考勤：同一学生同一课程同一天重复登记应被拒绝")
    void testDuplicateAttendance() {
        Student student = anyStudent();
        Course course = anyCourse();
        Date date = Date.valueOf(LocalDate.now().minusDays(2));
        try (var conn = com.course.sims.util.Db.getConnection(); var st = conn.createStatement()) {
            st.execute("DELETE FROM attendance WHERE student_id = " + student.getId()
                    + " AND course_id = " + course.getId() + " AND att_date = '" + date + "'");
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        Attendance first = new Attendance(student.getId(), course.getId(), date, Attendance.PRESENT);
        attendanceService.save(first);
        Attendance second = new Attendance(student.getId(), course.getId(), date, Attendance.ABSENT);
        BusinessException e = assertBizError("重复登记应被拒绝", () -> attendanceService.save(second));
        assertTrue(e.getMessage().contains("已有考勤记录"), "提示应说明当日已有考勤记录");
        attendanceService.delete(first.getId());
    }

    @Test
    @DisplayName("TC-E-09 登记考勤：考勤日期晚于今天应被拒绝")
    void testFutureDateRejected() {
        Student student = anyStudent();
        Course course = anyCourse();
        Attendance a = new Attendance(student.getId(), course.getId(),
                Date.valueOf(LocalDate.now().plusDays(3)), Attendance.PRESENT);
        BusinessException e = assertBizError("未来日期应被拒绝", () -> attendanceService.save(a));
        assertTrue(e.getMessage().contains("不能晚于今天"), "提示应说明考勤日期不能晚于今天");
    }

    @Test
    @DisplayName("TC-E-10 登记考勤：考勤状态取值非法应被拒绝")
    void testInvalidStatusRejected() {
        Student student = anyStudent();
        Course course = anyCourse();
        Attendance a = new Attendance(student.getId(), course.getId(),
                Date.valueOf(LocalDate.now().minusDays(1)), "旷课");
        BusinessException e = assertBizError("非法状态应被拒绝", () -> attendanceService.save(a));
        assertTrue(e.getMessage().contains("不合法"), "提示应说明状态取值不合法");
    }

    @Test
    @DisplayName("TC-E-11 登记考勤：必填项缺失应被拒绝（课程/日期/状态）")
    void testRequiredValidation() {
        Student student = anyStudent();
        Course course = anyCourse();
        Attendance noCourse = new Attendance(student.getId(), null, Date.valueOf(LocalDate.now()), Attendance.PRESENT);
        assertBizError("未选择课程应被拒绝", () -> attendanceService.save(noCourse));

        Attendance noDate = new Attendance(student.getId(), course.getId(), null, Attendance.PRESENT);
        assertBizError("未选择日期应被拒绝", () -> attendanceService.save(noDate));

        Attendance noStatus = new Attendance(student.getId(), course.getId(),
                Date.valueOf(LocalDate.now().minusDays(1)), "");
        BusinessException e = assertBizError("未选择状态应被拒绝", () -> attendanceService.save(noStatus));
        assertTrue(e.getMessage().contains("考勤状态"), "提示应说明需要选择考勤状态");
    }

    @Test
    @DisplayName("TC-E-12 批量登记：整班一次登记成功，重复登记时更新而非新增")
    void testBatchSave() {
        Course course = anyCourse();
        Student student = anyStudent();
        Date date = Date.valueOf(LocalDate.now().minusDays(5));
        try (var conn = com.course.sims.util.Db.getConnection(); var st = conn.createStatement()) {
            st.execute("DELETE FROM attendance WHERE course_id = " + course.getId()
                    + " AND att_date = '" + date + "'");
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{student.getId(), Attendance.PRESENT, "批量登记"});
        rows.add(new Object[]{student.getId() + 1, Attendance.LATE, "批量登记"});
        int count = attendanceService.batchSave(course.getId(), date, rows);
        assertEquals(2, count, "批量登记条数不正确");
        // 再次批量登记应更新状态
        rows.clear();
        rows.add(new Object[]{student.getId(), Attendance.ABSENT, "状态变更"});
        attendanceService.batchSave(course.getId(), date, rows);
        assertEquals(Attendance.ABSENT,
                scalarString("SELECT status FROM attendance WHERE student_id = ? AND course_id = ? AND att_date = ?",
                        student.getId(), course.getId(), date), "重复批量登记应更新已有记录状态");
        assertEquals(2, scalarInt("SELECT COUNT(*) FROM attendance WHERE course_id = ? AND att_date = ?",
                course.getId(), date), "重复批量登记不应产生多余记录");
    }

    @Test
    @DisplayName("TC-E-13 批量登记：未选择课程、日期或数据为空应被拒绝")
    void testBatchSaveValidation() {
        Date date = Date.valueOf(LocalDate.now());
        List<Object[]> empty = new ArrayList<>();
        assertBizError("未选择课程应被拒绝", () -> attendanceService.batchSave(null, date, empty));
        assertBizError("未选择日期应被拒绝", () -> attendanceService.batchSave(anyCourse().getId(), null, empty));
        assertBizError("数据为空应被拒绝", () -> attendanceService.batchSave(anyCourse().getId(), date, empty));
    }

    // ------------------------------------------------------------------
    //  TC-E-14 ~ TC-E-16  预警与默认值
    // ------------------------------------------------------------------

    @Test
    @DisplayName("TC-E-14 考勤预警：预警名单中异常次数均达到阈值")
    void testWarningList() {
        List<Map<String, Object>> warnings = attendanceService.warningList();
        assertTrue(warnings.size() > 0, "演示数据中应存在考勤预警学生");
        int threshold = new SystemService().getIntConfig("attendance.warn", 3);
        for (Map<String, Object> row : warnings) {
            int abnormal = ((Number) row.get("abnormal_count")).intValue();
            assertTrue(abnormal >= threshold,
                    "预警名单中出现异常次数低于阈值的学生：" + abnormal + " < " + threshold);
            assertNotNull(row.get("sno"), "预警名单应包含学号");
            assertNotNull(row.get("class_name"), "预警名单应包含班级名称");
        }
    }

    @Test
    @DisplayName("TC-E-15 一键全勤：默认数据行为全部出勤且条数等于班级人数")
    void testBuildDefaultRows() {
        var clazz = anyClassWithStudents();
        List<Object[]> rows = attendanceService.buildDefaultRows(clazz.getId(), anyCourse().getId(),
                Date.valueOf(LocalDate.now()));
        assertEquals(clazz.getStudentCount(), rows.size(), "默认行数应等于班级学生人数");
        for (Object[] row : rows) {
            assertEquals(Attendance.PRESENT, row[1], "默认考勤状态应为出勤");
        }
        assertEquals(0, attendanceService.buildDefaultRows(null, null, null).size(),
                "班级为空时应返回空列表");
    }

    @Test
    @DisplayName("TC-E-16 删除考勤：记录不存在时应给出明确提示")
    void testDeleteNotExist() {
        BusinessException e = assertBizError("删除不存在的考勤应抛出业务异常",
                () -> attendanceService.delete(999999));
        assertTrue(e.getMessage().contains("不存在"), "提示应说明记录不存在");
    }
}
