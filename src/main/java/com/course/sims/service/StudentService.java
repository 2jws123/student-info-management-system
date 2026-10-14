package com.course.sims.service;

import com.course.sims.dao.DaoFactory;
import com.course.sims.dao.ScoreDao;
import com.course.sims.dao.StudentDao;
import com.course.sims.entity.Score;
import com.course.sims.entity.Student;
import com.course.sims.util.PageResult;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 学生信息管理业务逻辑（模块 M2）。
 *
 * <p>核心业务规则：</p>
 * <ol>
 *   <li>学号全校唯一，格式为 7 位数字（入学年份 4 位 + 流水号 3 位）；</li>
 *   <li>手机号须为 11 位、以 1 开头；邮箱须符合通用邮箱格式；</li>
 *   <li>学生必须归属一个已存在的班级，且不得超出班级容量；</li>
 *   <li>删除学生时级联清理其成绩与考勤记录，保证数据一致性。</li>
 * </ol>
 *
 * @author 课程设计第 2 小组 - 成员 B
 * @version 1.0.0
 */
public class StudentService {

    /** 学号格式：7 位数字 */
    private static final Pattern SNO_PATTERN = Pattern.compile("^\\d{7}$");
    /** 手机号格式：1 开头 11 位数字 */
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1\\d{10}$");
    /** 邮箱格式 */
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");

    private final StudentDao studentDao = DaoFactory.studentDao();
    private final ScoreDao scoreDao = DaoFactory.scoreDao();

    /**
     * 分页查询学生。
     *
     * @param keyword  关键字（学号 / 姓名 / 手机号）
     * @param classId  班级过滤
     * @param status   学籍状态过滤
     * @param gender   性别过滤
     * @param pageNo   页码
     * @param pageSize 每页条数
     * @return 分页结果
     */
    public PageResult<Student> findByPage(String keyword, Integer classId, String status, String gender,
                                          int pageNo, int pageSize) {
        return studentDao.findByPage(keyword, classId, status, gender, pageNo, pageSize);
    }

    /**
     * 查询学生列表（导出使用）。
     *
     * @param keyword 关键字
     * @param classId 班级过滤
     * @param status  学籍状态过滤
     * @return 学生列表
     */
    public List<Student> findList(String keyword, Integer classId, String status) {
        return studentDao.findList(keyword, classId, status);
    }

    /**
     * 按主键查询学生。
     *
     * @param id 主键
     * @return 学生对象，不存在返回 null
     */
    public Student findById(Integer id) {
        return studentDao.findById(id);
    }

    /**
     * 按学号查询学生。
     *
     * @param sno 学号
     * @return 学生对象，不存在返回 null
     */
    public Student findBySno(String sno) {
        return studentDao.findBySno(sno);
    }

    /**
     * 按班级查询学生。
     *
     * @param classId 班级主键
     * @return 学生列表
     */
    public List<Student> findByClass(Integer classId) {
        return studentDao.findByClass(classId);
    }

    /**
     * 生成新学号。
     *
     * @param enrollYear 入学年份
     * @return 学号
     */
    public String generateSno(int enrollYear) {
        return studentDao.generateSno(enrollYear);
    }

    /**
     * 保存学生（新增或修改），执行完整业务校验。
     *
     * @param student 学生对象
     * @return 主键
     * @throws BusinessException 校验不通过
     */
    public int save(Student student) {
        validate(student);
        if (studentDao.existsSno(student.getSno(), student.getId())) {
            throw new BusinessException("ERR-101", "学号已存在：" + student.getSno());
        }
        int result;
        if (student.getId() == null) {
            result = studentDao.insert(student);
            // 把自增主键回写到实体，便于调用方（界面/测试）继续使用该对象
            student.setId(result);
            AuthService.log("新增学生", "学号：" + student.getSno() + "，姓名：" + student.getName());
        } else {
            result = studentDao.update(student);
            AuthService.log("修改学生", "学号：" + student.getSno() + "，姓名：" + student.getName());
        }
        return result;
    }

    /**
     * 删除学生，并级联删除其成绩与考勤记录。
     *
     * @param id 学生主键
     * @throws BusinessException 学生不存在
     */
    public void delete(Integer id) {
        Student student = studentDao.findById(id);
        if (student == null) {
            throw new BusinessException("ERR-102", "学生不存在或已被删除");
        }
        scoreDao.deleteByStudent(id);
        DaoFactory.attendanceDao().deleteByStudent(id);
        studentDao.deleteById(id);
        AuthService.log("删除学生", "学号：" + student.getSno() + "，姓名：" + student.getName());
    }

