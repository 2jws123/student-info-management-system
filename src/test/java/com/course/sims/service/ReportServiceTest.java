package com.course.sims.service;

import com.course.sims.BaseTest;
import com.course.sims.entity.Attendance;
import com.course.sims.entity.Course;
import com.course.sims.entity.Score;
import com.course.sims.entity.Student;
import com.course.sims.util.ExcelUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 数据导出与报表测试（模块 M8）。
 *
 * @author 课程设计第 2 小组 - 成员 E
 * @version 1.0.0
 */
@DisplayName("模块 M8：数据导出与报表测试")
class ReportServiceTest extends BaseTest {

    private final ReportService reportService = new ReportService();
    private final StudentService studentService = new StudentService();
    private final TeachingService teachingService = new TeachingService();
    private final AttendanceService attendanceService = new AttendanceService();
    private final ScoreService scoreService = new ScoreService();

    /**
     * 取一名演示学生。
     *
     * @return 学生对象
     */
    /**
     * 取第一名"有成绩记录"的学生（成绩单导出必须有成绩才有内容）。
     *
     * @return 学生对象
     */
    private Student anyStudent() {
        Score first = scoreService.findByPage(null, null, null, null, null, 1, 1).getRows().get(0);
        return studentService.findById(first.getStudentId());
    }

    // ------------------------------------------------------------------
    //  TC-G-01 ~ TC-G-06  导出功能
    // ------------------------------------------------------------------

    @Test
    @DisplayName("TC-G-01 导出学生名册：文件生成、行数与查询结果一致、含中文表头")
    void testExportStudents(@TempDir Path tempDir) throws Exception {
        File file = tempDir.resolve("学生名册.xlsx").toFile();
        long expected = studentService.findByPage(null, null, null, null, 1, 15).getTotal();
        int count = reportService.exportStudents(file, null, null, null);
        assertEquals(expected, count, "导出行数应与查询结果一致");
        assertTrue(file.exists() && file.length() > 0, "导出文件未生成或为空");
        List<String[]> rows = ExcelUtil.read(file, true);
        assertEquals(expected, rows.size(), "读回的 Excel 数据行数不正确");
        List<Student> expectedList = studentService.findList(null, null, null);
        assertEquals(expectedList.get(0).getSno(), rows.get(0)[0], "第一列应为学号");
        assertEquals(expectedList.get(0).getName(), rows.get(0)[1], "第二列应为姓名");
        assertTrue(rows.get(0).length >= 12, "学生名册应包含 12 列");
    }

    @Test
    @DisplayName("TC-G-02 导出学生名册：关键字筛选后只导出筛选结果")
    void testExportStudentsWithFilter(@TempDir Path tempDir) throws Exception {
        File file = tempDir.resolve("筛选名册.xlsx").toFile();
        int count = reportService.exportStudents(file, "软件", null, null);
        List<String[]> rows = ExcelUtil.read(file, true);
        assertEquals(count, rows.size(), "导出行数与返回条数不一致");
        for (String[] row : rows) {
            assertTrue(row[5].contains("软件") || row[6].contains("软件"),
                    "筛选后不应出现不匹配的专业/班级：" + row[5] + " / " + row[6]);
        }
    }

    @Test
    @DisplayName("TC-G-03 导出成绩表：75 条成绩全部导出且含总评与绩点列")
    void testExportScores(@TempDir Path tempDir) throws Exception {
        File file = tempDir.resolve("成绩表.xlsx").toFile();
        int count = reportService.exportScores(file, null, null, null, null);
        assertEquals(75, count, "应导出全部 75 条成绩");
        List<String[]> rows = ExcelUtil.read(file, true);
        assertEquals(75, rows.size(), "读回的成绩行数不正确");
        assertEquals(15, rows.get(0).length, "成绩表应包含 15 列（含总评、绩点、等级）");
    }

    @Test
    @DisplayName("TC-G-04 导出成绩表：按课程筛选生效")
    void testExportScoresFiltered(@TempDir Path tempDir) throws Exception {
        File file = tempDir.resolve("单科成绩.xlsx").toFile();
        Course course = teachingService.findAllCourses().get(0);
        int count = reportService.exportScores(file, null, null, course.getId(), null);
        assertEquals(15, count, "单门课程应有 15 名学生的成绩");
        List<String[]> rows = ExcelUtil.read(file, true);
        assertEquals(15, rows.size(), "单科成绩导出明细行数不正确");
        for (String[] row : rows) {
            assertEquals(course.getCourseName(), row[4], "筛选后课程名称应一致");
        }
    }

    @Test
    @DisplayName("TC-G-05 导出学生成绩单：含学生信息标题与课程明细")
    void testExportTranscript(@TempDir Path tempDir) throws Exception {
        Student student = anyStudent();
        File file = tempDir.resolve("成绩单.xlsx").toFile();
        int count = reportService.exportTranscript(file, student.getId(), null);
        assertTrue(count > 0, "成绩单应包含课程记录");
        List<String[]> rows = ExcelUtil.read(file, true);
        assertTrue(rows.size() >= count, "读回的数据行数不应少于课程数");
        assertEquals(11, rows.get(0).length, "成绩单应为 11 列");
    }

