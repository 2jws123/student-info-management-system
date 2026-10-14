package com.course.sims.service;

import com.course.sims.dao.AttendanceDao;
import com.course.sims.dao.ClazzDao;
import com.course.sims.dao.ConfigDao;
import com.course.sims.dao.CourseDao;
import com.course.sims.dao.DaoFactory;
import com.course.sims.dao.MajorDao;
import com.course.sims.dao.ScoreDao;
import com.course.sims.dao.StudentDao;
import com.course.sims.dao.UserDao;
import com.course.sims.entity.Config;
import com.course.sims.entity.User;

import java.util.List;
import java.util.Map;

/**
 * 统计分析与报表业务逻辑（模块 M7），同时汇总首页数据看板指标。
 *
 * <p>本模块不新增数据表，全部通过 SQL 聚合在数据库端完成计算，
 * 保证在万级数据量下仍然保持毫秒级响应。</p>
 *
 * @author 课程设计第 2 小组 - 成员 E
 * @version 1.0.0
 */
public class StatsService {

    private final StudentDao studentDao = DaoFactory.studentDao();
    private final MajorDao majorDao = DaoFactory.majorDao();
    private final ClazzDao clazzDao = DaoFactory.clazzDao();
    private final CourseDao courseDao = DaoFactory.courseDao();
    private final ScoreDao scoreDao = DaoFactory.scoreDao();
    private final AttendanceDao attendanceDao = DaoFactory.attendanceDao();
    private final UserDao userDao = DaoFactory.userDao();
    private final ConfigDao configDao = DaoFactory.configDao();

    /**
     * 汇总首页数据看板全部指标。
     *
     * @return 看板统计数据
     */
    public DashboardStats dashboard() {
        DashboardStats stats = new DashboardStats();
        stats.setStudentTotal(studentDao.countAll());
        stats.setStudyingTotal(studentDao.countByStatus("在读"));
        stats.setClassTotal(clazzDao.countAll());
        stats.setMajorTotal(majorDao.countAll());
        stats.setCourseTotal(courseDao.countAll());
        stats.setUserTotal(userDao.countAll());
        stats.setScoreTotal(scoreDao.countAll());
        stats.setAttendanceTotal(attendanceDao.countAll());

        Map<String, Object> overview = scoreDao.overviewStat();
        stats.setAvgScore(toDouble(overview.get("avg_score")));
        stats.setPassRate(toDouble(overview.get("pass_rate")));
        stats.setExcellentCount((int) toDouble(overview.get("excellent_count")));
        stats.setFailedCount((int) toDouble(overview.get("failed_count")));

        int warn = configDao.getInt(Config.KEY_ATTENDANCE_WARN, 3);
        stats.setAttendanceWarningCount(attendanceDao.warningList(warn).size());

        for (Map<String, Object> row : scoreDao.classAverageRank(null)) {
            stats.getClassRank().add(new String[]{str(row.get("class_name")), str(row.get("avg_score"))});
        }
        for (Map<String, Object> row : scoreDao.scoreDistribution(null)) {
            stats.getScoreDistribution().add(new String[]{str(row.get("level_name")), str(row.get("level_count"))});
        }
        return stats;
    }

    /**
     * 班级平均分排名。
     *
     * @param semester 学期过滤，可为 null
     * @return 排名列表：class_name、student_count、avg_score、pass_rate
     */
    public List<Map<String, Object>> classAverageRank(String semester) {
        return scoreDao.classAverageRank(semester);
    }

    /**
     * 课程平均分统计。
     *
     * @param semester 学期过滤，可为 null
     * @return 统计列表：course_name、avg_score、max_score、min_score、pass_rate
     */
    public List<Map<String, Object>> courseAverageStat(String semester) {
        return scoreDao.courseAverageStat(semester);
    }

    /**
     * 成绩五档分布。
     *
     * @param courseId 课程过滤，可为 null
     * @return 分布列表：level_name、level_count
     */
    public List<Map<String, Object>> scoreDistribution(Integer courseId) {
        return scoreDao.scoreDistribution(courseId);
    }

