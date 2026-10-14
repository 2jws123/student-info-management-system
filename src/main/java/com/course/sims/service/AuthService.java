package com.course.sims.service;

import com.course.sims.config.Session;
import com.course.sims.dao.DaoFactory;
import com.course.sims.dao.SysLogDao;
import com.course.sims.dao.UserDao;
import com.course.sims.entity.SysLog;
import com.course.sims.entity.User;
import com.course.sims.util.MD5Util;
import com.course.sims.util.PageResult;

import java.sql.Timestamp;
import java.util.List;

/**
 * 用户认证与权限管理业务逻辑（模块 M1）。
 *
 * <p>职责：登录认证、密码修改与重置、账号增删改查、角色权限判定、操作日志留痕。</p>
 *
 * @author 课程设计第 2 小组 - 成员 A
 * @version 1.0.0
 */
public class AuthService {

    private final UserDao userDao = DaoFactory.userDao();
    private final SysLogDao logDao = DaoFactory.sysLogDao();

    /**
     * 用户登录认证。
     *
     * <p>处理流程：① 参数非空校验 → ② 按用户名查询账号 → ③ 校验账号是否存在 →
     * ④ 校验账号是否被禁用 → ⑤ 比对 MD5 密码 → ⑥ 写日志并更新最后登录时间 → ⑦ 建立会话。</p>
     *
     * @param username 用户名
     * @param password 明文密码
     * @return 登录成功的用户对象
     * @throws BusinessException 用户名或密码错误、账号被禁用
     */
    public User login(String username, String password) {
        if (username == null || username.isBlank()) {
            throw new BusinessException("ERR-002", "请输入用户名");
        }
        if (password == null || password.isEmpty()) {
            throw new BusinessException("ERR-003", "请输入密码");
        }
        User user = userDao.findByUsername(username.trim());
        if (user == null) {
            throw new BusinessException("ERR-001", "用户名不存在，请检查后重新输入");
        }
        // 先校验账号状态再比对密码，避免把"账号是否被禁用"的信息泄露给未通过身份验证的人
        if (!user.isEnabled()) {
            throw new BusinessException("ERR-004", "该账号已被禁用，请联系系统管理员");
        }
        if (!MD5Util.matches(password, user.getPassword())) {
            throw new BusinessException("ERR-001", "密码错误，请重新输入");
        }
        userDao.updateLastLogin(user.getId(), new Timestamp(System.currentTimeMillis()));
        Session.login(user);
        writeLog(user.getUsername(), "登录系统", "角色：" + user.getRoleName());
        return user;
    }

    /**
     * 注销登录。
     */
    public void logout() {
        if (Session.getCurrentUser() != null) {
            writeLog(Session.getUsername(), "退出系统", "");
            Session.logout();
        }
    }

    /**
     * 修改密码（校验原密码，且新密码需二次确认）。
     *
     * @param userId      用户主键
     * @param oldPassword 原密码（明文）
     * @param newPassword 新密码（明文）
     * @param confirm     确认密码
     * @throws BusinessException 原密码错误、两次输入不一致、新密码过短
     */
    public void changePassword(Integer userId, String oldPassword, String newPassword, String confirm) {
        User user = userDao.findById(userId);
        if (user == null) {
            throw new BusinessException("ERR-005", "用户不存在");
        }
        if (!MD5Util.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("ERR-006", "原密码不正确");
        }
        validatePassword(newPassword);
        if (!newPassword.equals(confirm)) {
            throw new BusinessException("ERR-007", "两次输入的新密码不一致");
        }
        if (MD5Util.matches(newPassword, user.getPassword())) {
            throw new BusinessException("ERR-008", "新密码不能与原密码相同");
        }
        userDao.updatePassword(userId, MD5Util.encrypt(newPassword));
        writeLog(Session.getUsername(), "修改密码", "用户：" + user.getUsername());
    }

    /**
     * 重置指定用户密码为系统默认密码（管理员操作）。
     *
     * @param userId       目标用户主键
     * @param defaultPwd   默认密码明文
     * @throws BusinessException 权限不足或用户不存在
     */
    public void resetPassword(Integer userId, String defaultPwd) {
        requireAdmin();
        User user = userDao.findById(userId);
        if (user == null) {
            throw new BusinessException("ERR-005", "用户不存在");
        }
        userDao.updatePassword(userId, MD5Util.encrypt(defaultPwd));
        writeLog(Session.getUsername(), "重置密码", "目标用户：" + user.getUsername());
    }

