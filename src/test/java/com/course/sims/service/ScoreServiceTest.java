package com.course.sims.service;

import com.course.sims.BaseTest;
import com.course.sims.entity.Course;
import com.course.sims.entity.Score;
import com.course.sims.entity.Student;
import com.course.sims.util.PageResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 成绩管理与绩点计算测试（模块 M5）。
 *
 * @author 课程设计第 2 小组 - 成员 E
 * @version 1.0.0
 */
@DisplayName("模块 M5：成绩管理与绩点计算测试")
class ScoreServiceTest extends BaseTest {

    private final ScoreService scoreService = new ScoreService();
    private final StudentService studentService = new StudentService();
    private final TeachingService teachingService = new TeachingService();

    /**
     * 取一名演示学生。
     *
     * @return 学生对象
     */
    /**
     * 取第一名"有成绩记录"的学生，避免用例依赖学号排序。
     *
     * <p>演示数据只为前 15 名学生录入了成绩，直接取学生列表第一人可能没有成绩，
     * 因此统一从成绩表中反查学生，保证用例与演示数据的具体分布解耦。</p>
     *
     * @return 学生对象
     */
    private Student anyStudent() {
        var scored = scoreService.findByPage(null, null, null, null, null, 1, 1).getRows();
        if (!scored.isEmpty()) {
            return studentService.findById(scored.get(0).getStudentId());
        }
        // 成绩表已被清空（例如用例需要空表作为前置条件）时退回学生列表首条
        return studentService.findByPage(null, null, null, null, 1, 1).getRows().get(0);
    }

    /**
     * 取一门演示课程。
     *
     * @return 课程对象
     */
    private Course anyCourse() {
        return teachingService.findAllCourses().get(0);
    }

    // ------------------------------------------------------------------
    //  TC-D-01 ~ TC-D-06  成绩查询与总评
    // ------------------------------------------------------------------

    @Test
    @DisplayName("TC-D-01 成绩列表：演示数据 75 条，分页查询正常")
    void testFindByPage() {
        PageResult<Score> page = scoreService.findByPage(null, null, null, null, null, 1, 15);
        assertEquals(75L, page.getTotal(), "演示数据应有 75 条成绩记录");
        assertEquals(15, page.getRows().size(), "首页应返回 15 条");
        Score first = page.getRows().get(0);
        assertTrue(first.getSno() != null && first.getStudentName() != null, "应联表带出学生信息");
        assertTrue(first.getCourseName() != null, "应联表带出课程名称");
    }

    @Test
    @DisplayName("TC-D-02 成绩列表：按及格状态筛选，不及格记录总评均小于 60")
    void testFilterFailed() {
        PageResult<Score> failed = scoreService.findByPage(null, null, null, null, Boolean.FALSE, 1, 50);
        assertTrue(failed.getTotal() > 0, "演示数据中应存在不及格记录");
        for (Score s : failed.getRows()) {
            assertTrue(s.getTotalScore() == null || s.getTotalScore() < 60,
                    "不及格筛选结果中包含及格记录：" + s.getTotalScore());
        }
        PageResult<Score> passed = scoreService.findByPage(null, null, null, null, Boolean.TRUE, 1, 50);
        for (Score s : passed.getRows()) {
            assertTrue(s.getTotalScore() >= 60, "及格筛选结果中包含不及格记录");
        }
    }

    @Test
    @DisplayName("TC-D-03 成绩列表：按班级、课程、学期筛选生效")
    void testFilterConditions() {
        Course course = anyCourse();
        PageResult<Score> byCourse = scoreService.findByPage(null, null, course.getId(), null, null, 1, 50);
        for (Score s : byCourse.getRows()) {
            assertEquals(course.getId(), s.getCourseId(), "课程筛选结果不正确");
        }
        PageResult<Score> bySemester = scoreService.findByPage(null, null, null, "2024-2025-1", null, 1, 50);
        assertEquals(75L, bySemester.getTotal(), "第一学期应有 75 条成绩");
        PageResult<Score> other = scoreService.findByPage(null, null, null, "2025-2026-1", null, 1, 50);
        assertEquals(0L, other.getTotal(), "不存在的学期应返回 0 条");
    }

    @Test
    @DisplayName("TC-D-04 数据库总评计算列与 Java 端算法结果一致")
    void testTotalScoreColumnConsistency() {
        List<Score> list = scoreService.findList(null, null, null, "2024-2025-1");
        assertTrue(list.size() > 0, "演示成绩不能为空");
        for (Score s : list) {
            Double javaTotal = s.calcTotal();
            assertTrue(javaTotal != null, "Java 端总评不应为 null");
            assertEquals(javaTotal, s.getTotalScore(), 0.05D,
                    "Java 算法与数据库计算列结果不一致：" + s.getSno() + " " + s.getCourseName()
                            + " Java=" + javaTotal + " DB=" + s.getTotalScore());
        }
    }

