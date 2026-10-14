package com.course.sims.entity;

import java.sql.Timestamp;

/**
 * 系统用户实体，对应数据表 {@code sys_user}。
 *
 * @author 课程设计第 2 小组 - 成员 A
 * @version 1.0.0
 */
public class User {

    /** 角色常量：系统管理员 */
    public static final String ROLE_ADMIN = "ADMIN";
    /** 角色常量：教师 */
    public static final String ROLE_TEACHER = "TEACHER";
    /** 角色常量：辅导员 */
    public static final String ROLE_COUNSELOR = "COUNSELOR";
    /** 角色常量：学生 */
    public static final String ROLE_STUDENT = "STUDENT";

    private Integer id;
    private String username;
    private String password;
    private String realName;
    private String role;
    private String relatedSno;
    private String phone;
    private Integer status;
    private Timestamp lastLogin;
    private Timestamp createTime;

    public User() {
    }

    public User(String username, String password, String realName, String role) {
        this.username = username;
        this.password = password;
        this.realName = realName;
        this.role = role;
        this.status = 1;
    }

    /**
     * 获取角色中文名称。
     *
     * @return 中文角色名
     */
    public String getRoleName() {
        if (role == null) {
            return "未知";
        }
        switch (role) {
            case ROLE_ADMIN:
                return "系统管理员";
            case ROLE_TEACHER:
                return "教师";
            case ROLE_COUNSELOR:
                return "辅导员";
            case ROLE_STUDENT:
                return "学生";
            default:
                return role;
        }
    }

    /** @return 是否启用 */
    public boolean isEnabled() {
        return status != null && status == 1;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getRelatedSno() {
        return relatedSno;
    }

    public void setRelatedSno(String relatedSno) {
        this.relatedSno = relatedSno;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Timestamp getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(Timestamp lastLogin) {
        this.lastLogin = lastLogin;
    }

    public Timestamp getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Timestamp createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return realName + "(" + username + ")";
    }
}
