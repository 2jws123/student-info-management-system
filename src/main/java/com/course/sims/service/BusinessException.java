package com.course.sims.service;

/**
 * 业务异常。
 *
 * <p>用于表达"可以被用户理解并修正"的业务规则冲突，例如学号重复、班级已满、
 * 专业下仍有班级不能删除等。界面层捕获后以对话框形式向用户展示提示信息，
 * 与系统级异常（数据库连接失败等）区分处理。</p>
 *
 * @author 课程设计第 2 小组 - 成员 A
 * @version 1.0.0
 */
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** 业务错误码，便于测试用例与错误信息表对照 */
    private final String code;

    /**
     * 构造业务异常。
     *
     * @param message 面向用户的提示信息
     */
    public BusinessException(String message) {
        this("ERR-BIZ", message);
    }

    /**
     * 构造带错误码的业务异常。
     *
     * @param code    错误码
     * @param message 面向用户的提示信息
     */
    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }

    /** @return 业务错误码 */
    public String getCode() {
        return code;
    }
}
