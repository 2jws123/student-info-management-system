package com.course.sims.service;

import com.course.sims.dao.ClazzDao;
import com.course.sims.dao.CourseDao;
import com.course.sims.dao.DaoFactory;
import com.course.sims.dao.MajorDao;
import com.course.sims.dao.StudentDao;
import com.course.sims.entity.Clazz;
import com.course.sims.entity.Course;
import com.course.sims.entity.Major;
import com.course.sims.util.PageResult;

import java.util.List;

/**
 * 教学基础数据业务逻辑，覆盖模块 M3（班级与专业管理）与模块 M4（课程管理）。
 *
 * <p>核心业务规则：</p>
 * <ol>
 *   <li>专业代码、班级代码、课程代码均须全局唯一；</li>
 *   <li>删除专业前需保证其下没有班级；删除班级前需保证其下没有学生；</li>
 *   <li>删除课程前需保证没有关联成绩与考勤记录；</li>
 *   <li>学分范围为 0.5~10，学时范围为 8~200。</li>
 * </ol>
 *
 * @author 课程设计第 2 小组 - 成员 B
 * @version 1.0.0
 */
public class TeachingService {

    private final MajorDao majorDao = DaoFactory.majorDao();
    private final ClazzDao clazzDao = DaoFactory.clazzDao();
    private final CourseDao courseDao = DaoFactory.courseDao();
    private final StudentDao studentDao = DaoFactory.studentDao();

    // ------------------------------------------------------------------
    //  专业管理
    // ------------------------------------------------------------------

    /**
     * 分页查询专业。
     *
     * @param keyword  关键字
     * @param pageNo   页码
     * @param pageSize 每页条数
     * @return 分页结果
     */
    public PageResult<Major> findMajors(String keyword, int pageNo, int pageSize) {
        return majorDao.findByPage(keyword, pageNo, pageSize);
    }

    /**
     * 查询全部专业。
     *
     * @return 专业列表
     */
    public List<Major> findAllMajors() {
        return majorDao.findAll();
    }

    /**
     * 保存专业。
     *
     * @param major 专业对象
     * @return 主键
     * @throws BusinessException 校验失败
     */
    public int saveMajor(Major major) {
        if (major.getMajorCode() == null || major.getMajorCode().isBlank()) {
            throw new BusinessException("ERR-201", "专业代码不能为空");
        }
        if (major.getMajorName() == null || major.getMajorName().isBlank()) {
            throw new BusinessException("ERR-202", "专业名称不能为空");
        }
        if (major.getDeptName() == null || major.getDeptName().isBlank()) {
            throw new BusinessException("ERR-203", "所属院系不能为空");
        }
        if (major.getDuration() == null || major.getDuration() < 2 || major.getDuration() > 6) {
            throw new BusinessException("ERR-204", "学制年数应在 2~6 年之间");
        }
        if (majorDao.existsCode(major.getMajorCode().trim(), major.getId())) {
            throw new BusinessException("ERR-205", "专业代码已存在：" + major.getMajorCode());
        }
        int id;
        if (major.getId() == null) {
            id = majorDao.insert(major);
            major.setId(id);
            AuthService.log("新增专业", major.getMajorName());
        } else {
            id = majorDao.update(major);
            AuthService.log("修改专业", major.getMajorName());
        }
        return id;
    }

    /**
     * 删除专业（专业下存在班级时禁止删除）。
     *
     * @param id 专业主键
     * @throws BusinessException 存在关联班级
     */
    public void deleteMajor(Integer id) {
        Major major = majorDao.findById(id);
        if (major == null) {
            throw new BusinessException("ERR-206", "专业不存在");
        }
        int count = clazzDao.countByMajor(id);
        if (count > 0) {
            throw new BusinessException("ERR-207", "专业【" + major.getMajorName() + "】下还有 " + count
                    + " 个班级，请先删除或调整这些班级");
        }
        majorDao.deleteById(id);
        AuthService.log("删除专业", major.getMajorName());
    }

