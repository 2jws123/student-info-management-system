package com.course.sims.service;

import com.course.sims.dao.AttendanceDao;
import com.course.sims.dao.ConfigDao;
import com.course.sims.dao.DaoFactory;
import com.course.sims.dao.StudentDao;
import com.course.sims.entity.Attendance;
import com.course.sims.entity.Config;
import com.course.sims.entity.Student;
import com.course.sims.util.PageResult;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 考勤管理业务逻辑（模块 M6）。
 *
 * <p>核心业务规则：</p>
 * <ol>
 *   <li>同一学生、同一课程、同一天只允许一条考勤记录；</li>
 *   <li>考勤日期不能晚于今天；</li>
 *   <li>出勤率 = 出勤次数 / 总考勤次数 × 100%；</li>
 *   <li>当学生异常考勤次数达到系统参数 {@code attendance.warn}（默认 3 次）时进入预警名单。</li>
 * </ol>
 *
 * @author 课程设计第 2 小组 - 成员 C
 * @version 1.0.0
 */
public class AttendanceService {

    private final AttendanceDao attendanceDao = DaoFactory.attendanceDao();
    private final StudentDao studentDao = DaoFactory.studentDao();
    private final ConfigDao configDao = DaoFactory.configDao();

    /**
     * 分页查询考勤记录。
     *
     * @param keyword  关键字
     * @param classId  班级过滤
     * @param courseId 课程过滤
     * @param status   状态过滤
     * @param startDay 起始日期
     * @param endDay   截止日期
     * @param pageNo   页码
     * @param pageSize 每页条数
     * @return 分页结果
     */
    public PageResult<Attendance> findByPage(String keyword, Integer classId, Integer courseId, String status,
                                             Date startDay, Date endDay, int pageNo, int pageSize) {
        return attendanceDao.findByPage(keyword, classId, courseId, status, startDay, endDay, pageNo, pageSize);
    }

    /**
     * 查询考勤明细（导出使用）。
     *
     * @param classId  班级过滤
     * @param courseId 课程过滤
     * @param startDay 起始日期
     * @param endDay   截止日期
     * @return 考勤列表
     */
    public List<Attendance> findList(Integer classId, Integer courseId, Date startDay, Date endDay) {
        return attendanceDao.findList(classId, courseId, startDay, endDay);
    }

    /**
     * 查询某学生考勤记录。
     *
     * @param studentId 学生主键
     * @return 考勤列表
     */
    public List<Attendance> findByStudent(Integer studentId) {
        return attendanceDao.findByStudent(studentId);
    }

    /**
     * 保存考勤记录（新增或修改）。
     *
     * @param a 考勤记录
     * @return 主键
     * @throws BusinessException 校验失败或重复登记
     */
    public int save(Attendance a) {
        validate(a);
        if (attendanceDao.exists(a.getStudentId(), a.getCourseId(), a.getAttDate(), a.getId())) {
            throw new BusinessException("ERR-401", "该学生本课程当日已有考勤记录，请勿重复登记");
        }
        int result;
        if (a.getId() == null) {
            result = attendanceDao.insert(a);
            a.setId(result);
            AuthService.log("登记考勤", describe(a));
        } else {
            result = attendanceDao.update(a);
            AuthService.log("修改考勤", describe(a));
        }
        return result;
    }

    /**
     * 批量登记考勤（整班一次登记）。
     *
     * @param courseId 课程主键
     * @param attDate  考勤日期
     * @param rows     每行：[学生主键, 考勤状态, 备注]
     * @return 成功登记条数
     * @throws BusinessException 参数非法
     */
    public int batchSave(Integer courseId, Date attDate, List<Object[]> rows) {
        if (courseId == null) {
            throw new BusinessException("ERR-402", "请选择课程");
        }
        if (attDate == null) {
            throw new BusinessException("ERR-403", "请选择考勤日期");
        }
        if (rows == null || rows.isEmpty()) {
            throw new BusinessException("ERR-404", "没有需要保存的考勤数据");
        }
        int count = 0;
        for (Object[] row : rows) {
            Attendance a = new Attendance();
            a.setStudentId((Integer) row[0]);
            a.setCourseId(courseId);
            a.setAttDate(attDate);
            a.setStatus(row[1] == null ? Attendance.PRESENT : row[1].toString());
            a.setRemark(row[2] == null ? "" : row[2].toString());
            Attendance exist = attendanceDao.findByStudentAndCourseDate(a.getStudentId(), courseId, attDate);
            if (exist != null) {
                exist.setStatus(a.getStatus());
                exist.setRemark(a.getRemark());
                attendanceDao.update(exist);
            } else {
                attendanceDao.insert(a);
            }
            count++;
        }
        AuthService.log("批量登记考勤", "课程ID=" + courseId + "，日期=" + attDate + "，共 " + count + " 条");
        return count;
    }

