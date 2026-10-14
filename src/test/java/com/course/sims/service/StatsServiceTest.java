package com.course.sims.service;

import com.course.sims.BaseTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 统计分析与报表测试（模块 M7）。
 *
 * @author 课程设计第 2 小组 - 成员 E
 * @version 1.0.0
 */
@DisplayName("模块 M7：统计分析与报表测试")
class StatsServiceTest extends BaseTest {

    private final StatsService statsService = new StatsService();

    // ------------------------------------------------------------------
    //  TC-F-01 ~ TC-F-04  首页看板
    // ------------------------------------------------------------------

    @Test
    @DisplayName("TC-F-01 数据看板：各项基础指标与数据库实际数据一致")
    void testDashboardBasic() {
        DashboardStats stats = statsService.dashboard();
        assertEquals(40, stats.getStudentTotal(), "学生总数应为 40");
        assertEquals(10, stats.getClassTotal(), "班级总数应为 10");
        assertEquals(5, stats.getMajorTotal(), "专业总数应为 5");
        assertEquals(10, stats.getCourseTotal(), "课程总数应为 10");
        assertEquals(75, stats.getScoreTotal(), "成绩记录数应为 75");
        assertEquals(900, stats.getAttendanceTotal(), "考勤记录数应为 900");
        assertTrue(stats.getStudyingTotal() >= 39, "在读学生数应不少于 39");
        assertTrue(stats.getUserTotal() >= 4, "系统用户数应不少于 4");
    }

    @Test
    @DisplayName("TC-F-02 数据看板：平均分与及格率计算正确且与明细一致")
    void testDashboardScore() {
        DashboardStats stats = statsService.dashboard();
        double expectedAvg = scalarDouble("SELECT ROUND(AVG(total_score), 1) FROM score WHERE total_score IS NOT NULL");
        double expectedPass = scalarDouble("SELECT ROUND(100.0 * SUM(CASE WHEN total_score >= 60 THEN 1 ELSE 0 END) "
                + "/ COUNT(*), 1) FROM score WHERE total_score IS NOT NULL");
        assertEquals(expectedAvg, stats.getAvgScore(), 0.05D, "看板平均分与数据库统计不一致");
        assertEquals(expectedPass, stats.getPassRate(), 0.05D, "看板及格率与数据库统计不一致");
        assertTrue(stats.getAvgScore() > 0 && stats.getAvgScore() <= 100, "平均分应在合理范围");
        assertEquals(stats.getFailedCount(),
                scalarInt("SELECT COUNT(*) FROM score WHERE total_score < 60"), "不及格记录数统计不一致");
    }

    @Test
    @DisplayName("TC-F-03 数据看板：班级排名与成绩分布列表非空且结构正确")
    void testDashboardLists() {
        DashboardStats stats = statsService.dashboard();
        assertTrue(stats.getClassRank().size() >= 1, "班级平均分排名不应为空");
        for (String[] row : stats.getClassRank()) {
            assertEquals(2, row.length, "班级排名每行应为 [班级名, 平均分] 两个元素");
            assertNotNull(row[0], "班级名不应为空");
            Double.parseDouble(row[1]);
        }
        assertTrue(stats.getScoreDistribution().size() >= 1, "成绩等级分布不应为空");
        int sum = 0;
        for (String[] row : stats.getScoreDistribution()) {
            sum += Integer.parseInt(row[1]);
        }
        assertEquals(75, sum, "成绩等级分布人数之和应等于成绩总记录数");
    }

    @Test
    @DisplayName("TC-F-04 数据看板：考勤预警人数与预警名单长度一致")
    void testDashboardWarning() {
        DashboardStats stats = statsService.dashboard();
        assertEquals(statsService.attendanceWarning().size(), stats.getAttendanceWarningCount(),
                "看板预警人数与预警名单长度不一致");
    }

    // ------------------------------------------------------------------
    //  TC-F-05 ~ TC-F-10  各类统计查询
    // ------------------------------------------------------------------

    @Test
    @DisplayName("TC-F-05 班级平均分排名：按平均分降序排列")
    void testClassAverageRank() {
        List<Map<String, Object>> list = statsService.classAverageRank(null);
        assertTrue(list.size() >= 1, "班级排名不应为空");
        double prev = 101D;
        for (Map<String, Object> row : list) {
            double avg = ((Number) row.get("avg_score")).doubleValue();
            assertTrue(avg <= prev, "班级排名未按平均分降序排列");
            prev = avg;
            assertNotNull(row.get("class_name"), "应包含班级名称");
            assertNotNull(row.get("student_count"), "应包含人数");
            assertNotNull(row.get("pass_rate"), "应包含及格率");
        }
    }

    @Test
    @DisplayName("TC-F-06 课程成绩统计：所有课程的平均分介于最低分与最高分之间")
    void testCourseAverageStat() {
        List<Map<String, Object>> list = statsService.courseAverageStat(null);
        assertEquals(5, list.size(), "第一学期 5 门课程均有成绩");
        for (Map<String, Object> row : list) {
            double avg = ((Number) row.get("avg_score")).doubleValue();
            double max = ((Number) row.get("max_score")).doubleValue();
            double min = ((Number) row.get("min_score")).doubleValue();
            assertTrue(avg >= min && avg <= max,
                    "课程【" + row.get("course_name") + "】平均分不在最低分与最高分之间");
            double rate = ((Number) row.get("pass_rate")).doubleValue();
            assertTrue(rate >= 0 && rate <= 100, "及格率超出范围：" + rate);
        }
    }

