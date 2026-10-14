package com.course.sims.dao;

import com.course.sims.entity.Config;
import com.course.sims.util.Db;
import com.course.sims.util.RowMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统参数数据访问对象，对应数据表 {@code sys_config}。
 *
 * @author 课程设计第 2 小组 - 成员 D
 * @version 1.0.0
 */
public class ConfigDao {

    /** 结果集行映射器 */
    public static final RowMapper<Config> MAPPER = rs -> {
        Config c = new Config();
        c.setId(Db.getInt(rs, "id"));
        c.setConfigKey(rs.getString("config_key"));
        c.setConfigValue(rs.getString("config_value"));
        c.setDescription(rs.getString("description"));
        return c;
    };

    private static final String COLUMNS = "id, config_key, config_value, description";

    /**
     * 查询全部参数。
     *
     * @return 参数列表
     */
    public List<Config> findAll() {
        return Db.query("SELECT " + COLUMNS + " FROM sys_config ORDER BY id", MAPPER);
    }

    /**
     * 以 Map 形式查询全部参数，便于按 key 快速取值。
     *
     * @return 参数键值对
     */
    public Map<String, String> findAllAsMap() {
        Map<String, String> map = new LinkedHashMap<>();
        for (Config c : findAll()) {
            map.put(c.getConfigKey(), c.getConfigValue());
        }
        return map;
    }

    /**
     * 按参数键查询。
     *
     * @param key 参数键
     * @return 参数对象，不存在返回 null
     */
    public Config findByKey(String key) {
        return Db.queryOne("SELECT " + COLUMNS + " FROM sys_config WHERE config_key = ?", MAPPER, key);
    }

    /**
     * 读取参数值（字符串）。
     *
     * @param key          参数键
     * @param defaultValue 默认值
     * @return 参数值
     */
    public String getValue(String key, String defaultValue) {
        Config c = findByKey(key);
        return c == null || c.getConfigValue() == null ? defaultValue : c.getConfigValue();
    }

    /**
     * 读取参数值（整数）。
     *
     * @param key          参数键
     * @param defaultValue 默认值
     * @return 参数值
     */
    public int getInt(String key, int defaultValue) {
        Config c = findByKey(key);
        return c == null ? defaultValue : c.asInt(defaultValue);
    }

    /**
     * 读取参数值（小数）。
     *
     * @param key          参数键
     * @param defaultValue 默认值
     * @return 参数值
     */
    public double getDouble(String key, double defaultValue) {
        Config c = findByKey(key);
        return c == null ? defaultValue : c.asDouble(defaultValue);
    }

    /**
     * 保存参数：存在则更新，不存在则插入（H2 MERGE 语法）。
     *
     * @param config 参数对象
     * @return 受影响行数
     */
    public int save(Config config) {
        return Db.update("MERGE INTO sys_config (config_key, config_value, description) KEY (config_key) "
                + "VALUES (?, ?, ?)", config.getConfigKey(), config.getConfigValue(),
                config.getDescription() == null ? "" : config.getDescription());
    }

    /**
     * 删除参数。
     *
     * @param key 参数键
     * @return 受影响行数
     */
    public int deleteByKey(String key) {
        return Db.update("DELETE FROM sys_config WHERE config_key = ?", key);
    }

    /**
     * 统计参数数量。
     *
     * @return 参数数量
     */
    public int countAll() {
        return Db.queryInt("SELECT COUNT(*) FROM sys_config");
    }
}