    @Test
    @DisplayName("TC-D-05 单科成绩查询：学生成绩单按学期分组正确")
    void testFindByStudent() {
        Student student = anyStudent();
        List<Score> scores = scoreService.findByStudent(student.getId(), null);
        assertTrue(scores.size() > 0, "演示学生应有成绩记录");
        List<Score> term = scoreService.findByStudent(student.getId(), "2024-2025-1");
        assertEquals(5, term.size(), "第一学期应有 5 门课程成绩");
        List<Score> course = scoreService.findByCourse(anyCourse().getId(), null);
        assertTrue(course.size() > 0, "按课程查询成绩应有记录");
    }

    @Test
    @DisplayName("TC-D-06 课程统计：平均分、最高分、最低分、及格率计算正确")
    void testCourseStat() {
        Map<String, Object> stat = scoreService.courseStat(anyCourse().getId());
        double avg = ((Number) stat.get("avg_score")).doubleValue();
        double max = ((Number) stat.get("max_score")).doubleValue();
        double min = ((Number) stat.get("min_score")).doubleValue();
        double passRate = ((Number) stat.get("pass_rate")).doubleValue();
        assertTrue(avg >= min && avg <= max, "平均分应介于最低分与最高分之间");
        assertTrue(passRate >= 0 && passRate <= 100, "及格率应在 0~100 之间");
        assertEquals(15, ((Number) stat.get("total_count")).intValue(), "该课程应有 15 名学生的成绩");
    }

    // ------------------------------------------------------------------
    //  TC-D-07 ~ TC-D-12  录入与校验
    // ------------------------------------------------------------------

    @Test
    @DisplayName("TC-D-07 录入成绩：合法数据保存成功，总评自动计算")
    void testSaveScore() {
        clearScores();
        Student student = anyStudent();
        Course course = anyCourse();
        Score score = new Score(student.getId(), course.getId(), "2099-2100-1");
        score.setUsualScore(88.0);
        score.setExamScore(92.0);
        scoreService.save(score);
        assertTrue(score.getId() > 0, "录入成绩应返回主键");
        double total = scalarIntDouble("SELECT total_score FROM score WHERE id = ?", score.getId());
        assertEquals(90.8D, total, 0.05D, "数据库总评成绩应为 90.8");
        clearScores();
    }

    @Test
    @DisplayName("TC-D-08 录入成绩：补考成绩覆盖总评，重修标记自动置位")
    void testSaveScoreWithMakeup() {
        clearScores();
        Student student = anyStudent();
        Course course = anyCourse();
        Score score = new Score(student.getId(), course.getId(), "2099-2100-1");
        score.setUsualScore(70.0);
        score.setExamScore(50.0);
        score.setMakeupScore(66.0);
        scoreService.save(score);
        double total = scalarIntDouble("SELECT total_score FROM score WHERE id = ?", score.getId());
        assertEquals(66.0D, total, 0.05D, "存在补考成绩时总评应取补考成绩");
        assertEquals(1, scalarInt("SELECT COUNT(*) FROM score WHERE id = ? AND is_retake = TRUE", score.getId()),
                "填写补考成绩后重修标记应为真");
        clearScores();
    }

    @Test
    @DisplayName("TC-D-09 录入成绩：分数越界（<0 或 >100）应被拒绝")
    void testScoreRangeValidation() {
        Student student = anyStudent();
        Course course = anyCourse();
        Score tooHigh = new Score(student.getId(), course.getId(), "2099-2100-1");
        tooHigh.setUsualScore(120.0);
        tooHigh.setExamScore(90.0);
        assertTrue(assertBizError("平时成绩超过 100 应被拒绝", () -> scoreService.save(tooHigh))
                .getMessage().contains("0~100"), "提示应说明取值范围");

        Score negative = new Score(student.getId(), course.getId(), "2099-2100-1");
        negative.setUsualScore(80.0);
        negative.setExamScore(-5.0);
        assertTrue(assertBizError("期末成绩为负数应被拒绝", () -> scoreService.save(negative))
                .getMessage().contains("0~100"), "提示应说明取值范围");
    }

