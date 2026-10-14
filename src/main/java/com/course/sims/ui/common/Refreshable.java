package com.course.sims.ui.common;

/**
 * 可刷新界面组件接口。
 *
 * <p>所有业务面板都实现该接口，主窗体在切换菜单或点击"刷新"按钮时统一调用
 * {@link #refreshData()}，使数据刷新成为横向统一行为。</p>
 *
 * @author 课程设计第 2 小组 - 成员 D
 * @version 1.0.0
 */
public interface Refreshable {

    /**
     * 重新从业务层加载数据并刷新界面。
     */
    void refreshData();
}
