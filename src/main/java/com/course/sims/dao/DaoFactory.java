package com.course.sims.dao;

/**
 * DAO 工厂（简单工厂 + 静态单例）。
 *
 * <p>所有 DAO 都是无状态的（不持有连接、不持有可变字段），因此可安全地在整个
 * 应用生命周期内共享同一实例。统一由本工厂提供，避免各处重复 new，
 * 也为将来替换实现（如改为连接池或 MyBatis）保留唯一修改点。</p>
 *
 * @author 课程设计第 2 小组 - 成员 A
 * @version 1.0.0
 */
public final class DaoFactory {

    private static final UserDao USER_DAO = new UserDao();
    private static final MajorDao MAJOR_DAO = new MajorDao();
    private static final ClazzDao CLAZZ_DAO = new ClazzDao();
    private static final StudentDao STUDENT_DAO = new StudentDao();
    private static final CourseDao COURSE_DAO = new CourseDao();
    private static final ScoreDao SCORE_DAO = new ScoreDao();
    private static final AttendanceDao ATTENDANCE_DAO = new AttendanceDao();
    private static final SysLogDao SYS_LOG_DAO = new SysLogDao();
    private static final ConfigDao CONFIG_DAO = new ConfigDao();

    private DaoFactory() {
    }

    /** @return 用户 DAO */
    public static UserDao userDao() {
        return USER_DAO;
    }

    /** @return 专业 DAO */
    public static MajorDao majorDao() {
        return MAJOR_DAO;
    }

    /** @return 班级 DAO */
    public static ClazzDao clazzDao() {
        return CLAZZ_DAO;
    }

    /** @return 学生 DAO */
    public static StudentDao studentDao() {
        return STUDENT_DAO;
    }

    /** @return 课程 DAO */
    public static CourseDao courseDao() {
        return COURSE_DAO;
    }

    /** @return 成绩 DAO */
    public static ScoreDao scoreDao() {
        return SCORE_DAO;
    }

    /** @return 考勤 DAO */
    public static AttendanceDao attendanceDao() {
        return ATTENDANCE_DAO;
    }

    /** @return 系统日志 DAO */
    public static SysLogDao sysLogDao() {
        return SYS_LOG_DAO;
    }

    /** @return 系统参数 DAO */
    public static ConfigDao configDao() {
        return CONFIG_DAO;
    }
}
