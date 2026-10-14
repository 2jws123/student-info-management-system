package com.course.sims.service;

import com.course.sims.dao.CourseDao;
import com.course.sims.dao.DaoFactory;
import com.course.sims.dao.ScoreDao;
import com.course.sims.dao.StudentDao;
import com.course.sims.entity.Course;
import com.course.sims.entity.Score;
import com.course.sims.entity.Student;
import com.course.sims.util.PageResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 成绩管理业务逻辑（模块 M5），并承担绩点（GPA）计算。
 *
 * <p>核心业务规则：</p>
 * <ol>
 *   <li>成绩取值 0~100，允许小数，最多 1 位小数；</li>
 *   <li>总评成绩 = 平时成绩 × 30% + 期末成绩 × 70%；存在补考成绩时以补考成绩为准；</li>
 *   <li>同一学生同一课程只允许一条成绩记录（数据库唯一约束 + 业务层判重）；</li>
 *   <li>绩点按四等五级制计算：90 分及以上 4.0、80~89 为 3.0、70~79 为 2.0、60~69 为 1.0、
 *       60 分以下 0；GPA = Σ(绩点 × 学分) / Σ学分。</li>
 * </ol>
 *
 * @author 课程设计第 2 小组 - 成员 C
 * @version 1.0.0
 */
public class ScoreService {

    /** 平时成绩权重 */
    public static final double USUAL_RATIO = 0.3D;
    /** 期末成绩权重 */
    public static final double EXAM_RATIO = 0.7D;

    private final ScoreDao scoreDao = DaoFactory.scoreDao();
    private final StudentDao studentDao = DaoFactory.studentDao();
    private final CourseDao courseDao = DaoFactory.courseDao();
    private final StudentService studentService = new StudentService();

    /**
     * 分页查询成绩。
     *
     * @param keyword   关键字
     * @param classId   班级过滤
     * @param courseId  课程过滤
     * @param semester  学期过滤
     * @param passState 及格状态过滤
     * @param pageNo    页码
     * @param pageSize  每页条数
     * @return 分页结果
     */
    public PageResult<Score> findByPage(String keyword, Integer classId, Integer courseId, String semester,
                                        Boolean passState, int pageNo, int pageSize) {
        return scoreDao.findByPage(keyword, classId, courseId, semester, passState, pageNo, pageSize);
    }

    /**
     * 查询成绩明细列表（导出使用）。
     *
     * @param keyword  关键字
     * @param classId  班级过滤
     * @param courseId 课程过滤
     * @param semester 学期过滤
     * @return 成绩列表
     */
    public List<Score> findList(String keyword, Integer classId, Integer courseId, String semester) {
        return scoreDao.findList(keyword, classId, courseId, semester);
    }

    /**
     * 查询某课程的全部成绩（成绩录入界面使用）。
     *
     * @param courseId 课程主键
     * @param classId  班级过滤，可为 null
     * @return 成绩列表
     */
    public List<Score> findByCourse(Integer courseId, Integer classId) {
        return scoreDao.findByCourse(courseId, classId);
    }

    /**
     * 查询某学生的成绩。
     *
     * @param studentId 学生主键
     * @param semester  学期过滤
     * @return 成绩列表
     */
    public List<Score> findByStudent(Integer studentId, String semester) {
        return scoreDao.findByStudent(studentId, semester);
    }

    /**
     * 保存单条成绩（新增或修改）。
     *
     * @param score 成绩对象
     * @return 主键
     * @throws BusinessException 校验失败或重复录入
     */
    public int save(Score score) {
        validate(score);
        Score exist = scoreDao.findByStudentAndCourse(score.getStudentId(), score.getCourseId());
        if (exist != null && !exist.getId().equals(score.getId())) {
            throw new BusinessException("ERR-301", "该学生本门课程已存在成绩记录，请直接修改，不要重复录入");
        }
        int result;
        if (score.getId() == null) {
            result = scoreDao.insert(score);
            score.setId(result);
            AuthService.log("录入成绩", describe(score));
        } else {
            result = scoreDao.update(score);
            AuthService.log("修改成绩", describe(score));
        }
        return result;
    }

