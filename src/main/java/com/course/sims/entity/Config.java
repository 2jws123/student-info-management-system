package com.course.sims.entity;

/**
 * 系统参数实体，对应数据表 {@code sys_config}。
 * <p>把及格线、成绩权重、缺勤预警阈值等易变业务规则外置为参数，
 * 修改参数即可调整系统行为，无需重新编译。</p>
 *
 * @author 课程设计第 2 小组 - 成员 D
 * @version 1.0.0
 */
public class Config {

    /** 参数键常量 */
    public static final String KEY_SYSTEM_NAME = "system.name";
    public static final String KEY_SYSTEM_VERSION = "system.version";
    public static final String KEY_PASS_LINE = "score.pass.line";
    public static final String KEY_EXCELLENT_LINE = "score.excellent.line";
    public static final String KEY_USUAL_RATIO = "score.usual.ratio";
    public static final String KEY_EXAM_RATIO = "score.exam.ratio";
    public static final String KEY_ATTENDANCE_WARN = "attendance.warn";
    public static final String KEY_PAGE_SIZE = "page.size";

    private Integer id;
    private String configKey;
    private String configValue;
    private String description;

    public Config() {
    }

    public Config(String configKey, String configValue, String description) {
        this.configKey = configKey;
        this.configValue = configValue;
        this.description = description;
    }

    /**
     * 以 double 形式读取参数值。
     *
     * @param defaultValue 默认值
     * @return 数值，解析失败返回默认值
     */
    public double asDouble(double defaultValue) {
        try {
            return Double.parseDouble(configValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * 以 int 形式读取参数值。
     *
     * @param defaultValue 默认值
     * @return 整数，解析失败返回默认值
     */
    public int asInt(int defaultValue) {
        try {
            return Integer.parseInt(configValue.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigValue() {
        return configValue;
    }

    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return configKey + " = " + configValue;
    }
}
