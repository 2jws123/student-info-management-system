package com.course.sims.service;

import com.course.sims.config.DatabaseInitializer;
import com.course.sims.config.Session;
import com.course.sims.dao.ConfigDao;
import com.course.sims.dao.DaoFactory;
import com.course.sims.dao.SysLogDao;
import com.course.sims.entity.Config;
import com.course.sims.entity.SysLog;
import com.course.sims.util.Db;
import com.course.sims.util.PageResult;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 系统维护与日志管理业务逻辑，覆盖模块 M9（系统日志管理）与模块 M10（系统维护与配置）。
 *
 * <p>提供：操作日志查询与清理、系统参数读写、数据库备份、数据库重置、运行状态查询。</p>
 *
 * @author 课程设计第 2 小组 - 成员 D
 * @version 1.0.0
 */
public class SystemService {

    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final SysLogDao logDao = DaoFactory.sysLogDao();
    private final ConfigDao configDao = DaoFactory.configDao();

    // ------------------------------------------------------------------
    //  系统日志
    // ------------------------------------------------------------------

    /**
     * 分页查询操作日志。
     *
     * @param keyword  关键字
     * @param startDay 起始日期
     * @param endDay   截止日期
     * @param pageNo   页码
     * @param pageSize 每页条数
     * @return 分页结果
     */
    public PageResult<SysLog> findLogs(String keyword, java.sql.Date startDay, java.sql.Date endDay,
                                       int pageNo, int pageSize) {
        return logDao.findByPage(keyword, startDay, endDay, pageNo, pageSize);
    }

    /**
     * 查询最近日志（首页动态使用）。
     *
     * @param limit 数量
     * @return 日志列表
     */
    public List<SysLog> recentLogs(int limit) {
        return logDao.findRecent(limit);
    }

    /**
     * 清空全部操作日志（仅管理员）。
     *
     * @return 被清理的日志条数
     * @throws BusinessException 权限不足
     */
    public int clearLogs() {
        if (!Session.isAdmin()) {
            throw new BusinessException("ERR-000", "权限不足：仅系统管理员可以清空日志");
        }
        int count = logDao.countAll();
        logDao.clearAll();
        AuthService.log("清空日志", "共清理 " + count + " 条记录");
        return count;
    }

    /**
     * 记录一条操作日志。
     *
     * @param operation 操作类型
     * @param detail    详情
     */
    public void writeLog(String operation, String detail) {
        logDao.insert(new SysLog(Session.getUsername(), operation, detail));
    }

    // ------------------------------------------------------------------
    //  系统参数
    // ------------------------------------------------------------------

    /**
     * 查询全部系统参数。
     *
     * @return 参数列表
     */
    public List<Config> findAllConfigs() {
        return configDao.findAll();
    }

    /**
     * 读取字符串参数。
     *
     * @param key          参数键
     * @param defaultValue 默认值
     * @return 参数值
     */
    public String getConfig(String key, String defaultValue) {
        return configDao.getValue(key, defaultValue);
    }

    /**
     * 读取整型参数。
     *
     * @param key          参数键
     * @param defaultValue 默认值
     * @return 参数值
     */
    public int getIntConfig(String key, int defaultValue) {
        return configDao.getInt(key, defaultValue);
    }

    /**
     * 保存系统参数（带合法性校验）。
     *
     * @param config 参数对象
     * @throws BusinessException 校验失败
     */
    public void saveConfig(Config config) {
        if (!Session.isAdmin()) {
            throw new BusinessException("ERR-000", "权限不足：仅系统管理员可以修改系统参数");
        }
        if (config.getConfigKey() == null || config.getConfigKey().isBlank()) {
            throw new BusinessException("ERR-501", "参数键不能为空");
        }
        if (config.getConfigValue() == null || config.getConfigValue().isBlank()) {
            throw new BusinessException("ERR-502", "参数值不能为空");
        }
        validateConfigValue(config);
        configDao.save(config);
        AuthService.log("修改系统参数", config.getConfigKey() + " = " + config.getConfigValue());
    }

    /**
     * 校验关键参数的取值范围。
     *
     * @param config 参数对象
     * @throws BusinessException 取值非法
     */
    private void validateConfigValue(Config config) {
        String key = config.getConfigKey();
        String value = config.getConfigValue().trim();
        switch (key) {
            case Config.KEY_PASS_LINE:
            case Config.KEY_EXCELLENT_LINE:
                requireNumber(value, key, 0, 100);
                break;
            case Config.KEY_USUAL_RATIO:
            case Config.KEY_EXAM_RATIO:
                requireNumber(value, key, 0, 1);
                break;
            case Config.KEY_ATTENDANCE_WARN:
                requireNumber(value, key, 1, 100);
                break;
            case Config.KEY_PAGE_SIZE:
                requireNumber(value, key, 5, 200);
                break;
            default:
                break;
        }
    }

    /**
     * 校验数值型参数。
     *
     * @param value 参数值
     * @param key   参数键
     * @param min   下限
     * @param max   上限
     * @throws BusinessException 非法
     */
    private void requireNumber(String value, String key, double min, double max) {
        try {
            double d = Double.parseDouble(value);
            if (d < min || d > max) {
                throw new BusinessException("ERR-503", "参数【" + key + "】取值应在 " + min + "~" + max + " 之间");
            }
        } catch (NumberFormatException e) {
            throw new BusinessException("ERR-504", "参数【" + key + "】必须为数值");
        }
    }