    @Test
    @DisplayName("TC-D-10 录入成绩：必填项缺失（学生/课程/学期/全部成绩）应被拒绝")
    void testScoreRequiredValidation() {
        Student student = anyStudent();
        Course course = anyCourse();

        Score noCourse = new Score(student.getId(), null, "2099-2100-1");
        noCourse.setUsualScore(80.0);
        assertBizError("未选择课程应被拒绝", () -> scoreService.save(noCourse));

        Score noSemester = new Score(student.getId(), course.getId(), "");
        noSemester.setUsualScore(80.0);
        assertBizError("未选择学期应被拒绝", () -> scoreService.save(noSemester));

        Score noScore = new Score(student.getId(), course.getId(), "2099-2100-1");
        assertBizError("平时与期末成绩均未填写应被拒绝", () -> scoreService.save(noScore));

        Score badStudent = new Score(999999, course.getId(), "2099-2100-1");
        badStudent.setUsualScore(80.0);
        assertBizError("学生不存在应被拒绝", () -> scoreService.save(badStudent));
    }

    @Test
    @DisplayName("TC-D-11 录入成绩：同一学生同一课程重复录入应被拒绝")
    void testDuplicateScore() {
        clearScores();
        Student student = anyStudent();
        Course course = anyCourse();
        Score first = new Score(student.getId(), course.getId(), "2099-2100-1");
        first.setUsualScore(80.0);
        first.setExamScore(80.0);
        scoreService.save(first);

        Score duplicate = new Score(student.getId(), course.getId(), "2099-2100-1");
        duplicate.setUsualScore(90.0);
        duplicate.setExamScore(90.0);
        BusinessException e = assertBizError("重复录入应被拒绝", () -> scoreService.save(duplicate));
        assertTrue(e.getMessage().contains("已存在成绩记录"), "提示应说明已存在成绩记录");
        clearScores();
    }

    @Test
    @DisplayName("TC-D-12 修改成绩：同一记录修改不与自身冲突")
    void testUpdateScore() {
        clearScores();
        Student student = anyStudent();
        Course course = anyCourse();
        Score score = new Score(student.getId(), course.getId(), "2099-2100-1");
        score.setUsualScore(80.0);
        score.setExamScore(80.0);
        scoreService.save(score);

        score.setExamScore(100.0);
        scoreService.save(score);
        assertEquals(94.0D, scalarIntDouble("SELECT total_score FROM score WHERE id = ?", score.getId()),
                0.05D, "修改后期末成绩应更新且总评重算为 94.0");
        clearScores();
    }

    // ------------------------------------------------------------------
    //  TC-D-13 ~ TC-D-16  批量与导入
    // ------------------------------------------------------------------

    @Test
    @DisplayName("TC-D-13 批量录入：整班成绩一次保存，总评逐条计算")
    void testBatchSave() {
        clearScores();
        Course course = anyCourse();
        List<Student> students = studentService.findByPage(null, null, null, null, 1, 10).getRows();
        List<Object[]> rows = new ArrayList<>();
        for (int i = 0; i < students.size(); i++) {
            rows.add(new Object[]{students.get(i).getId(), 80.0 + i, 70.0 + i, null, "批量录入测试"});
        }
        int count = scoreService.batchSave("2099-2100-1", course.getId(), rows);
        assertEquals(students.size(), count, "批量保存条数与提交条数不一致");
        assertEquals(students.size(),
                scalarInt("SELECT COUNT(*) FROM score WHERE course_id = ?", course.getId()), "批量保存未全部写入");
        // 二次批量保存应更新已有记录而不是报错
        int again = scoreService.batchSave("2099-2100-1", course.getId(), rows);
        assertEquals(students.size(), again, "重复批量保存应更新已有记录");
        assertEquals(students.size(),
                scalarInt("SELECT COUNT(*) FROM score WHERE course_id = ?", course.getId()),
                "重复批量保存不应产生重复记录");
        clearScores();
    }

    @Test
    @DisplayName("TC-D-14 批量录入：未选择课程或数据为空应被拒绝")
    void testBatchSaveValidation() {
        assertBizError("未选择课程应被拒绝", () -> scoreService.batchSave("2099-2100-1", null, new ArrayList<>()));
        assertBizError("数据为空应被拒绝",
                () -> scoreService.batchSave("2099-2100-1", anyCourse().getId(), new ArrayList<>()));
    }