    /**
     * 批量删除学生。
     *
     * @param ids 学生主键列表
     * @return 实际删除人数
     */
    public int deleteBatch(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("ERR-103", "请先选择要删除的学生记录");
        }
        int count = 0;
        for (Integer id : ids) {
            delete(id);
            count++;
        }
        return count;
    }

    /**
     * 业务校验：必填项、格式、班级容量。
     *
     * @param s 学生对象
     * @throws BusinessException 校验失败
     */
    public void validate(Student s) {
        if (s == null) {
            throw new BusinessException("ERR-104", "学生信息不能为空");
        }
        if (s.getSno() == null || !SNO_PATTERN.matcher(s.getSno().trim()).matches()) {
            throw new BusinessException("ERR-105", "学号格式不正确，应为 7 位数字，如 2024001");
        }
        if (s.getName() == null || s.getName().isBlank()) {
            throw new BusinessException("ERR-106", "学生姓名不能为空");
        }
        if (s.getName().trim().length() > 20) {
            throw new BusinessException("ERR-107", "学生姓名长度不能超过 20 个字符");
        }
        if (s.getGender() == null || s.getGender().isBlank()) {
            throw new BusinessException("ERR-108", "请选择性别");
        }
        if (s.getClassId() == null) {
            throw new BusinessException("ERR-109", "请选择所属班级");
        }
        if (s.getPhone() != null && !s.getPhone().isBlank()
                && !PHONE_PATTERN.matcher(s.getPhone().trim()).matches()) {
            throw new BusinessException("ERR-110", "手机号格式不正确，应为 11 位数字");
        }
        if (s.getEmail() != null && !s.getEmail().isBlank()
                && !EMAIL_PATTERN.matcher(s.getEmail().trim()).matches()) {
            throw new BusinessException("ERR-111", "邮箱格式不正确");
        }
        if (s.getEnrollYear() == null) {
            throw new BusinessException("ERR-112", "请选择入学年份");
        }
        if (s.getStatus() == null || s.getStatus().isBlank()) {
            s.setStatus(Student.STATUS_STUDYING);
        }
        // 班级容量校验（仅"在读"学生占用容量）
        var clazz = DaoFactory.clazzDao().findById(s.getClassId());
        if (clazz == null) {
            throw new BusinessException("ERR-113", "所选班级不存在");
        }
        if (Student.STATUS_STUDYING.equals(s.getStatus())) {
            int current = DaoFactory.clazzDao().countStudents(s.getClassId());
            if (s.getId() != null) {
                Student old = studentDao.findById(s.getId());
                if (old != null && s.getClassId().equals(old.getClassId())) {
                    current--; // 修改本人信息不占用新增容量
                }
            }
            if (clazz.getCapacity() != null && current + 1 > clazz.getCapacity()) {
                throw new BusinessException("ERR-114", "班级【" + clazz.getClassName() + "】人数已达容量上限 "
                        + clazz.getCapacity() + " 人，无法继续添加");
            }
        }
    }

    /**
     * 统计学生总数。
     *
     * @return 学生数量
     */
    public int countAll() {
        return studentDao.countAll();
    }

    /**
     * 查询学生成绩单（含课程成绩、已修学分、加权平均分、平均绩点）。
     *
     * @param studentId 学生主键
     * @param semester  学期过滤，null 表示全部学期
     * @return 成绩单对象
     * @throws BusinessException 学生不存在
     */
    public StudentTranscript getTranscript(Integer studentId, String semester) {
        Student student = studentDao.findById(studentId);
        if (student == null && studentId != null) {
            // 兼容传入学号（如 2024001）的场景：学生角色登录时会话中保存的是学号
            student = studentDao.findBySno(String.valueOf(studentId));
        }
        if (student == null) {
            throw new BusinessException("ERR-102", "学生不存在");
        }
        studentId = student.getId();
        StudentTranscript transcript = new StudentTranscript(student);
        List<Score> scores = scoreDao.findByStudent(studentId, semester);
        for (Score score : scores) {
            transcript.addScore(score);
        }
        transcript.setFailedCount(scoreDao.countFailed(studentId));
        if (semester == null) {
            transcript.setWeightedAverage(scoreDao.calcWeightedAverage(studentId));
            transcript.setGpa(scoreDao.calcGpa(studentId));
        } else {
            double sum = 0D;
            double credit = 0D;
            double pointSum = 0D;
            for (Score score : scores) {
                if (score.getTotalScore() != null && score.getCredit() != null) {
                    sum += score.getTotalScore() * score.getCredit();
                    credit += score.getCredit();
                    pointSum += score.getGradePoint() * score.getCredit();
                }
            }
            transcript.setWeightedAverage(credit <= 0 ? 0D : Math.round(sum / credit * 100D) / 100D);
            transcript.setGpa(credit <= 0 ? 0D : Math.round(pointSum / credit * 100D) / 100D);
        }
        return transcript;
    }

    /**
     * 统计各班人数。
     *
     * @return 班级人数列表
     */
    public List<java.util.Map<String, Object>> countGroupByClass() {
        return studentDao.countGroupByClass();
    }

    /**
     * 统计各专业人数。
     *
     * @return 专业人数列表
     */
    public List<java.util.Map<String, Object>> countGroupByMajor() {
        return studentDao.countGroupByMajor();
    }
}