    // ------------------------------------------------------------------
    //  数据库维护
    // ------------------------------------------------------------------

    /**
     * 备份数据库为 SQL 脚本文件。
     *
     * @param targetFile 目标文件（.sql）
     * @return 备份文件路径
     * @throws BusinessException 备份失败
     */
    public String backupDatabase(File targetFile) {
        if (targetFile == null) {
            throw new BusinessException("ERR-505", "请选择备份文件保存位置");
        }
        String path = targetFile.getAbsolutePath().replace("\\", "/");
        try (Connection conn = Db.getConnection(); Statement st = conn.createStatement()) {
            st.execute("SCRIPT TO '" + path + "'");
        } catch (Exception e) {
            throw new BusinessException("ERR-506", "数据库备份失败：" + e.getMessage());
        }
        AuthService.log("备份数据库", path);
        return path;
    }

    /**
     * 构造默认备份文件名。
     *
     * @return 形如 backup-20240901-120000.sql 的文件名
     */
    public String defaultBackupFileName() {
        return "backup-" + LocalDateTime.now().format(FILE_STAMP) + ".sql";
    }

    /**
     * 恢复演示数据：清空全部业务表并重新灌入初始演示数据（不删除数据库文件，无需重启程序）。
     *
     * @return 提示信息
     * @throws BusinessException 操作失败
     */
    public String resetDemoData() {
        if (!Session.isAdmin()) {
            throw new BusinessException("ERR-000", "权限不足：仅系统管理员可以重置数据");
        }
        List<String> tables = new ArrayList<>(List.of(
                "score", "attendance", "student", "clazz", "major", "course", "sys_user", "sys_log", "sys_config"));
        try (Connection conn = Db.getConnection(); Statement st = conn.createStatement()) {
            st.execute("SET REFERENTIAL_INTEGRITY FALSE");
            for (String table : tables) {
                st.execute("TRUNCATE TABLE " + table + " RESTART IDENTITY");
            }
            st.execute("SET REFERENTIAL_INTEGRITY TRUE");
            Db.executeScript(conn, "/db/schema.sql");
            Db.executeScript(conn, "/db/data.sql");
            // 演示数据显式指定了主键，需校正自增序列，否则后续新增会撞主键
            Db.restartIdentities(conn);
        } catch (Exception e) {
            throw new BusinessException("ERR-507", "重置演示数据失败：" + e.getMessage());
        }
        DatabaseInitializer.generateAttendance();
        return "演示数据已重置：专业 5 个、班级 10 个、学生 40 人、课程 10 门、成绩 75 条，考勤数据已重新生成。";    }

    /**
     * 彻底重置数据库文件（删除 db/sims.mv.db 后重新建库），重置后需要重新登录。
     *
     * @param dbPath 数据库文件路径（不含 .mv.db 后缀），如 db/sims
     * @return 提示信息
     * @throws BusinessException 操作失败
     */
    public String resetDatabaseFile(String dbPath) {
        if (!Session.isAdmin()) {
            throw new BusinessException("ERR-000", "权限不足：仅系统管理员可以重置数据库");
        }
        String base = dbPath == null ? "db/sims" : dbPath;
        if (!base.startsWith("./") && !base.startsWith(".\\") && !base.matches("^[A-Za-z]:[/\\\\].*")
                && !base.startsWith("/")) {
            base = "./" + base;
        }
        try {
            for (String suffix : new String[]{".mv.db", ".trace.db"}) {
                File f = new File(base + suffix);
                if (f.exists() && !f.delete()) {
                    throw new BusinessException("ERR-508", "数据库文件被占用，请关闭程序后手动删除：" + f.getAbsolutePath());
                }
            }
            DatabaseInitializer.init();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("ERR-509", "重置数据库失败：" + e.getMessage());
        }
        return "数据库已重建，请重新登录系统。";
    }

    /**
     * 查询数据库运行状态（表数量与各表记录数），用于"关于/系统状态"展示。
     *
     * @return 状态文本行列表
     */
    public List<String> databaseStatus() {
        List<String> lines = new ArrayList<>();
        lines.add("数据库文件：" + Db.getDatabasePath());
        String[] tables = {"major", "clazz", "student", "course", "score", "attendance", "sys_user", "sys_log",
                "sys_config"};
        try (Connection conn = Db.getConnection(); Statement st = conn.createStatement()) {
            int tableCount = 0;
            try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES "
                    + "WHERE TABLE_SCHEMA = 'PUBLIC'")) {
                if (rs.next()) {
                    tableCount = rs.getInt(1);
                }
            }
            lines.add("数据表数量：" + tableCount + " 张");
            for (String table : tables) {
                try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
                    if (rs.next()) {
                        lines.add(String.format("  %-12s %d 条", table, rs.getInt(1)));
                    }
                }
            }
            lines.add("当前登录用户：" + Session.getRealName() + "（" + Session.getUsername() + "）");
            lines.add("本次在线时长：" + Session.getOnlineSeconds() + " 秒");
            lines.add("统计时间：" + new Timestamp(System.currentTimeMillis()));
        } catch (Exception e) {
            lines.add("读取数据库状态失败：" + e.getMessage());
        }
        return lines;
    }

    /**
     * 统计日志总数。
     *
     * @return 日志条数
     */
    public int countLogs() {
        return logDao.countAll();
    }
}