    // ------------------------------------------------------------------
    //  班级管理
    // ------------------------------------------------------------------

    /**
     * 分页查询班级。
     *
     * @param keyword  关键字
     * @param majorId  专业过滤
     * @param grade    年级过滤
     * @param pageNo   页码
     * @param pageSize 每页条数
     * @return 分页结果
     */
    public PageResult<Clazz> findClasses(String keyword, Integer majorId, Integer grade, int pageNo, int pageSize) {
        return clazzDao.findByPage(keyword, majorId, grade, pageNo, pageSize);
    }

    /**
     * 查询全部班级。
     *
     * @return 班级列表
     */
    public List<Clazz> findAllClasses() {
        return clazzDao.findAll();
    }

    /**
     * 按专业查询班级。
     *
     * @param majorId 专业主键
     * @return 班级列表
     */
    public List<Clazz> findClassesByMajor(Integer majorId) {
        return clazzDao.findByMajor(majorId);
    }

    /**
     * 保存班级。
     *
     * @param clazz 班级对象
     * @return 主键
     * @throws BusinessException 校验失败
     */
    public int saveClazz(Clazz clazz) {
        if (clazz.getClassCode() == null || clazz.getClassCode().isBlank()) {
            throw new BusinessException("ERR-208", "班级代码不能为空");
        }
        if (clazz.getClassName() == null || clazz.getClassName().isBlank()) {
            throw new BusinessException("ERR-209", "班级名称不能为空");
        }
        if (clazz.getMajorId() == null) {
            throw new BusinessException("ERR-210", "请选择所属专业");
        }
        if (majorDao.findById(clazz.getMajorId()) == null) {
            throw new BusinessException("ERR-206", "所选专业不存在");
        }
        if (clazz.getGrade() == null || clazz.getGrade() < 2000 || clazz.getGrade() > 2100) {
            throw new BusinessException("ERR-211", "年级取值不合法，应为 2000~2100 之间的年份");
        }
        if (clazz.getCapacity() == null || clazz.getCapacity() <= 0) {
            throw new BusinessException("ERR-212", "班级容量必须大于 0");
        }
        if (clazzDao.existsCode(clazz.getClassCode().trim(), clazz.getId())) {
            throw new BusinessException("ERR-213", "班级代码已存在：" + clazz.getClassCode());
        }
        // 容量不得小于现有学生数
        if (clazz.getId() != null) {
            int current = clazzDao.countStudents(clazz.getId());
            if (clazz.getCapacity() < current) {
                throw new BusinessException("ERR-214", "班级容量不能小于当前人数 " + current + " 人");
            }
        }
        int id;
        if (clazz.getId() == null) {
            id = clazzDao.insert(clazz);
            clazz.setId(id);
            AuthService.log("新增班级", clazz.getClassName());
        } else {
            id = clazzDao.update(clazz);
            AuthService.log("修改班级", clazz.getClassName());
        }
        return id;
    }

    /**
     * 删除班级（班级下存在学生时禁止删除）。
     *
     * @param id 班级主键
     * @throws BusinessException 存在关联学生
     */
    public void deleteClazz(Integer id) {
        Clazz clazz = clazzDao.findById(id);
        if (clazz == null) {
            throw new BusinessException("ERR-215", "班级不存在");
        }
        int count = clazzDao.countStudents(id);
        if (count > 0) {
            throw new BusinessException("ERR-216", "班级【" + clazz.getClassName() + "】下还有 " + count
                    + " 名学生，请先转班或删除学生");
        }
        clazzDao.deleteById(id);
        AuthService.log("删除班级", clazz.getClassName());
    }

    // ------------------------------------------------------------------
    //  课程管理
    // ------------------------------------------------------------------