    /**
     * 删除考勤记录。
     *
     * @param id 主键
     */
    public void delete(Integer id) {
        Attendance a = attendanceDao.findById(id);
        if (a == null) {
            throw new BusinessException("ERR-405", "考勤记录不存在");
        }
        attendanceDao.deleteById(id);
        AuthService.log("删除考勤", describe(a));
    }

    /**
     * 考勤业务校验。
     *
     * @param a 考勤记录
     * @throws BusinessException 校验失败
     */
    public void validate(Attendance a) {
        if (a.getStudentId() == null) {
            throw new BusinessException("ERR-307", "请选择学生");
        }
        if (a.getCourseId() == null) {
            throw new BusinessException("ERR-402", "请选择课程");
        }
        if (a.getAttDate() == null) {
            throw new BusinessException("ERR-403", "请选择考勤日期");
        }
        if (a.getAttDate().after(new Date(System.currentTimeMillis()))) {
            throw new BusinessException("ERR-406", "考勤日期不能晚于今天");
        }
        if (a.getStatus() == null || a.getStatus().isBlank()) {
            throw new BusinessException("ERR-407", "请选择考勤状态");
        }
        boolean valid = false;
        for (String s : Attendance.ALL_STATUS) {
            if (s.equals(a.getStatus())) {
                valid = true;
                break;
            }
        }
        if (!valid) {
            throw new BusinessException("ERR-408", "考勤状态取值不合法：" + a.getStatus());
        }
        if (DaoFactory.courseDao().findById(a.getCourseId()) == null) {
            throw new BusinessException("ERR-303", "所选课程不存在");
        }
        if (studentDao.findById(a.getStudentId()) == null) {
            throw new BusinessException("ERR-308", "所选学生不存在");
        }
    }

    /**
     * 计算某学生出勤率。
     *
     * @param studentId 学生主键
     * @return 出勤率（%）
     */
    public double attendanceRate(Integer studentId) {
        return attendanceDao.attendanceRate(studentId);
    }

    /**
     * 考勤预警名单（异常次数达到阈值）。
     *
     * @return 名单列表
     */
    public List<Map<String, Object>> warningList() {
        int threshold = configDao.getInt(Config.KEY_ATTENDANCE_WARN, 3);
        return attendanceDao.warningList(threshold);
    }

    /**
     * 按状态统计考勤次数。
     *
     * @return 统计列表
     */
    public List<Map<String, Object>> countGroupByStatus() {
        return attendanceDao.countGroupByStatus();
    }

    /**
     * 按班级统计出勤率。
     *
     * @return 统计列表
     */
    public List<Map<String, Object>> classAttendanceRate() {
        return attendanceDao.classAttendanceRate();
    }

    /**
     * 生成某班级某课程某日的默认考勤数据（全部出勤），供"一键全勤"按钮使用。
     *
     * @param classId  班级主键
     * @param courseId 课程主键
     * @param attDate  考勤日期
     * @return 行数据列表：[学生主键, 状态, 备注]
     */
    public List<Object[]> buildDefaultRows(Integer classId, Integer courseId, Date attDate) {
        List<Object[]> rows = new ArrayList<>();
        if (classId == null) {
            return rows;
        }
        for (Student s : studentDao.findByClass(classId)) {
            rows.add(new Object[]{s.getId(), Attendance.PRESENT, ""});
        }
        return rows;
    }

    /**
     * 统计考勤记录总数。
     *
     * @return 记录数量
     */
    public int countAll() {
        return attendanceDao.countAll();
    }

    /**
     * 生成考勤描述文本。
     *
     * @param a 考勤记录
     * @return 描述文本
     */
    private String describe(Attendance a) {
        return (a.getSno() == null ? "" : a.getSno())
                + (a.getStudentName() == null ? "" : " " + a.getStudentName())
                + " " + a.getAttDate() + " " + a.getStatus();
    }
}