    /**
     * 分数段人数分布（每 10 分一段）。
     *
     * @return 分布列表：score_range、num
     */
    public List<Map<String, Object>> scoreSegment() {
        return scoreDao.scoreSegment();
    }

    /**
     * 学生成绩排名。
     *
     * @param classId  班级过滤，可为 null 表示全校
     * @param semester 学期过滤，可为 null
     * @param topN     取前 N 名
     * @return 排名列表
     */
    public List<Map<String, Object>> studentRank(Integer classId, String semester, int topN) {
        return scoreDao.studentRank(classId, semester, topN);
    }

    /**
     * 专业人数分布。
     *
     * @return 分布列表：major_name、student_count
     */
    public List<Map<String, Object>> majorDistribution() {
        return studentDao.countGroupByMajor();
    }

    /**
     * 年级人数分布。
     *
     * @return 分布列表：grade、student_count
     */
    public List<Map<String, Object>> gradeDistribution() {
        return studentDao.countGroupByGrade();
    }

    /**
     * 男女比例分布。
     *
     * @return 分布列表：gender、student_count
     */
    public List<Map<String, Object>> genderDistribution() {
        return studentDao.countGroupByGender();
    }

    /**
     * 班级人数统计（含容量）。
     *
     * @return 统计列表：class_id、class_name、student_count
     */
    public List<Map<String, Object>> classStudentCount() {
        return studentDao.countGroupByClass();
    }

    /**
     * 考勤状态统计。
     *
     * @return 统计列表：status、num
     */
    public List<Map<String, Object>> attendanceStat() {
        return attendanceDao.countGroupByStatus();
    }

    /**
     * 班级出勤率统计。
     *
     * @return 统计列表：class_name、total_count、present_count、rate
     */
    public List<Map<String, Object>> classAttendanceRate() {
        return attendanceDao.classAttendanceRate();
    }

    /**
     * 考勤预警名单。
     *
     * @return 预警名单
     */
    public List<Map<String, Object>> attendanceWarning() {
        int threshold = configDao.getInt(Config.KEY_ATTENDANCE_WARN, 3);
        return attendanceDao.warningList(threshold);
    }

    /**
     * 教师授课任务统计（每位教师的课程数、平均分）。
     *
     * @return 统计列表：teacher、course_count、avg_score
     */
    public List<Map<String, Object>> teacherWorkload() {
        return com.course.sims.util.Db.queryForList(
                "SELECT co.teacher, COUNT(DISTINCT co.id) AS course_count, "
                        + "ROUND(AVG(sc.total_score), 1) AS avg_score "
                        + "FROM course co LEFT JOIN score sc ON sc.course_id = co.id "
                        + "WHERE co.teacher IS NOT NULL AND co.teacher <> '' "
                        + "GROUP BY co.teacher ORDER BY course_count DESC");
    }

    /**
     * 统计全部指标的可导出表格数据（报表模块使用）。
     *
     * @return 指标数组：[学生总数, 班级数, 课程数, 教师数, 平均分, 及格率]
     */
    public Object[] overviewArray() {
        Map<String, Object> overview = scoreDao.overviewStat();
        return new Object[]{
                studentDao.countAll(),
                clazzDao.countAll(),
                courseDao.countAll(),
                userDao.countByRole(User.ROLE_TEACHER),
                toDouble(overview.get("avg_score")),
                toDouble(overview.get("pass_rate"))
        };
    }

    /**
     * 安全地把 Object 转成 double。
     *
     * @param value 值
     * @return 数值，null 返回 0
     */
    private double toDouble(Object value) {
        if (value == null) {
            return 0D;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0D;
        }
    }

    /**
     * 安全地把 Object 转成字符串，null 转为空串（避免表格出现 "null"）。
     *
     * @param value 值
     * @return 字符串
     */
    private String str(Object value) {
        return value == null ? "" : value.toString();
    }
}