    /**
     * 校验密码强度：长度 6~20 位。
     *
     * @param password 明文密码
     * @throws BusinessException 长度不合规
     */
    private void validatePassword(String password) {
        if (password == null || password.length() < 6 || password.length() > 20) {
            throw new BusinessException("ERR-009", "密码长度需为 6~20 位");
        }
    }

    /**
     * 分页查询用户列表。
     *
     * @param keyword  关键字
     * @param role     角色过滤
     * @param status   状态过滤
     * @param pageNo   页码
     * @param pageSize 每页条数
     * @return 分页结果
     */
    public PageResult<User> findUsers(String keyword, String role, Integer status, int pageNo, int pageSize) {
        requireAdmin();
        return userDao.findByPage(keyword, role, status, pageNo, pageSize);
    }

    /**
     * 查询全部启用用户（下拉选择使用）。
     *
     * @return 用户列表
     */
    public List<User> findAllEnabled() {
        return userDao.findAllEnabled();
    }

    /**
     * 保存用户（新增或修改）。
     *
     * @param user     用户对象
     * @param rawPass  明文密码（新增时必填）
     * @return 主键
     * @throws BusinessException 用户名重复、字段为空
     */
    public int saveUser(User user, String rawPass) {
        requireAdmin();
        if (user.getUsername() == null || user.getUsername().isBlank()) {
            throw new BusinessException("ERR-010", "用户名不能为空");
        }
        if (user.getRealName() == null || user.getRealName().isBlank()) {
            throw new BusinessException("ERR-011", "真实姓名不能为空");
        }
        if (userDao.existsUsername(user.getUsername().trim(), user.getId())) {
            throw new BusinessException("ERR-012", "用户名已存在：" + user.getUsername());
        }
        if (user.getId() == null) {
            validatePassword(rawPass);
            user.setPassword(MD5Util.encrypt(rawPass));
            user.setStatus(user.getStatus() == null ? 1 : user.getStatus());
            int id = userDao.insert(user);
            user.setId(id);
            writeLog(Session.getUsername(), "新增用户", "用户名：" + user.getUsername() + "，角色：" + user.getRoleName());
            return id;
        }
        userDao.update(user);
        writeLog(Session.getUsername(), "修改用户", "用户名：" + user.getUsername());
        return user.getId();
    }

    /**
     * 删除用户。
     *
     * @param userId 用户主键
     * @throws BusinessException 不能删除当前登录账号或最后一个管理员
     */
    public void deleteUser(Integer userId) {
        requireAdmin();
        User user = userDao.findById(userId);
        if (user == null) {
            throw new BusinessException("ERR-005", "用户不存在");
        }
        if (user.getUsername().equals(Session.getUsername())) {
            throw new BusinessException("ERR-013", "不能删除当前登录的账号");
        }
        if (User.ROLE_ADMIN.equals(user.getRole()) && userDao.countByRole(User.ROLE_ADMIN) <= 1) {
            throw new BusinessException("ERR-014", "系统必须保留至少一个管理员账号");
        }
        userDao.deleteById(userId);
        writeLog(Session.getUsername(), "删除用户", "用户名：" + user.getUsername());
    }

    /**
     * 判断当前会话是否具备管理员权限。
     *
     * @throws BusinessException 权限不足
     */
    private void requireAdmin() {
        if (!Session.isAdmin()) {
            throw new BusinessException("ERR-000", "权限不足：该操作仅系统管理员可执行");
        }
    }

    /**
     * 写入操作日志（内部统一入口，任何日志写入失败都不应影响主流程）。
     *
     * @param username  操作人
     * @param operation 操作类型
     * @param detail    详情
     */
    private void writeLog(String username, String operation, String detail) {
        try {
            logDao.insert(new SysLog(username, operation, detail));
        } catch (RuntimeException ignored) {
            // 日志写入失败不影响业务主流程
        }
    }

    /**
     * 供其他 Service 调用的日志记录入口。
     *
     * @param operation 操作类型
     * @param detail    详情
     */
    public static void log(String operation, String detail) {
        try {
            DaoFactory.sysLogDao().insert(new SysLog(Session.getUsername(), operation, detail));
        } catch (RuntimeException ignored) {
            // 忽略日志异常
        }
    }
}