    /**
     * 批量保存某课程某学期的成绩。
     *
     * @param semester 学期
     * @param courseId 课程主键
     * @param rows     每行数据：[学生主键, 平时成绩, 期末成绩, 补考成绩, 备注]
     * @return 成功保存的记录数
     * @throws BusinessException 课程不存在或数据校验失败
     */
    public int batchSave(String semester, Integer courseId, List<Object[]> rows) {
        if (courseId == null) {
            throw new BusinessException("ERR-302", "请先选择课程");
        }
        Course course = courseDao.findById(courseId);
        if (course == null) {
            throw new BusinessException("ERR-303", "课程不存在");
        }
        if (rows == null || rows.isEmpty()) {
            throw new BusinessException("ERR-304", "没有需要保存的成绩数据");
        }
        List<Score> list = new ArrayList<>();
        for (Object[] row : rows) {
            Score s = new Score();
            s.setStudentId((Integer) row[0]);
            s.setCourseId(courseId);
            s.setSemester(semester == null || semester.isBlank() ? course.getSemester() : semester);
            s.setUsualScore(toDouble(row[1]));
            s.setExamScore(toDouble(row[2]));
            s.setMakeupScore(toDouble(row[3]));
            s.setRemark(row[4] == null ? "" : row[4].toString());
            s.setIsRetake(s.getMakeupScore() != null);
            validate(s);
            Score exist = scoreDao.findByStudentAndCourse(s.getStudentId(), courseId);
            if (exist != null) {
                s.setId(exist.getId());
                scoreDao.update(s);
            } else {
                scoreDao.insert(s);
            }
            list.add(s);
        }
        AuthService.log("批量录入成绩", "课程：" + course.getCourseName() + "，共 " + list.size() + " 条");
        return list.size();
    }

    /**
     * Excel 导入成绩数据。
     *
     * @param courseId 课程主键
     * @param semester 学期
     * @param rows     导入行：[学号, 平时成绩, 期末成绩, 补考成绩]
     * @return 导入结果描述：[成功条数, 失败条数, 错误明细]
     */
    public Object[] importScores(Integer courseId, String semester, List<String[]> rows) {
        int success = 0;
        StringBuilder errors = new StringBuilder();
        if (rows == null || rows.isEmpty()) {
            throw new BusinessException("ERR-305", "导入文件中没有有效数据行");
        }
        for (int i = 0; i < rows.size(); i++) {
            String[] row = rows.get(i);
            try {
                String sno = row[0] == null ? "" : row[0].trim();
                Student student = studentDao.findBySno(sno);
                if (student == null) {
                    errors.append("第 ").append(i + 2).append(" 行：学号 ").append(sno).append(" 不存在；");
                    continue;
                }
                Score s = new Score();
                s.setStudentId(student.getId());
                s.setCourseId(courseId);
                s.setSemester(semester);
                s.setUsualScore(parseScore(row.length > 1 ? row[1] : null));
                s.setExamScore(parseScore(row.length > 2 ? row[2] : null));
                s.setMakeupScore(parseScore(row.length > 3 ? row[3] : null));
                s.setIsRetake(s.getMakeupScore() != null);
                validate(s);
                Score exist = scoreDao.findByStudentAndCourse(s.getStudentId(), courseId);
                if (exist != null) {
                    s.setId(exist.getId());
                    scoreDao.update(s);
                } else {
                    scoreDao.insert(s);
                }
                success++;
            } catch (BusinessException e) {
                errors.append("第 ").append(i + 2).append(" 行：").append(e.getMessage()).append("；");
            }
        }
        AuthService.log("导入成绩", "成功 " + success + " 条，失败 " + (rows.size() - success) + " 条");
        return new Object[]{success, rows.size() - success, errors.toString()};
    }