    /**
     * 分页查询课程。
     *
     * @param keyword    关键字
     * @param courseType 课程类型过滤
     * @param semester   学期过滤
     * @param pageNo     页码
     * @param pageSize   每页条数
     * @return 分页结果
     */
    public PageResult<Course> findCourses(String keyword, String courseType, String semester,
                                          int pageNo, int pageSize) {
        return courseDao.findByPage(keyword, courseType, semester, pageNo, pageSize);
    }

    /**
     * 查询全部课程。
     *
     * @return 课程列表
     */
    public List<Course> findAllCourses() {
        return courseDao.findAll();
    }

    /**
     * 查询全部学期。
     *
     * @return 学期列表
     */
    public List<String> findAllSemesters() {
        return courseDao.findAllSemesters();
    }

    /**
     * 保存课程。
     *
     * @param course 课程对象
     * @return 主键
     * @throws BusinessException 校验失败
     */
    public int saveCourse(Course course) {
        if (course.getCourseCode() == null || course.getCourseCode().isBlank()) {
            throw new BusinessException("ERR-217", "课程代码不能为空");
        }
        if (course.getCourseName() == null || course.getCourseName().isBlank()) {
            throw new BusinessException("ERR-218", "课程名称不能为空");
        }
        if (course.getCredit() == null || course.getCredit() < 0.5 || course.getCredit() > 10) {
            throw new BusinessException("ERR-219", "学分应在 0.5~10 之间");
        }
        if (course.getHours() == null || course.getHours() < 8 || course.getHours() > 200) {
            throw new BusinessException("ERR-220", "学时应在 8~200 之间");
        }
        if (course.getCourseType() == null || course.getCourseType().isBlank()) {
            throw new BusinessException("ERR-221", "请选择课程类型");
        }
        if (courseDao.existsCode(course.getCourseCode().trim(), course.getId())) {
            throw new BusinessException("ERR-222", "课程代码已存在：" + course.getCourseCode());
        }
        int id;
        if (course.getId() == null) {
            id = courseDao.insert(course);
            course.setId(id);
            AuthService.log("新增课程", course.getCourseName());
        } else {
            id = courseDao.update(course);
            AuthService.log("修改课程", course.getCourseName());
        }
        return id;
    }

    /**
     * 删除课程（存在成绩或考勤记录时禁止删除）。
     *
     * @param id 课程主键
     * @throws BusinessException 存在关联数据
     */
    public void deleteCourse(Integer id) {
        Course course = courseDao.findById(id);
        if (course == null) {
            throw new BusinessException("ERR-223", "课程不存在");
        }
        int scoreCount = DaoFactory.scoreDao().countByCourse(id);
        if (scoreCount > 0) {
            throw new BusinessException("ERR-224", "课程【" + course.getCourseName() + "】已录入 " + scoreCount
                    + " 条成绩记录，请先删除相关成绩");
        }
        int attCount = DaoFactory.attendanceDao().countByCourse(id);
        if (attCount > 0) {
            throw new BusinessException("ERR-225", "课程【" + course.getCourseName() + "】已存在 " + attCount
                    + " 条考勤记录，请先删除相关考勤");
        }
        courseDao.deleteById(id);
        AuthService.log("删除课程", course.getCourseName());
    }

    /**
     * 查询课程数据统计（课程数量、总学分、必修课程数）。
     *
     * @return 统计结果数组：[课程数, 总学分, 必修数]
     */
    public double[] courseSummary() {
        List<Course> courses = courseDao.findAll();
        double credit = 0D;
        int required = 0;
        for (Course c : courses) {
            if (c.getCredit() != null) {
                credit += c.getCredit();
            }
            if (Course.TYPE_REQUIRED.equals(c.getCourseType())) {
                required++;
            }
        }
        return new double[]{courses.size(), credit, required};
    }

    /**
     * 统计学生总数（看板使用）。
     *
     * @return 学生总数
     */
    public int countStudents() {
        return studentDao.countAll();
    }
}
