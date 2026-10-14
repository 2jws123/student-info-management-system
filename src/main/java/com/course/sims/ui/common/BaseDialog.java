package com.course.sims.ui.common;

import com.course.sims.config.UiTheme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 表单对话框基类（模板方法 + 建造者风格）。
 *
 * <p>提供统一的对话框骨架：顶部标题、中部两列表单网格（标签 + 输入组件）、
 * 底部"保存/取消"按钮，并以 {@link #addField(String, JComponent, boolean)} 收集字段，
 * 使各模块的编辑对话框代码量大幅下降（新增一个对话框通常不超过 60 行）。</p>
 *
 * @author 课程设计第 2 小组 - 成员 D
 * @version 1.0.0
 */
public abstract class BaseDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    /** 表单网格面板 */
    protected final JPanel formPanel = new JPanel(new GridBagLayout());
    /** 表单字段（标签 -> 组件），保持插入顺序 */
    private final Map<String, JComponent> fields = new LinkedHashMap<>();
    /** 底部按钮区 */
    protected final JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
    /** 对话框标题文本 */
    private final String dialogTitle;
    /** 保存后是否成功（供调用方判断是否需要刷新列表） */
    private boolean saved;

    /**
     * 构造对话框。
     *
     * @param owner 父窗口
     * @param title 标题
     */
    protected BaseDialog(Window owner, String title) {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        this.dialogTitle = title;
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(UiTheme.boldFont(16f));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(14, 18, 10, 18));
        add(titleLabel, BorderLayout.NORTH);

        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(BorderFactory.createEmptyBorder(6, 18, 10, 18));
        add(formPanel, BorderLayout.CENTER);

        buttonBar.setBackground(Color.WHITE);
        add(buttonBar, BorderLayout.SOUTH);
    }

    /**
     * 初始化对话框：装配字段、按钮，并完成数据回填。
     * <p>子类构造方法末尾调用。</p>
     */
    protected void initDialog() {
        buildForm();
        buttonBar.add(UiUtil.button("保存", UiTheme.PRIMARY));
        buttonBar.add(UiUtil.button("取消", new Color(0x6B7280)));
        ((javax.swing.JButton) buttonBar.getComponent(0)).addActionListener(e -> onSave());
        ((javax.swing.JButton) buttonBar.getComponent(1)).addActionListener(e -> dispose());
        loadDataToForm();
        pack();
        setMinimumSize(new Dimension(Math.max(getWidth(), 460), getHeight()));
        setLocationRelativeTo(getOwner());
    }

    /**
     * 构建表单字段（子类通过 {@link #addField} 添加）。
     */
    protected abstract void buildForm();

    /**
     * 保存按钮回调（子类实现校验与持久化逻辑）。
     */
    protected abstract void onSave();

    /**
     * 数据回填（子类覆写，把实体值写入组件）。
     */
    protected void loadDataToForm() {
        // 默认无需回填
    }

    /**
     * 向表单添加一行。
     *
     * @param label     标签文字
     * @param component 输入组件
     * @param required  是否必填（必填会在标签后显示红色星号）
     */
    protected void addField(String label, JComponent component, boolean required) {
        int row = fields.size();
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row;
        labelConstraints.anchor = GridBagConstraints.EAST;
        labelConstraints.insets = new Insets(6, 4, 6, 10);
        JLabel fieldLabel = required ? UiUtil.requiredLabel(label) : UiUtil.formLabel(label);
        formPanel.add(fieldLabel, labelConstraints);

        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridx = 1;
        fieldConstraints.gridy = row;
        fieldConstraints.weightx = 1.0;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.insets = new Insets(6, 0, 6, 4);
        formPanel.add(component, fieldConstraints);
        fields.put(label, component);
    }

    /**
     * 向表单添加一行（可选，默认非必填）。
     *
     * @param label     标签文字
     * @param component 输入组件
     */
    protected void addField(String label, JComponent component) {
        addField(label, component, false);
    }

    /**
     * 添加一个占满整行的说明文字。
     *
     * @param text 文字
     */
    protected void addHint(String text) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = fields.size() + 1;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(8, 4, 4, 4);
        JLabel label = new JLabel("<html><body style='width:320px;color:#6B7280'>" + text + "</body></html>");
        label.setFont(UiTheme.pickChineseFont(12f));
        formPanel.add(label, c);
    }

    /**
     * 添加垂直间距。
     *
     * @param height 高度
     */
    protected void addSpacer(int height) {
        formPanel.add(Box.createVerticalStrut(height));
    }

    /**
     * 在表单中插入一个自定义行（跨两列）。
     *
     * @param component 组件
     */
    protected void addFullRow(JComponent component) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = fields.size() + 1;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(6, 4, 6, 4);
        formPanel.add(component, c);
    }

    /**
     * 获取表单字段组件。
     *
     * @param label 标签文字
     * @return 组件，不存在返回 null
     */
    protected JComponent getField(String label) {
        return fields.get(label);
    }

    /**
     * 标记保存成功并关闭对话框。
     */
    protected void markSavedAndClose() {
        saved = true;
        dispose();
    }

    /**
     * 判断是否保存成功。
     *
     * @return 保存成功返回 true
     */
    public boolean isSaved() {
        return saved;
    }

    /**
     * 以垂直盒式布局创建一个"标签在上、组件在下"的分组面板。
     *
     * @param label     标签
     * @param component 组件
     * @return 分组面板
     */
    protected JPanel group(String label, JComponent component) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        JLabel l = UiUtil.formLabel(label);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(l);
        panel.add(Box.createVerticalStrut(3));
        panel.add(component);
        return panel;
    }

    /**
     * 获取父窗口（可能为 Frame 或 Dialog）。
     *
     * @return 父窗口
     */
    protected Window ownerWindow() {
        return getOwner();
    }

    /**
     * 转换为 Frame（用于父窗口为 Frame 的场景）。
     *
     * @return Frame，非 Frame 返回 null
     */
    protected Frame ownerFrame() {
        return getOwner() instanceof Frame ? (Frame) getOwner() : null;
    }
}