    /**
     * 删除成绩记录。
     *
     * @param id 主键
     */
    public void delete(Integer id) {
        Score score = scoreDao.findById(id);
        if (score == null) {
            throw new BusinessException("ERR-306", "成绩记录不存在");
        }
        scoreDao.deleteById(id);
        AuthService.log("删除成绩", describe(score));
    }

    /**
     * 统计某课程成绩指标。
     *
     * @param courseId 课程主键
     * @return 统计 Map
     */
    public Map<String, Object> courseStat(Integer courseId) {
        return scoreDao.courseStat(courseId);
    }

    /**
     * 查询不及格名单。
     *
     * @param courseId 课程过滤
     * @return 名单列表
     */
    public List<Map<String, Object>> failedList(Integer courseId) {
        return scoreDao.failedList(courseId);
    }

    /**
     * 计算某学生平均绩点。
     *
     * @param studentId 学生主键
     * @return GPA
     */
    public double calcGpa(Integer studentId) {
        return scoreDao.calcGpa(studentId);
    }

    /**
     * 计算某学生加权平均分。
     *
     * @param studentId 学生主键
     * @return 加权平均分
     */
    public double calcWeightedAverage(Integer studentId) {
        return scoreDao.calcWeightedAverage(studentId);
    }

    /**
     * 查询学生成绩单。
     *
     * @param studentId 学生主键
     * @param semester  学期过滤
     * @return 成绩单
     */
    public StudentTranscript transcript(Integer studentId, String semester) {
        return studentService.getTranscript(studentId, semester);
    }

    /**
     * 成绩业务校验。
     *
     * @param s 成绩对象
     * @throws BusinessException 校验失败
     */
    public void validate(Score s) {
        if (s.getStudentId() == null) {
            throw new BusinessException("ERR-307", "请选择学生");
        }
        if (s.getCourseId() == null) {
            throw new BusinessException("ERR-302", "请选择课程");
        }
        if (studentDao.findById(s.getStudentId()) == null) {
            throw new BusinessException("ERR-308", "所选学生不存在");
        }
        if (courseDao.findById(s.getCourseId()) == null) {
            throw new BusinessException("ERR-303", "所选课程不存在");
        }
        if (s.getSemester() == null || s.getSemester().isBlank()) {
            throw new BusinessException("ERR-309", "请选择学期");
        }
        checkRange(s.getUsualScore(), "平时成绩");
        checkRange(s.getExamScore(), "期末成绩");
        checkRange(s.getMakeupScore(), "补考成绩");
        if (s.getUsualScore() == null && s.getExamScore() == null && s.getMakeupScore() == null) {
            throw new BusinessException("ERR-310", "平时成绩与期末成绩至少填写一项");
        }
        if (s.getMakeupScore() != null) {
            s.setIsRetake(Boolean.TRUE);
        }
    }

    /**
     * 校验分数取值范围 0~100。
     *
     * @param score 分数
     * @param name  字段名称
     * @throws BusinessException 越界
     */
    private void checkRange(Double score, String name) {
        if (score == null) {
            return;
        }
        if (score < 0 || score > 100) {
            throw new BusinessException("ERR-311", name + "取值必须在 0~100 之间");
        }
    }

    /**
     * 字符串转分数。
     *
     * @param text 文本
     * @return 分数，空串返回 null
     * @throws BusinessException 格式错误
     */
    private Double parseScore(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            throw new BusinessException("ERR-312", "分数格式不正确：" + text);
        }
    }

    /**
     * 对象转分数。
     *
     * @param value 值
     * @return 分数
     */
    private Double toDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return parseScore(value.toString());
    }

    /**
     * 生成成绩描述文本（日志与提示使用）。
     *
     * @param s 成绩对象
     * @return 描述文本
     */
    private String describe(Score s) {
        Student student = studentDao.findById(s.getStudentId());
        Course course = courseDao.findById(s.getCourseId());
        return (student == null ? "" : student.getSno() + " " + student.getName())
                + " / " + (course == null ? "" : course.getCourseName())
                + " 总评=" + s.calcTotal();
    }
}