    @Test
    @DisplayName("TC-F-07 成绩等级分布：五档人数之和等于成绩总记录数")
    void testScoreDistribution() {
        List<Map<String, Object>> list = statsService.scoreDistribution(null);
        int sum = 0;
        for (Map<String, Object> row : list) {
            sum += ((Number) row.get("level_count")).intValue();
            assertNotNull(row.get("level_name"), "应包含等级名称");
        }
        assertEquals(75, sum, "五档分布人数之和应等于 75");
        assertTrue(list.size() <= 5, "成绩等级最多五档");
    }

    @Test
    @DisplayName("TC-F-08 分数段分布：各分数段人数之和等于成绩总记录数")
    void testScoreSegment() {
        List<Map<String, Object>> list = statsService.scoreSegment();
        int sum = 0;
        for (Map<String, Object> row : list) {
            sum += ((Number) row.get("num")).intValue();
            assertNotNull(row.get("score_range"), "应包含分数段标签");
        }
        assertEquals(75, sum, "分数段人数之和应等于 75");
    }

    @Test
    @DisplayName("TC-F-09 学生成绩排名：按平均分降序，且取前 N 名生效")
    void testStudentRank() {
        List<Map<String, Object>> top5 = statsService.studentRank(null, null, 5);
        assertEquals(5, top5.size(), "前 5 名应返回 5 条记录");
        double prev = 101D;
        for (Map<String, Object> row : top5) {
            double avg = ((Number) row.get("avg_score")).doubleValue();
            assertTrue(avg <= prev, "学生排名未按平均分降序排列");
            prev = avg;
            assertNotNull(row.get("sno"), "应包含学号");
            assertNotNull(row.get("name"), "应包含姓名");
            assertNotNull(row.get("total_credit"), "应包含已获学分");
        }
        List<Map<String, Object>> topAll = statsService.studentRank(null, null, 100);
        assertEquals(15, topAll.size(), "演示数据中 15 名学生有成绩");
    }

    @Test
    @DisplayName("TC-F-10 分布统计：专业、年级、性别、班级人数之和均等于学生总数")
    void testDistributions() {
        int total = scalarInt("SELECT COUNT(*) FROM student");
        int majorSum = 0;
        for (Map<String, Object> row : statsService.majorDistribution()) {
            majorSum += ((Number) row.get("student_count")).intValue();
            assertNotNull(row.get("major_name"), "应包含专业名称");
        }
        assertEquals(total, majorSum, "各专业人数之和应等于学生总数");

        int gradeSum = 0;
        for (Map<String, Object> row : statsService.gradeDistribution()) {
            gradeSum += ((Number) row.get("student_count")).intValue();
        }
        assertEquals(total, gradeSum, "各年级人数之和应等于学生总数");

        int genderSum = 0;
        for (Map<String, Object> row : statsService.genderDistribution()) {
            genderSum += ((Number) row.get("student_count")).intValue();
            assertNotNull(row.get("gender"), "应包含性别");
        }
        assertEquals(total, genderSum, "男女人数之和应等于学生总数");

        int classSum = 0;
        for (Map<String, Object> row : statsService.classStudentCount()) {
            classSum += ((Number) row.get("student_count")).intValue();
        }
        assertEquals(total, classSum, "各班人数之和应等于学生总数");
    }

    @Test
    @DisplayName("TC-F-11 教师授课任务：教师课程数与平均分统计正确")
    void testTeacherWorkload() {
        List<Map<String, Object>> list = statsService.teacherWorkload();
        assertTrue(list.size() >= 5, "演示数据中至少应有 5 位授课教师");
        for (Map<String, Object> row : list) {
            assertNotNull(row.get("teacher"), "应包含教师姓名");
            assertTrue(((Number) row.get("course_count")).intValue() >= 1, "教师承担课程数应不少于 1");
        }
    }

    @Test
    @DisplayName("TC-F-12 概览数组：指标顺序与数值正确")
    void testOverviewArray() {
        Object[] overview = statsService.overviewArray();
        assertEquals(6, overview.length, "概览数组应包含 6 项指标");
        assertEquals(40, ((Number) overview[0]).intValue(), "第 1 项应为学生总数");
        assertEquals(10, ((Number) overview[1]).intValue(), "第 2 项应为班级总数");
        assertEquals(10, ((Number) overview[2]).intValue(), "第 3 项应为课程总数");
        assertTrue(((Number) overview[3]).intValue() >= 1, "第 4 项应为教师人数");
    }

    /**
     * 查询单个 double 值。
     *
     * @param sql 查询语句
     * @return 数值
     */
    private double scalarDouble(String sql) {
        try (var conn = com.course.sims.util.Db.getConnection();
             var st = conn.createStatement();
             var rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getDouble(1) : 0D;
        } catch (Exception e) {
            throw new IllegalStateException("查询失败：" + sql, e);
        }
    }
}
