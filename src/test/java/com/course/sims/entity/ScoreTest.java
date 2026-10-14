package com.course.sims.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 成绩实体计算逻辑单元测试。
 *
 * <p>重点验证系统最核心的业务算法：总评成绩计算、绩点换算、成绩等级判定。
 * 这些算法同时被 Java 业务层与数据库计算列实现，必须保持完全一致。</p>
 *
 * @author 课程设计第 2 小组 - 成员 E
 * @version 1.0.0
 */
@DisplayName("成绩算法测试（总评 / 绩点 / 等级）")
class ScoreTest {

    /**
     * 构造指定分数的成绩对象。
     *
     * @param usual  平时成绩
     * @param exam   期末成绩
     * @param makeup 补考成绩
     * @return 成绩对象
     */
    private Score build(Double usual, Double exam, Double makeup) {
        Score score = new Score();
        score.setUsualScore(usual);
        score.setExamScore(exam);
        score.setMakeupScore(makeup);
        return score;
    }

    // ------------------------------------------------------------------
    //  TC-S-01 ~ TC-S-06  总评成绩计算
    // ------------------------------------------------------------------

    @Test
    @DisplayName("TC-S-01 总评成绩 = 平时×0.3 + 期末×0.7（88 / 92 → 90.8）")
    void testCalcTotal() {
        assertEquals(90.8D, build(88.0, 92.0, null).calcTotal(), 0.001D, "总评成绩计算不正确");
    }

    @Test
    @DisplayName("TC-S-02 总评成绩四舍五入保留 1 位小数（85 / 78 → 80.1）")
    void testCalcTotalRounding() {
        assertEquals(80.1D, build(85.0, 78.0, null).calcTotal(), 0.001D, "总评成绩舍入不正确");
    }

    @Test
    @DisplayName("TC-S-03 存在补考成绩时以补考成绩为准（平时70 期末50 补考66 → 66）")
    void testCalcTotalWithMakeup() {
        assertEquals(66.0D, build(70.0, 50.0, 66.0).calcTotal(), 0.001D, "补考成绩应覆盖总评成绩");
    }

    @Test
    @DisplayName("TC-S-04 仅有平时成绩时按 0 分计算期末（90 / null → 27.0）")
    void testCalcTotalOnlyUsual() {
        assertEquals(27.0D, build(90.0, null, null).calcTotal(), 0.001D, "缺考期末成绩应按 0 分参与计算");
    }

    @Test
    @DisplayName("TC-S-05 三项成绩均为空时总评成绩为 null")
    void testCalcTotalAllNull() {
        assertNull(build(null, null, null).calcTotal(), "无任何成绩时总评应为 null");
    }

    @Test
    @DisplayName("TC-S-06 满分与零分边界（100 / 100 → 100.0，0 / 0 → 0.0）")
    void testCalcTotalBoundary() {
        assertEquals(100.0D, build(100.0, 100.0, null).calcTotal(), 0.001D, "满分成绩计算不正确");
        assertEquals(0.0D, build(0.0, 0.0, null).calcTotal(), 0.001D, "零分成绩计算不正确");
    }

    // ------------------------------------------------------------------
    //  TC-S-07 ~ TC-S-12  绩点换算（四等五级制）
    // ------------------------------------------------------------------

    @Test
    @DisplayName("TC-S-07 绩点换算：90 分及以上为 4.0（含 90 边界）")
    void testGradePoint4() {
        Score score = build(95.0, 95.0, null);
        score.setTotalScore(95.0);
        assertEquals(4.0D, score.getGradePoint(), 0.001D, "95 分绩点应为 4.0");
        score.setTotalScore(90.0);
        assertEquals(4.0D, score.getGradePoint(), 0.001D, "90 分绩点应为 4.0");
    }

    @Test
    @DisplayName("TC-S-08 绩点换算：80~89 分为 3.0（含 80、89.9 边界）")
    void testGradePoint3() {
        Score score = build(80.0, 80.0, null);
        score.setTotalScore(80.0);
        assertEquals(3.0D, score.getGradePoint(), 0.001D, "80 分绩点应为 3.0");
        score.setTotalScore(89.9);
        assertEquals(3.0D, score.getGradePoint(), 0.001D, "89.9 分绩点应为 3.0");
    }

    @Test
    @DisplayName("TC-S-09 绩点换算：70~79 分为 2.0")
    void testGradePoint2() {
        Score score = build(70.0, 70.0, null);
        score.setTotalScore(75.0);
        assertEquals(2.0D, score.getGradePoint(), 0.001D, "75 分绩点应为 2.0");
    }

    @Test
    @DisplayName("TC-S-10 绩点换算：60~69 分为 1.0（含 60 边界）")
    void testGradePoint1() {
        Score score = build(60.0, 60.0, null);
        score.setTotalScore(60.0);
        assertEquals(1.0D, score.getGradePoint(), 0.001D, "60 分绩点应为 1.0");
    }

    @Test
    @DisplayName("TC-S-11 绩点换算：60 分以下为 0")
    void testGradePoint0() {
        Score score = build(50.0, 50.0, null);
        score.setTotalScore(59.9);
        assertEquals(0.0D, score.getGradePoint(), 0.001D, "59.9 分绩点应为 0");
    }

    @Test
    @DisplayName("TC-S-12 绩点换算：无总评成绩时按 0 处理，不抛异常")
    void testGradePointNull() {
        assertEquals(0.0D, build(null, null, null).getGradePoint(), 0.001D, "无成绩时绩点应为 0");
    }

    // ------------------------------------------------------------------
    //  TC-S-13 ~ TC-S-16  成绩等级与及格判定
    // ------------------------------------------------------------------

    @Test
    @DisplayName("TC-S-13 成绩等级判定：优 / 良 / 中 / 及格 / 不及格")
    void testScoreLevel() {
        Score score = build(0.0, 0.0, null);
        score.setTotalScore(95.0);
        assertEquals("优", score.getScoreLevel(), "95 分应为优");
        score.setTotalScore(85.0);
        assertEquals("良", score.getScoreLevel(), "85 分应为良");
        score.setTotalScore(75.0);
        assertEquals("中", score.getScoreLevel(), "75 分应为中");
        score.setTotalScore(65.0);
        assertEquals("及格", score.getScoreLevel(), "65 分应为及格");
        score.setTotalScore(50.0);
        assertEquals("不及格", score.getScoreLevel(), "50 分应为不及格");
    }

    @Test
    @DisplayName("TC-S-14 成绩等级判定：无成绩时显示未录入")
    void testScoreLevelNull() {
        assertEquals("未录入", build(null, null, null).getScoreLevel(), "无成绩时应显示未录入");
    }

    @Test
    @DisplayName("TC-S-15 及格判定：60 分为及格边界")
    void testIsPassed() {
        Score score = build(60.0, 60.0, null);
        score.setTotalScore(60.0);
        assertTrue(score.isPassed(), "60 分应判定为及格");
        score.setTotalScore(59.9);
        assertFalse(score.isPassed(), "59.9 分应判定为不及格");
    }

    @Test
    @DisplayName("TC-S-16 及格判定：未录入成绩时应判定为不及格而非抛异常")
    void testIsPassedNull() {
        assertFalse(build(null, null, null).isPassed(), "无成绩时应判定为不及格");
    }
}
