package com.course.sims.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 数据库访问基础工具类（JDBC 封装）。
 * <p>职责：</p>
 * <ol>
 *   <li>管理 H2 内嵌数据库连接（文件模式，免安装数据库服务器）；</li>
 *   <li>首次启动自动执行 schema.sql 建表、data.sql 灌入演示数据；</li>
 *   <li>对 JDBC 的增删改查做统一封装，屏蔽资源释放与异常处理细节。</li>
 * </ol>
 *
 * <p>数据库文件默认位于工程目录 db/sims.mv.db，
 * 可通过 JVM 参数 {@code -Dsims.db.path=绝对路径} 指定（单元测试用内存库）。</p>
 *
 * @author 课程设计第 2 小组 - 成员 A（数据库 / 公共模块）
 * @version 1.0.0
 */
public final class Db {

    /** 默认数据库文件路径 */
    private static final String DEFAULT_DB_PATH = "db/sims";

    /** 初始化标记，防止同一 JVM 内重复建库 */
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

    static {
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("未找到 H2 数据库驱动，请检查 Maven 依赖", e);
        }
    }

    private Db() {
    }

    /**
     * 获取数据库连接。
     *
     * @return JDBC 连接，使用方需自行关闭（推荐 try-with-resources）
     * @throws SQLException 连接失败
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(buildUrl(), "sa", "");
    }

    /**
     * 构建 JDBC URL。
     *
     * <p>两种配置方式：</p>
     * <ul>
     *   <li>{@code -Dsims.db.path=db/sims}：文件库模式（生产运行默认），自动转换为
     *       {@code jdbc:h2:db/sims;MODE=MySQL;DATABASE_TO_LOWER=TRUE;AUTO_SERVER=TRUE}；</li>
     *   <li>{@code -Dsims.db.path=jdbc:h2:mem:sims_test}：直接给出完整 JDBC URL（单元测试使用），
     *       此时若为内存库会自动补上 {@code DB_CLOSE_DELAY=-1}，
     *       否则内存数据库会在第一个连接关闭时被销毁，导致后续连接中的表丢失。</li>
     * </ul>
     *
     * @return JDBC URL
     */
    public static String buildUrl() {
        String configured = System.getProperty("sims.db.path", DEFAULT_DB_PATH);
        if (configured.startsWith("jdbc:")) {
            String url = configured;
            if (isMemoryDatabase(url) && !url.contains("DB_CLOSE_DELAY")) {
                url += ";DB_CLOSE_DELAY=-1";
            }
            return url;
        }
        // H2 2.x 不接受隐式相对路径（如 db/sims），必须以 ./ 或绝对路径给出
        String path = configured;
        if (!isAbsolutePath(path) && !path.startsWith("./") && !path.startsWith(".\\")
                && !path.startsWith("~/")) {
            path = "./" + path;
        }
        String url = "jdbc:h2:" + path + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE";
        if (isMemoryDatabase(url)) {
            return url + ";DB_CLOSE_DELAY=-1";
        }
        return url + ";AUTO_SERVER=TRUE";
    }

    /**
     * 判断是否为绝对路径（Windows 盘符或 Unix 根路径）。
     *
     * @param path 路径
     * @return 绝对路径返回 true
     */
    private static boolean isAbsolutePath(String path) {
        return path.matches("^[A-Za-z]:[/\\\\].*") || path.startsWith("/");
    }

    /**
     * 判断 JDBC URL 是否为内存数据库。
     *
     * @param url JDBC URL
     * @return 内存库返回 true
     */
    private static boolean isMemoryDatabase(String url) {
        return url.contains(":mem:");
    }

    /**
     * 获取数据库文件路径（去掉 jdbc 前缀与连接参数），用于界面展示与文件重置。
     *
     * @return 数据库路径，如 db/sims 或 mem:sims_test
     */
    public static String getDatabasePath() {
        String path = getDatabasePathRaw();
        if (path.startsWith("./") || path.startsWith(".\\")) {
            return path.substring(2);
        }
        return path;
    }

    /**
     * 获取去掉 jdbc 前缀与连接参数后的原始数据库路径（保留 ./ 前缀，供文件操作使用）。
     *
     * @return 原始路径
     */
    public static String getDatabasePathRaw() {
        String url = buildUrl();
        String path = url.startsWith("jdbc:h2:") ? url.substring("jdbc:h2:".length()) : url;
        int index = path.indexOf(';');
        return index > 0 ? path.substring(0, index) : path;
    }

    /**
     * 初始化数据库：建表 + 首次灌入演示数据。多次调用只执行一次。
     */
    public static void initDatabase() {
        if (!INITIALIZED.compareAndSet(false, true)) {
            return;
        }
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                executeScript(conn, "/db/schema.sql");
                if (isTableEmpty(conn, "sys_user")) {
                    executeScript(conn, "/db/data.sql");
                }
                restartIdentities(conn);
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            INITIALIZED.set(false);
            throw new IllegalStateException("数据库初始化失败：" + e.getMessage(), e);
        }
    }

    /**
     * 把所有业务表的自增序列推进到"当前最大主键 + 1"。
     *
     * <p>必要性：脚本中显式指定主键插入演示数据（INSERT ... VALUES (1, ...)）时，
     * 数据库自增序列并不会随之推进，后续 INSERT 会从 1 开始分配主键而撞上已存在的记录，
     * 表现为"新增数据报唯一约束冲突、返回主键为 -1"。
     * 因此在数据初始化完成后统一校正一次序列。</p>
     *
     * @param conn 连接
     */
    public static void restartIdentities(Connection conn) {
        String[] tables = {"major", "clazz", "student", "course", "score", "attendance", "sys_user", "sys_log",
                "sys_config"};
        for (String table : tables) {
            String sql = "ALTER TABLE " + table + " ALTER COLUMN id RESTART WITH "
                    + "(SELECT COALESCE(MAX(id), 0) + 1 FROM " + table + ")";
            try (Statement st = conn.createStatement()) {
                st.execute(sql);
            } catch (SQLException e) {
                // 空表或表不存在时忽略（表被清空后 COALESCE 已保证从 1 开始）
                System.err.println("[提示] 校正 " + table + " 自增序列失败：" + e.getMessage());
            }
        }
    }

    /**
     * 判断某张表是否为空（用于决定是否需要灌入演示数据）。
     *
     * @param conn  连接
     * @param table 表名
     * @return 表不存在或无记录返回 true
     */
    public static boolean isTableEmpty(Connection conn, String table) {
        String sql = "SELECT COUNT(*) FROM " + table;
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            return rs.next() && rs.getInt(1) == 0;
        } catch (SQLException e) {
            // 表不存在
            return true;
        }
    }

    /**
     * 执行 classpath 下的 SQL 脚本文件。
     *
     * @param conn       连接
     * @param scriptPath classpath 路径，如 /db/schema.sql
     * @throws IOException        读取脚本失败
     * @throws SQLException       执行失败
     */
    public static void executeScript(Connection conn, String scriptPath) throws IOException, SQLException {
        String script = readResource(scriptPath);
        try (Statement st = conn.createStatement()) {
            for (String sql : splitStatements(script)) {
                st.execute(sql);
            }
        }
    }

    /**
     * 读取 classpath 文本资源（UTF-8）。
     *
     * @param path classpath 路径
     * @return 文本内容
     * @throws IOException 资源不存在或读取失败
     */
    public static String readResource(String path) throws IOException {
        try (InputStream in = Db.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IOException("classpath 资源不存在：" + path);
            }
            StringBuilder sb = new StringBuilder(4096);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append('\n');
                }
            }
            return sb.toString();
        }
    }

    /**
     * 按分号切分 SQL 脚本，跳过注释行。
     *
     * @param script 脚本内容
     * @return 单条 SQL 语句列表
     */
    static List<String> splitStatements(String script) {
        List<String> list = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String rawLine : script.split("\\R")) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("--")) {
                continue;
            }
            current.append(rawLine).append('\n');
            if (line.endsWith(";")) {
                String sql = current.toString().trim();
                list.add(sql.substring(0, sql.length() - 1));
                current.setLength(0);
            }
        }
        if (!current.toString().isBlank()) {
            list.add(current.toString().trim());
        }
        return list;
    }

    // ------------------------------------------------------------------
    //  增删改
    // ------------------------------------------------------------------

    /**
     * 执行 INSERT / UPDATE / DELETE。
     *
     * @param sql    带占位符的 SQL
     * @param params 参数
     * @return 受影响行数
     */
    public static int update(String sql, Object... params) {
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, params);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("执行更新失败：" + sql + " -> " + e.getMessage(), e);
        }
    }

    /**
     * 执行 INSERT 并返回自增主键。
     *
     * @param sql    带占位符的 INSERT 语句
     * @param params 参数
     * @return 生成的主键值；失败返回 -1
     */
    public static int insertReturnKey(String sql, Object... params) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, params);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        } catch (SQLException e) {
            throw new RuntimeException("插入数据失败：" + e.getMessage(), e);
        }
    }

    /**
     * 批量执行同一条 SQL（用于批量导入/批量灌数据）。
     *
     * @param sql        带占位符的 SQL
     * @param paramsList 参数数组列表
     * @return 成功执行的总行数
     */
    public static int batchUpdate(String sql, List<Object[]> paramsList) {
        if (paramsList == null || paramsList.isEmpty()) {
            return 0;
        }
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            for (Object[] params : paramsList) {
                bind(ps, params);
                ps.addBatch();
            }
            int[] result = ps.executeBatch();
            conn.commit();
            int total = 0;
            for (int r : result) {
                total += Math.max(r, 0);
            }
            return total;
        } catch (SQLException e) {
            throw new RuntimeException("批量执行失败：" + e.getMessage(), e);
        }
    }

    // ------------------------------------------------------------------
    //  查询
    // ------------------------------------------------------------------

    /**
     * 查询列表。
     *
     * @param sql    带占位符的 SQL
     * @param mapper 行映射器
     * @param params 参数
     * @param <T>    记录类型
     * @return 结果列表，无数据返回空列表
     */
    public static <T> List<T> query(String sql, RowMapper<T> mapper, Object... params) {
        List<T> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapper.map(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询失败：" + sql + " -> " + e.getMessage(), e);
        }
        return list;
    }

    /**
     * 查询单条记录。
     *
     * @param sql    带占位符的 SQL
     * @param mapper 行映射器
     * @param params 参数
     * @param <T>    记录类型
     * @return 记录对象，不存在返回 null
     */
    public static <T> T queryOne(String sql, RowMapper<T> mapper, Object... params) {
        List<T> list = query(sql, mapper, params);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 查询单个数值（COUNT / SUM / AVG 等聚合场景）。
     *
     * @param sql    带占位符的 SQL
     * @param params 参数
     * @return 数值，无结果返回 0
     */
    public static double queryDouble(String sql, Object... params) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0D;
            }
        } catch (SQLException e) {
            throw new RuntimeException("聚合查询失败：" + sql + " -> " + e.getMessage(), e);
        }
    }

    /**
     * 查询单个整型数值。
     *
     * @param sql    带占位符的 SQL
     * @param params 参数
     * @return 整数，无结果返回 0
     */
    public static int queryInt(String sql, Object... params) {
        return (int) queryDouble(sql, params);
    }

    /**
     * 查询单列字符串。
     *
     * @param sql    带占位符的 SQL
     * @param params 参数
     * @return 字符串，无结果返回 null
     */
    public static String queryString(String sql, Object... params) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询失败：" + sql + " -> " + e.getMessage(), e);
        }
    }

    /**
     * 查询单行记录为 Map（列名 -> 值），用于统计报表等动态结果集。
     *
     * @param sql    带占位符的 SQL
     * @param params 参数
     * @return LinkedHashMap，无结果返回空 Map
     */
    public static Map<String, Object> queryForMap(String sql, Object... params) {
        Map<String, Object> map = new LinkedHashMap<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getMetaData().getColumnCount();
                    for (int i = 1; i <= count; i++) {
                        map.put(columnKey(rs.getMetaData().getColumnLabel(i)), rs.getObject(i));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询失败：" + sql + " -> " + e.getMessage(), e);
        }
        return map;
    }

    /**
     * 查询多行记录为 Map 列表（统计分组场景）。
     *
     * @param sql    带占位符的 SQL
     * @param params 参数
     * @return Map 列表
     */
    public static List<Map<String, Object>> queryForList(String sql, Object... params) {
        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                int count = rs.getMetaData().getColumnCount();
                while (rs.next()) {
                    Map<String, Object> map = new LinkedHashMap<>();
                    for (int i = 1; i <= count; i++) {
                        map.put(columnKey(rs.getMetaData().getColumnLabel(i)), rs.getObject(i));
                    }
                    list.add(map);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询失败：" + sql + " -> " + e.getMessage(), e);
        }
        return list;
    }

    /**
     * 统一结果集列名大小写。
     *
     * <p>H2 在执行聚合查询时可能返回大写列名（如 TEACHER），而普通查询返回小写列名（如 teacher），
     * 导致同一处 Map 取值时而命中时而取不到。此处统一转为小写，
     * 使所有统计接口对外暴露的键名稳定为小写下划线形式。</p>
     *
     * @param label 原始列名
     * @return 小写列名
     */
    private static String columnKey(String label) {
        return label == null ? "" : label.toLowerCase(java.util.Locale.ROOT);
    }

    // ------------------------------------------------------------------
    //  ResultSet 类型安全读取（供 RowMapper 复用）
    // ------------------------------------------------------------------

    /**
     * 读取双精度数值，支持 DECIMAL/NUMERIC 类型。
     *
     * @param rs    结果集
     * @param label 列名
     * @return 数值，列为空时返回 null
     * @throws SQLException 读取异常
     */
    public static Double getDouble(ResultSet rs, String label) throws SQLException {
        double value = rs.getDouble(label);
        return rs.wasNull() ? null : value;
    }

    /**
     * 读取整型数值。
     *
     * @param rs    结果集
     * @param label 列名
     * @return 整数，列为空时返回 null
     * @throws SQLException 读取异常
     */
    public static Integer getInt(ResultSet rs, String label) throws SQLException {
        int value = rs.getInt(label);
        return rs.wasNull() ? null : value;
    }

    /**
     * 读取日期时间戳。
     *
     * @param rs    结果集
     * @param label 列名
     * @return 时间戳，列为空时返回 null
     * @throws SQLException 读取异常
     */
    public static Timestamp getTimestamp(ResultSet rs, String label) throws SQLException {
        return rs.getTimestamp(label);
    }

    /**
     * 读取 java.sql.Date 日期。
     *
     * @param rs    结果集
     * @param label 列名
     * @return 日期，列为空时返回 null
     * @throws SQLException 读取异常
     */
    public static java.sql.Date getDate(ResultSet rs, String label) throws SQLException {
        return rs.getDate(label);
    }

    /**
     * 为 PreparedStatement 绑定参数，自动处理 java.util.Date。
     *
     * @param ps     预编译语句
     * @param params 参数数组
     * @throws SQLException 绑定异常
     */
    public static void bind(PreparedStatement ps, Object... params) throws SQLException {
        if (params == null) {
            return;
        }
        for (int i = 0; i < params.length; i++) {
            Object p = params[i];
            if (p instanceof java.util.Date && !(p instanceof java.sql.Date)) {
                p = new Timestamp(((java.util.Date) p).getTime());
            }
            ps.setObject(i + 1, p);
        }
    }
}