    @Test
    @DisplayName("TC-D-15 Excel 导入：合法学号导入成功，非法学号进入失败明细")
    void testImportScores() {
        clearScores();
        Course course = anyCourse();
        Student student = anyStudent();
        List<String[]> rows = Arrays.asList(
                new String[]{student.getSno(), "85", "90", ""},
                new String[]{"9999999", "85", "90", ""},
                new String[]{student.getSno(), "200", "90", ""});
        Object[] result = scoreService.importScores(course.getId(), "2099-2100-1", rows);
        assertEquals(1, ((Number) result[0]).intValue(), "应成功导入 1 条");
        assertEquals(2, ((Number) result[1]).intValue(), "应有 2 条失败记录");
        String errors = String.valueOf(result[2]);
        assertTrue(errors.contains("9999999"), "失败明细应包含不存在的学号");
        clearScores();
    }

    @Test
    @DisplayName("TC-D-16 Excel 导入：无有效数据行应被拒绝")
    void testImportScoresEmpty() {
        assertBizError("无数据行应被拒绝",
                () -> scoreService.importScores(anyCourse().getId(), "2099-2100-1", new ArrayList<>()));
    }

    // ------------------------------------------------------------------
    //  TC-D-17 ~ TC-D-20  绩点、排名与统计
    // ------------------------------------------------------------------

    @Test
    @DisplayName("TC-D-17 平均绩点：GPA = Σ(绩点×学分)/Σ学分，结果在 0~4.0 之间")
    void testCalcGpa() {
        clearScores();
        Student student = anyStudent();
        List<Course> courses = teachingService.findAllCourses();
        // 课程1：90 分（4.0 绩点，5 学分）；课程2：60 分（1.0 绩点，4 学分）
        Score s1 = new Score(student.getId(), courses.get(0).getId(), "2099-2100-1");
        s1.setUsualScore(90.0);
        s1.setExamScore(90.0);
        scoreService.save(s1);
        Score s2 = new Score(student.getId(), courses.get(1).getId(), "2099-2100-1");
        s2.setUsualScore(60.0);
        s2.setExamScore(60.0);
        scoreService.save(s2);
        double gpa = scoreService.calcGpa(student.getId());
        // (4.0×5 + 1.0×4) / 9 = 24/9 ≈ 2.67
        assertEquals(2.67D, gpa, 0.02D, "GPA 计算不正确，实际：" + gpa);
        double avg = scoreService.calcWeightedAverage(student.getId());
        // (90×5 + 60×4) / 9 = 690/9 ≈ 76.67
        assertEquals(76.67D, avg, 0.05D, "加权平均分计算不正确，实际：" + avg);
        clearScores();
    }

    @Test
    @DisplayName("TC-D-18 平均绩点：无成绩记录时返回 0 且不抛异常")
    void testCalcGpaEmpty() {
        clearScores();
        assertEquals(0.0D, scoreService.calcGpa(anyStudent().getId()), 0.001D, "无成绩时 GPA 应为 0");
        clearScores();
    }

    @Test
    @DisplayName("TC-D-19 不及格名单：名单中所有记录总评均低于 60")
    void testFailedList() {
        List<Map<String, Object>> failed = scoreService.failedList(null);
        assertTrue(failed.size() > 0, "演示数据中应存在不及格记录");
        for (Map<String, Object> row : failed) {
            double total = ((Number) row.get("total_score")).doubleValue();
            assertTrue(total < 60, "不及格名单中出现及格记录：" + total);
        }
        assertTrue(failed.get(0).get("sno") != null, "名单应包含学号");
        assertTrue(failed.get(0).get("course_name") != null, "名单应包含课程名称");
    }

    @Test
    @DisplayName("TC-D-20 学生成绩单：汇总信息与明细一致（及格课程学分之和）")
    void testTranscriptSummary() {
        Student student = anyStudent();
        StudentTranscript transcript = scoreService.transcript(student.getId(), null);
        double expectedCredit = 0D;
        int failedCount = 0;
        for (Score s : transcript.getScores()) {
            if (s.isPassed() && s.getCredit() != null) {
                expectedCredit += s.getCredit();
            }
            if (!s.isPassed()) {
                failedCount++;
            }
        }
        assertEquals(expectedCredit, transcript.getEarnedCredit(), 0.001D, "已修学分汇总不正确");
        assertEquals(failedCount, transcript.getFailedCount(), "不及格门数汇总不正确");
    }

    /**
     * 查询单个 double 值（用于校验数据库计算列）。
     *
     * @param sql    查询语句
     * @param params 参数
     * @return 数值
     */
    private double scalarIntDouble(String sql, Object... params) {
        try (var conn = com.course.sims.util.Db.getConnection();
             var ps = conn.prepareStatement(sql)) {
            com.course.sims.util.Db.bind(ps, params);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0D;
            }
        } catch (Exception e) {
            throw new IllegalStateException("查询失败：" + sql, e);
        }
    }
}
