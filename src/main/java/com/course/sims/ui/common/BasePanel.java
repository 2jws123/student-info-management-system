package com.course.sims.ui.common;

import com.course.sims.config.Session;
import com.course.sims.config.UiTheme;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

/**
 * 业务模块面板基类（模板方法模式）。
 *
 * <p>把 10 个功能模块高度重复的界面骨架收敛到本类：</p>
 * <ul>
 *   <li>顶部：标题 + 查询条件区（由子类通过 {@link #buildQueryBar} 定制）；</li>
 *   <li>中部：带边框、斑马纹、居中对齐的数据表格；</li>
 *   <li>底部：操作按钮 + 分页条（首页/上一页/下一页/末页 + 每页条数）。</li>
 * </ul>
 *
 * <p>子类只需实现 {@link #configureColumns()}（定义列）、
 * {@link #loadData()}（查询数据）、{@link #onAdd()} / {@link #onEdit()} / {@link #onDelete()}
 * 等钩子方法即可，无需重复编写分页与表格逻辑。</p>
 *
 * @author 课程设计第 2 小组 - 成员 D
 * @version 1.0.0
 */
public abstract class BasePanel extends JPanel implements Refreshable {

    private static final long serialVersionUID = 1L;

    /** 数据表格 */
    protected final JTable table = new JTable();
    /** 表格模型 */
    protected final DefaultTableModel tableModel = new DefaultTableModel() {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    /** 查询条件容器 */
    protected final JPanel queryBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
    /** 操作按钮容器 */
    protected final JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
    /** 分页条容器 */
    protected final JPanel pageBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
    /** 状态提示标签 */
    protected final JLabel statusLabel = new JLabel("就绪");
    /** 每页条数下拉框 */
    protected final JComboBox<Integer> pageSizeBox = new JComboBox<>(new Integer[]{10, 15, 20, 30, 50});

    /** 当前页码 */
    protected int pageNo = 1;
    /** 每页条数 */
    protected int pageSize = 15;
    /** 总记录数 */
    protected long totalCount = 0L;
    /** 总页数 */
    protected int totalPages = 1;
    /** 分页信息标签 */
    private final JLabel pageInfoLabel = new JLabel();
    /** 按钮集合，便于统一按权限控制显隐 */
    private final List<JButton> actionButtons = new ArrayList<>();

    /**
     * 构造面板：初始化骨架（子类构造方法中调用 super 后应立刻调用 initUi()）。
     *
     * @param title 面板标题
     */
    protected BasePanel(String title) {
        setLayout(new BorderLayout());
        setBackground(UiTheme.PAGE_BG);
        setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(UiTheme.boldFont(18f));
        titleLabel.setForeground(new Color(0x1F2937));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(2, 2, 8, 2));

        queryBar.setOpaque(false);
        buttonBar.setOpaque(false);
        pageBar.setOpaque(false);
        statusLabel.setForeground(new Color(0x6B7280));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(titleLabel, BorderLayout.NORTH);
        top.add(queryBar, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);
    }