    @Test
    @DisplayName("TC-G-06 导出考勤表：行数与考勤记录数一致")
    void testExportAttendance(@TempDir Path tempDir) throws Exception {
        File file = tempDir.resolve("考勤表.xlsx").toFile();
        long expected = attendanceService.findByPage(null, null, null, null, null, null, 1, 15).getTotal();
        int count = reportService.exportAttendance(file, null, null, null, null);
        assertEquals(expected, count, "导出的考勤条数与查询结果不一致");
        List<String[]> rows = ExcelUtil.read(file, true);
        assertEquals(expected, rows.size(), "读回的考勤行数不正确");
        boolean validStatus = false;
        for (String status : Attendance.ALL_STATUS) {
            if (status.equals(rows.get(0)[4])) {
                validStatus = true;
                break;
            }
        }
        assertTrue(validStatus, "第 5 列应为合法考勤状态，实际：" + rows.get(0)[4]);
    }

    @Test
    @DisplayName("TC-G-07 导出统计分析报表：多工作表结构正确且数据完整")
    void testExportStatistics(@TempDir Path tempDir) throws Exception {
        File file = tempDir.resolve("统计分析报表.xlsx").toFile();
        int count = reportService.exportStatistics(file);
        assertTrue(count >= 7, "综合概览应至少包含 7 项指标");
        assertTrue(file.exists() && file.length() > 0, "统计报表文件未生成");
        // 校验多工作表结构
        try (var in = new java.io.FileInputStream(file);
             var wb = org.apache.poi.ss.usermodel.WorkbookFactory.create(in)) {
            assertEquals(5, wb.getNumberOfSheets(), "统计报表应包含 5 个工作表");
            assertEquals("综合概览", wb.getSheetName(0));
            assertEquals("班级平均分排名", wb.getSheetName(1));
            assertEquals("课程成绩统计", wb.getSheetName(2));
            assertEquals("成绩等级分布", wb.getSheetName(3));
            assertEquals("专业人数分布", wb.getSheetName(4));
            var classSheet = wb.getSheetAt(1);
            assertTrue(classSheet.getLastRowNum() >= 1, "班级排名工作表应有数据行");
        }
    }

    @Test
    @DisplayName("TC-G-08 导出数据看板：核心指标齐全")
    void testExportDashboard(@TempDir Path tempDir) throws Exception {
        File file = tempDir.resolve("数据看板.xlsx").toFile();
        int count = reportService.exportDashboard(file);
        assertEquals(12, count, "看板应导出 12 项指标");
        List<String[]> rows = ExcelUtil.read(file, true);
        assertEquals(12, rows.size(), "读回的看板指标行数不正确");
        assertEquals("学生总数", rows.get(0)[0], "第一项指标应为学生总数");
        assertEquals("40", rows.get(0)[1], "学生总数应为 40");
    }

    // ------------------------------------------------------------------
    //  TC-G-09 ~ TC-G-10  Excel 读取（导入功能依赖）
    // ------------------------------------------------------------------

    @Test
    @DisplayName("TC-G-09 Excel 读取：整数分数不带小数点，文本正确还原")
    void testExcelRead(@TempDir Path tempDir) throws Exception {
        File file = tempDir.resolve("导入样例.xlsx").toFile();
        ExcelUtil.export(file, "成绩", null, new String[]{"学号", "平时成绩", "期末成绩", "补考成绩"},
                List.of(new Object[]{"2024001", 85.0, 90.5, null},
                        new Object[]{"2024002", 78, 82.0, 80.0}));
        List<String[]> rows = ExcelUtil.read(file, true);
        assertEquals(2, rows.size(), "应读回 2 行数据");
        assertEquals("2024001", rows.get(0)[0], "学号文本不应被改写");
        assertEquals("85", rows.get(0)[1], "整数分数不应出现多余小数位");
        assertEquals("90.5", rows.get(0)[2], "小数分应正确保留");
        assertEquals("", rows.get(0)[3], "空单元格应读取为空串");
    }

    @Test
    @DisplayName("TC-G-10 Excel 读取：跳过空行，不把空行当作数据")
    void testExcelReadSkipBlankRows(@TempDir Path tempDir) throws Exception {
        File file = tempDir.resolve("含空行.xlsx").toFile();
        ExcelUtil.export(file, "测试", null, new String[]{"A", "B"},
                List.of(new Object[]{"1", "2"}, new Object[]{"", ""}, new Object[]{"3", "4"}));
        List<String[]> rows = ExcelUtil.read(file, true);
        assertEquals(2, rows.size(), "全空行应被跳过");
        assertNotNull(rows.get(1)[0], "第二条数据行不应为空");
    }
}
