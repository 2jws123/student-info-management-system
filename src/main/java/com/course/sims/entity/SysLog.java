package com.course.sims.entity;

import java.sql.Timestamp;

/**
 * 系统操作日志实体，对应数据表 {@code sys_log}。
 * <p>登录、增删改等关键操作都会留痕，便于审计与问题追溯。</p>
 *
 * @author 课程设计第 2 小组 - 成员 D
 * @version 1.0.0
 */
public class SysLog {

    private Integer id;
    private String username;
    private String operation;
    private String detail;
    private Timestamp logTime;

    public SysLog() {
    }

    public SysLog(String username, String operation, String detail) {
        this.username = username;
        this.operation = operation;
        this.detail = detail;
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

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public Timestamp getLogTime() {
        return logTime;
    }

    public void setLogTime(Timestamp logTime) {
        this.logTime = logTime;
    }

    @Override
    public String toString() {
        return logTime + " " + username + " " + operation;
    }
}
