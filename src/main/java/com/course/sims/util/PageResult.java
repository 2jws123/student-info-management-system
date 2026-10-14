package com.course.sims.util;

import java.util.ArrayList;
import java.util.List;

/**
 * 通用分页结果封装类。
 * <p>所有列表查询统一返回该对象，界面层据此渲染表格与分页条，
 * 使"分页"成为系统的横向公共能力而不是各模块重复实现。</p>
 *
 * @param <T> 记录类型
 * @author 课程设计第 2 小组 - 成员 A（公共模块）
 * @version 1.0.0
 */
public class PageResult<T> {

    /** 当前页数据 */
    private final List<T> rows;
    /** 满足条件的总记录数 */
    private final long total;
    /** 当前页码，从 1 开始 */
    private final int pageNo;
    /** 每页条数 */
    private final int pageSize;

    /**
     * 构造分页结果。
     *
     * @param rows     当前页数据
     * @param total    总记录数
     * @param pageNo   当前页码（从 1 开始）
     * @param pageSize 每页条数
     */
    public PageResult(List<T> rows, long total, int pageNo, int pageSize) {
        this.rows = rows == null ? new ArrayList<>() : rows;
        this.total = total;
        this.pageNo = Math.max(pageNo, 1);
        this.pageSize = pageSize <= 0 ? 15 : pageSize;
    }

    /**
     * 构造空分页结果。
     *
     * @param pageNo   页码
     * @param pageSize 每页条数
     * @param <T>      记录类型
     * @return 空分页结果
     */
    public static <T> PageResult<T> empty(int pageNo, int pageSize) {
        return new PageResult<>(new ArrayList<>(), 0L, pageNo, pageSize);
    }

    /** @return 当前页数据 */
    public List<T> getRows() {
        return rows;
    }

    /** @return 总记录数 */
    public long getTotal() {
        return total;
    }

    /** @return 当前页码 */
    public int getPageNo() {
        return pageNo;
    }

    /** @return 每页条数 */
    public int getPageSize() {
        return pageSize;
    }

    /** @return 总页数，无数据时为 1 */
    public int getTotalPages() {
        if (total <= 0) {
            return 1;
        }
        return (int) ((total + pageSize - 1) / pageSize);
    }

    /** @return 是否存在上一页 */
    public boolean hasPrev() {
        return pageNo > 1;
    }

    /** @return 是否存在下一页 */
    public boolean hasNext() {
        return pageNo < getTotalPages();
    }

    @Override
    public String toString() {
        return "PageResult{pageNo=" + pageNo + ", pageSize=" + pageSize
                + ", total=" + total + ", rows=" + rows.size() + "}";
    }
}
