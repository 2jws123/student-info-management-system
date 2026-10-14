package com.course.sims.config;

import com.course.sims.entity.User;

/**
 * 登录会话上下文（单例）。
 *
 * <p>保存当前登录用户、角色与登录时间，供界面层做菜单装配、业务层做权限二次校验。
 * 系统为单机桌面应用，同一时刻仅有一个登录用户，因此采用静态单例保存会话。</p>
 *
 * @author 课程设计第 2 小组 - 成员 A
 * @version 1.0.0
 */
public final class Session {

    private static User currentUser;
    private static long loginTime;

    private Session() {
    }

    /**
     * 开始会话。
     *
     * @param user 登录成功的用户
     */
    public static void login(User user) {
        currentUser = user;
        loginTime = System.currentTimeMillis();
    }

    /**
     * 结束会话（注销）。
     */
    public static void logout() {
        currentUser = null;
        loginTime = 0L;
    }

    /**
     * 获取当前登录用户。
     *
     * @return 当前用户，未登录返回 null
     */
    public static User getCurrentUser() {
        return currentUser;
    }

    /**
     * 获取当前登录用户名。
     *
     * @return 用户名，未登录返回 "system"
     */
    public static String getUsername() {
        return currentUser == null ? "system" : currentUser.getUsername();
    }

    /**
     * 获取当前登录用户真实姓名。
     *
     * @return 真实姓名，未登录返回 "未登录"
     */
    public static String getRealName() {
        return currentUser == null ? "未登录" : currentUser.getRealName();
    }

    /**
     * 获取当前用户角色。
     *
     * @return 角色码，未登录返回空串
     */
    public static String getRole() {
        return currentUser == null ? "" : currentUser.getRole();
    }

    /**
     * 获取当前用户关联学号（学生角色）。
     *
     * @return 学号，非学生角色返回空串
     */
    public static String getRelatedSno() {
        return currentUser == null || currentUser.getRelatedSno() == null ? "" : currentUser.getRelatedSno();
    }

    /**
     * 判断当前用户是否为系统管理员。
     *
     * @return 是管理员返回 true
     */
    public static boolean isAdmin() {
        return User.ROLE_ADMIN.equals(getRole());
    }

    /**
     * 判断当前用户是否为教师。
     *
     * @return 是教师返回 true
     */
    public static boolean isTeacher() {
        return User.ROLE_TEACHER.equals(getRole());
    }

    /**
     * 判断当前用户是否为辅导员。
     *
     * @return 是辅导员返回 true
     */
    public static boolean isCounselor() {
        return User.ROLE_COUNSELOR.equals(getRole());
    }

    /**
     * 判断当前用户是否为学生。
     *
     * @return 是学生返回 true
     */
    public static boolean isStudent() {
        return User.ROLE_STUDENT.equals(getRole());
    }

    /**
     * 判断当前用户是否具备"数据维护"权限（管理员、教师、辅导员可维护业务数据）。
     *
     * @return 具备维护权限返回 true
     */
    public static boolean canMaintain() {
        return isAdmin() || isTeacher() || isCounselor();
    }

    /**
     * 获取本次会话已持续的秒数。
     *
     * @return 在线秒数
     */
    public static long getOnlineSeconds() {
        if (loginTime == 0L) {
            return 0L;
        }
        return (System.currentTimeMillis() - loginTime) / 1000L;
    }
}