    /**
     * 初始化界面：装配表格、按钮条与分页条，并在末尾执行首次数据加载。
     * <p>子类构造方法中在设置完组件之后调用。</p>
     */
    protected void initUi() {
        configureColumns();
        table.setModel(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(28);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(true);
        table.setGridColor(new Color(0xE5E7EB));
        table.setIntercellSpacing(new Dimension(0, 1));
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setFont(UiTheme.boldFont(13f));
        table.setFont(UiTheme.pickChineseFont(13f));
        table.setFillsViewportHeight(true);
        applyColumnStyle();

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(0xE5E7EB)));
        scrollPane.getViewport().setBackground(Color.WHITE);

        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.add(scrollPane, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(buttonBar, BorderLayout.WEST);
        bottom.add(pageBar, BorderLayout.EAST);

        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.setOpaque(false);
        centerWrapper.add(center, BorderLayout.CENTER);
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
        statusBar.setOpaque(false);
        statusBar.add(statusLabel);
        centerWrapper.add(statusBar, BorderLayout.SOUTH);

        add(centerWrapper, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        buildQueryBar();
        buildButtons();
        buildPageBar();
        refreshData();
    }

    /**
     * 定义表格列（子类实现）。
     */
    protected abstract void configureColumns();

    /**
     * 从业务层查询数据并填充表格与分页信息（子类实现）。
     * <p>子类应在方法内调用 {@link #setPage(com.course.sims.util.PageResult)} 回填分页状态。</p>
     */
    protected abstract void loadData();

    // ------------------------------------------------------------------
    //  子类可覆写的钩子方法
    // ------------------------------------------------------------------

    /**
     * 构建查询条件区组件（子类覆写）。
     */
    protected void buildQueryBar() {
        // 默认无条件
    }

    /**
     * 构建操作按钮（子类覆写）。
     */
    protected void buildButtons() {
        addButton("新增", UiTheme.PRIMARY, e -> onAdd());
        addButton("修改", new Color(0x0EA5E9), e -> onEdit());
        addButton("删除", UiTheme.DANGER, e -> onDelete());
        addButton("刷新", new Color(0x6B7280), e -> refreshData());
    }

    /**
     * 新增操作（子类覆写）。
     */
    protected void onAdd() {
        // 默认不处理
    }

    /**
     * 修改操作（子类覆写）。
     */
    protected void onEdit() {
        // 默认不处理
    }

    /**
     * 删除操作（子类覆写）。
     */
    protected void onDelete() {
        // 默认不处理
    }

    /**
     * 双击表格行操作（子类覆写，默认执行修改）。
     */
    protected void onRowDoubleClick() {
        onEdit();
    }

    /**
     * 获取选中行对应的主键（子类覆写，默认读取第 0 列并尝试解析为整数）。
     *
     * @return 主键，未选中返回 null
     */
    protected Integer getSelectedId() {
        int row = table.getSelectedRow();
        if (row < 0) {
            return null;
        }
        int modelRow = table.convertRowIndexToModel(row);
        Object value = tableModel.getValueAt(modelRow, 0);
        try {
            return Integer.valueOf(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ------------------------------------------------------------------
    //  公共能力
    // ------------------------------------------------------------------

    /**
     * 构建分页条。
     */
    private void buildPageBar() {
        pageSizeBox.setSelectedItem(pageSize);
        pageSizeBox.addActionListener(e -> {
            Integer selected = (Integer) pageSizeBox.getSelectedItem();
            if (selected != null) {
                pageSize = selected;
                pageNo = 1;
                refreshData();
            }
        });
        JButton first = smallButton("首页", e -> gotoPage(1));
        JButton prev = smallButton("上一页", e -> gotoPage(pageNo - 1));
        JButton next = smallButton("下一页", e -> gotoPage(pageNo + 1));
        JButton last = smallButton("末页", e -> gotoPage(totalPages));
        pageBar.add(new JLabel("每页"));
        pageBar.add(pageSizeBox);
        pageBar.add(first);
        pageBar.add(prev);
        pageBar.add(pageInfoLabel);
        pageBar.add(next);
        pageBar.add(last);
    }

    /**
     * 跳转到指定页。
     *
     * @param target 目标页码
     */
    protected void gotoPage(int target) {
        if (target < 1 || target > totalPages) {
            return;
        }
        pageNo = target;
        refreshData();
    }

    /**
     * 创建小尺寸翻页按钮。
     *
     * @param text   按钮文字
     * @param action 点击事件
     * @return 按钮
     */
    private JButton smallButton(String text, java.awt.event.ActionListener action) {
        JButton button = new JButton(text);
        button.setFont(UiTheme.pickChineseFont(12f));
        button.setFocusPainted(false);
        button.addActionListener(action);
        return button;
    }

    /**
     * 向操作区添加按钮，并按当前用户权限自动控制显隐。
     *
     * @param text   文字
     * @param color  背景色
     * @param action 事件
     * @return 按钮
     */
    protected JButton addButton(String text, Color color, java.awt.event.ActionListener action) {
        JButton button = new JButton(text);
        button.setFont(UiTheme.pickChineseFont(13f));
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));
        button.addActionListener(action);
        buttonBar.add(button);
        actionButtons.add(button);
        return button;
    }

    /**
     * 按角色控制增删改按钮是否可用（学生角色只读）。
     */
    protected void applyRolePermission() {
        boolean maintain = Session.canMaintain();
        for (JButton button : actionButtons) {
            String text = button.getText();
            if ("新增".equals(text) || "修改".equals(text) || "删除".equals(text)
                    || "批量导入".equals(text) || "批量登记".equals(text) || "一键全勤".equals(text)
                    || "重置演示数据".equals(text)) {
                button.setEnabled(maintain);
            }
        }
    }

    /**
     * 设置列宽（子类可覆写）。
     *
     * @param columnModel 列模型
     */
    protected void configureColumnWidths(TableColumnModel columnModel) {
        // 默认按内容自适应
    }

    /**
     * 应用表格列样式：数字右对齐、其余居中。
     */
    private void applyColumnStyle() {
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
        configureColumnWidths(table.getColumnModel());
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    onRowDoubleClick();
                }
            }
        });
    }

    /**
     * 回填分页状态并刷新分页标签。
     *
     * @param page 分页结果
     */
    protected void setPage(com.course.sims.util.PageResult<?> page) {
        this.totalCount = page.getTotal();
        this.totalPages = page.getTotalPages();
        this.pageNo = Math.min(page.getPageNo(), totalPages);
        pageInfoLabel.setText("第 " + this.pageNo + " / " + totalPages + " 页");
        statusLabel.setText("共 " + totalCount + " 条记录");
    }

    /**
     * 清空表格数据。
     */
    protected void clearTable() {
        tableModel.setRowCount(0);
    }

    /**
     * 添加一行数据。
     *
     * @param row 行数据
     */
    protected void addRow(Object... row) {
        tableModel.addRow(row);
    }

    /**
     * 获取表格模型当前行数。
     *
     * @return 行数
     */
    protected int rowCount() {
        return tableModel.getRowCount();
    }

    @Override
    public void refreshData() {
        try {
            clearTable();
            loadData();
            applyRolePermission();
        } catch (RuntimeException e) {
            UiUtil.error(this, "加载数据失败：" + e.getMessage());
        }
    }

    /**
     * 创建查询条件标签。
     *
     * @param text 文本
     * @return 标签
     */
    protected JLabel queryLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(UiTheme.pickChineseFont(13f));
        return label;
    }

    /**
     * 创建查询文本输入框。
     *
     * @param width 宽度（像素）
     * @return 输入框
     */
    protected JTextField queryField(int width) {
        JTextField field = new JTextField(width);
        field.setFont(UiTheme.pickChineseFont(13f));
        field.setPreferredSize(new Dimension(width, 28));
        return field;
    }

    /**
     * 创建查询下拉框。
     *
     * @param items 选项
     * @param width 宽度
     * @return 下拉框
     */
    protected JComboBox<Object> queryCombo(Object[] items, int width) {
        Object[] data = new Object[items.length + 1];
        data[0] = "全部";
        System.arraycopy(items, 0, data, 1, items.length);
        JComboBox<Object> combo = new JComboBox<>(data);
        combo.setFont(UiTheme.pickChineseFont(13f));
        combo.setPreferredSize(new Dimension(width, 28));
        return combo;
    }

    /**
     * 判断下拉框是否选择了"全部"。
     *
     * @param combo 下拉框
     * @return 选择"全部"或未选择返回 true
     */
    protected boolean isAllSelected(JComboBox<?> combo) {
        Object value = combo.getSelectedItem();
        return value == null || "全部".equals(value);
    }

    /**
     * 获取下拉框选项值（"全部"返回 null）。
     *
     * @param combo 下拉框
     * @return 选项值
     */
    protected String comboValue(JComboBox<?> combo) {
        Object value = combo.getSelectedItem();
        return isAllSelected(combo) ? null : String.valueOf(value);
    }

    /**
     * 获取下拉框选项对应的实体主键（选项文本需为 "id-名称" 形式或对象数组第二项）。
     *
     * @param combo 下拉框
     * @return 主键，选择"全部"返回 null
     */
    protected Integer comboId(JComboBox<?> combo) {
        Object value = combo.getSelectedItem();
        if (value == null || "全部".equals(value)) {
            return null;
        }
        if (value instanceof Object[]) {
            Object id = ((Object[]) value)[0];
            if (id == null) {
                return null;
            }
            try {
                return Integer.valueOf(String.valueOf(id));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 添加一个带标签与组件的查询条件。
     *
     * @param label 标签文字
     * @param field 组件
     */
    protected void addQueryItem(String label, Component field) {
        queryBar.add(queryLabel(label));
        queryBar.add(field);
    }

    /**
     * 应用统一字体到子类扩展组件。
     *
     * @param font  字体
     * @param items 组件
     */
    protected void applyFont(Font font, Component... items) {
        for (Component c : items) {
            c.setFont(font);
        }
    }

    /**
     * 弹出确认对话框。
     *
     * @param message 提示信息
     * @return 用户选择"是"返回 true
     */
    protected boolean confirm(String message) {
        return JOptionPane.showConfirmDialog(this, message, "操作确认",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    /**
     * 弹出信息提示。
     *
     * @param message 提示信息
     */
    protected void info(String message) {
        UiUtil.info(this, message);
    }

    /**
     * 弹出错误提示。
     *
     * @param message 提示信息
     */
    protected void error(String message) {
        UiUtil.error(this, message);
    }
}
