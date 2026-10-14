package com.course.sims.config;

import com.course.sims.dao.AttendanceDao;
import com.course.sims.entity.Attendance;
import com.course.sims.util.Db;

import java.sql.Connection;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 演示数据初始化器。
 *
 * <p>schema.sql 负责建表，data.sql 负责静态基础数据；考勤记录属于"按时间规律产生"的
 * 过程性数据，写死在 SQL 中可读性差且容易过期，因此改由本类在程序首次启动时
 * 按考勤规律批量生成：为前 15 名学生、3 门课程、每周 1 次课、共 20 周生成记录，
 * 出勤状态按真实课堂分布加权（出勤约 84%、迟到 4%、早退 3%、病假 3%、事假 2%、缺勤 4%），
 * 并使用固定随机种子保证每次初始化结果一致，便于测试复现。</p>
 *
 * @author 课程设计第 2 小组 - 成员 D
 * @version 1.0.0
 */
public final class DatabaseInitializer {

    /** 生成考勤的学生数 */
    private static final int STUDENT_COUNT = 15;
    /** 生成考勤的课程数 */
    private static final int COURSE_COUNT = 3;
    /** 考勤周数 */
    private static final int WEEKS = 20;
    /** 固定随机种子，保证演示数据可复现 */
    private static final long SEED = 20240901L;

    private DatabaseInitializer() {
    }

    /**
     * 初始化数据库：建表、灌入基础数据、生成考勤演示数据。
     * <p>幂等：重复调用不会产生重复数据。</p>
     */
    public static void init() {
        Db.initDatabase();
        generateAttendanceIfAbsent();
    }

    /**
     * 仅在考勤表为空时生成考勤演示数据（幂等入口）。
     */
    public static void generateAttendanceIfAbsent() {
        try (Connection conn = openConnection()) {
            if (Db.isTableEmpty(conn, "attendance")) {
                generateAttendance();
            }
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException("判断考勤表状态失败：" + e.getMessage(), e);
        }
    }

    /**
     * 生成考勤演示数据（调用方负责保证表为空，避免重复插入）。
     *
     * @return 生成的记录条数
     */
    public static int generateAttendance() {
        AttendanceDao dao = new AttendanceDao();
        java.util.Random random = new java.util.Random(SEED);
        LocalDate today = LocalDate.now();
        // 从 20 周前开始，每周三、周五各一次课
        LocalDate start = today.minusWeeks(WEEKS);
        List<Attendance> list = new ArrayList<>();
        for (int week = 0; week < WEEKS; week++) {
            for (int courseId = 1; courseId <= COURSE_COUNT; courseId++) {
                LocalDate day = start.plusWeeks(week).plusDays(courseId == 2 ? 2 : 0);
                for (int studentId = 1; studentId <= STUDENT_COUNT; studentId++) {
                    Attendance a = new Attendance();
                    a.setStudentId(studentId);
                    a.setCourseId(courseId);
                    a.setAttDate(Date.valueOf(day));
                    a.setStatus(randomStatus(random));
                    a.setRemark("");
                    list.add(a);
                }
            }
        }
        dao.batchInsert(list);
        return list.size();
    }

    /**
     * 按加权概率随机产生考勤状态。
     *
     * @param random 随机数发生器
     * @return 考勤状态
     */
    private static String randomStatus(java.util.Random random) {
        int n = random.nextInt(100);
        if (n < 84) {
            return Attendance.PRESENT;
        }
        if (n < 88) {
            return Attendance.LATE;
        }
        if (n < 91) {
            return Attendance.EARLY_LEAVE;
        }
        if (n < 94) {
            return Attendance.SICK_LEAVE;
        }
        if (n < 96) {
            return Attendance.PERSONAL_LEAVE;
        }
        return Attendance.ABSENT;
    }

    /**
     * 打开一个临时连接用于表空判断。
     *
     * @return 连接对象
     */
    private static Connection openConnection() {
        try {
            return Db.getConnection();
        } catch (Exception e) {
            throw new IllegalStateException("数据库连接失败：" + e.getMessage(), e);
        }
    }
}
