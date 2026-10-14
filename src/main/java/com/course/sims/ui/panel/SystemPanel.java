package com.course.sims.ui.panel;

import com.course.sims.config.UiTheme;
import com.course.sims.entity.Config;
import com.course.sims.entity.SysLog;
import com.course.sims.service.BusinessException;
import com.course.sims.service.SystemService;
import com.course.sims.ui.common.BasePanel;
import com.course.sims.ui.common.UiUtil;
import com.course.sims.util.PageResult;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.MaskFormatter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.io.File;
import java.text.ParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统维护与日志面板（模块 M9 系统日志管理 + 模块 M10 系统维护与配置，仅系统管理员可见）。
 *
 * <p>主体是操作日志列表：可按关键字（操作人 / 操作类型 / 详情）与起止日期检索并分页浏览；
 * 围绕日志列表提供系统维护能力：数据库备份、重置演示数据、重建数据库文件、
 * 系统参数配置（弹出 {@link ConfigDialog}）、系统运行状态查看（弹出只读文本窗口）、清空日志。</p>
 *
 * <p>说明：数据库状态与日志列表无法同时容纳在一个表单中，因此状态信息通过独立弹窗展示。</p>
 *
 * @author 课程设计第 2 小组 - 成员 D
 * @version 1.0.0
 */
public class SystemPanel extends BasePanel {

    private static final long serialVersionUID = 1L;

    /**
     * 数据库文件路径（不含 .mv.db 后缀）。
     * <p>必须从 {@link com.course.sims.util.Db#getDatabasePath()} 动态获取，
     * 而不是写死 "db/sims"：否则通过 -Dsims.db.path 指定了其它数据库位置时，
     * "重建数据库"会误删默认路径下的文件。</p>
     */
    private static String dbPath() {
        return com.course.sims.util.Db.getDatabasePath();
    }
    /** 日期掩码格式 */
    private static final String DATE_PATTERN = "####-##-##";

    /** 系统维护业务服务 */
    private final SystemService systemService = new SystemService();

    /** 关键字输入框（操作人 / 操作类型 / 详情） */
    private final JTextField keywordField = queryField(16);

    /** 起始日期（yyyy-MM-dd） */
    private final JFormattedTextField startDayField = createDateField();

    /** 截止日期（yyyy-MM-dd） */
    private final JFormattedTextField endDayField = createDateField();

    /** 日期输入是否非法（非法时中止本次查询） */
    private boolean invalidDate;

    /**
     * 构造系统维护与日志面板。
     */
    public SystemPanel() {
        super("系统维护与日志");
        initUi();
    }

    /**
     * 创建日期输入框（掩码 yyyy-MM-dd）。
     * <p>{@link MaskFormatter} 构造方法会抛出 {@link ParseException}，此处已做异常保护：
     * 掩码非法时退化为普通文本框，保证界面仍可正常使用。</p>
     *
     * @return 日期输入框
     */
    private static JFormattedTextField createDateField() {
        JFormattedTextField field;
        try {
            field = new JFormattedTextField(new MaskFormatter(DATE_PATTERN));
        } catch (ParseException e) {
            field = new JFormattedTextField();
        }
        field.setColumns(10);
        field.setPreferredSize(new Dimension(100, 28));
        field.setFont(UiTheme.pickChineseFont(13f));
        field.setToolTipText("日期格式：yyyy-MM-dd，例如 2024-09-01");
        return field;
    }

    @Override
    protected void configureColumns() {
        tableModel.setColumnIdentifiers(new String[]{"ID", "操作时间", "操作人", "操作类型", "详情"});
    }

    @Override
    protected void buildQueryBar() {
        addQueryItem("关键字", keywordField);
        addQueryItem("起始日期", startDayField);
        addQueryItem("截止日期", endDayField);
        queryBar.add(queryLabel("（日期格式 yyyy-MM-dd）"));

        JButton search = UiUtil.button("查询", UiTheme.PRIMARY);
        search.setFont(UiTheme.pickChineseFont(12f));
        search.addActionListener(e -> {
            pageNo = 1;
            refreshData();
        });

        JButton reset = UiUtil.button("重置条件", new Color(0x6B7280));
        reset.setFont(UiTheme.pickChineseFont(12f));
        reset.addActionListener(e -> {
            keywordField.setText("");
            startDayField.setValue(null);
            startDayField.setText("");
            endDayField.setValue(null);
            endDayField.setText("");
            pageNo = 1;
            refreshData();
        });

        queryBar.add(search);
        queryBar.add(reset);

        keywordField.addActionListener(e -> {
            pageNo = 1;
            refreshData();
        });
    }

    @Override
    protected void buildButtons() {
        // 本模块不涉及日志的新增 / 修改 / 删除，因此不添加默认的增删改按钮
        addButton("数据库备份", UiTheme.PRIMARY, e -> backupDatabase());
        addButton("重置演示数据", UiTheme.WARNING, e -> resetDemoData());
        addButton("重建数据库", UiTheme.DANGER, e -> resetDatabase());
        addButton("系统参数", new Color(0x0EA5E9), e -> showConfigDialog());
        addButton("系统状态", new Color(0x8B5CF6), e -> showSystemStatus());
        addButton("清空日志", UiTheme.DANGER, e -> clearLogs());
        addButton("刷新", new Color(0x6B7280), e -> refreshData());
    }

    @Override
    protected void loadData() {
        invalidDate = false;
        java.sql.Date startDay = parseDay(startDayField);
        java.sql.Date endDay = parseDay(endDayField);
        if (invalidDate) {
            // 日期非法时不再执行查询，避免把错误条件当作"无此条件"而返回误导性结果
            statusLabel.setText("日期格式不正确，未执行查询");
            return;
        }

        PageResult<SysLog> page = systemService.findLogs(keywordField.getText().trim(), startDay, endDay,
                pageNo, pageSize);
        for (SysLog log : page.getRows()) {
            addRow(log.getId(),
                    UiUtil.formatDateTime(log.getLogTime()),
                    log.getUsername(),
                    log.getOperation(),
                    log.getDetail());
        }
        setPage(page);
        hideIdColumn();
    }

    /**
     * 隐藏 ID 列并设置各列宽度（第 0 列为主键，仅供内部使用）。
     */
    private void hideIdColumn() {
        if (table.getColumnCount() > 0) {
            table.getColumnModel().getColumn(0).setMinWidth(0);
            table.getColumnModel().getColumn(0).setMaxWidth(0);
            table.getColumnModel().getColumn(0).setWidth(0);
        }
        int[] widths = {0, 160, 100, 120, 460};
        for (int i = 1; i < Math.min(widths.length, table.getColumnCount()); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    /**
     * 解析日期输入框内容。
     * <p>空串或含空格（掩码占位符）视为未填写，返回 null；
     * {@link java.sql.Date#valueOf(String)} 可能抛出 {@link IllegalArgumentException}，已捕获并给出提示。</p>
     *
     * @param field 日期输入框
     * @return 日期，未填写或格式非法返回 null
     */
    private java.sql.Date parseDay(JFormattedTextField field) {
        String text = field.getText() == null ? "" : field.getText().trim();
        if (text.isEmpty() || text.contains(" ")) {
            return null;
        }
        try {
            return java.sql.Date.valueOf(text);
        } catch (IllegalArgumentException e) {
            invalidDate = true;
            UiUtil.warn(this, "日期格式不正确，应为 yyyy-MM-dd，例如 2024-09-01");
            return null;
        }
    }

    /**
     * 数据库备份：选择保存位置后导出为 SQL 脚本。
     */
    private void backupDatabase() {
        File file = UiUtil.chooseSaveFile(this, systemService.defaultBackupFileName(), "SQL 脚本 (*.sql)", "sql");
        if (file == null) {
            return;
        }
        try {
            String path = systemService.backupDatabase(file);
            UiUtil.success(this, "数据库备份成功\n备份文件：" + path);
            refreshData();
        } catch (BusinessException e) {
            UiUtil.warn(this, e.getMessage());
        } catch (RuntimeException e) {
            UiUtil.error(this, "数据库备份失败：" + e.getMessage());
        }
    }

    /**
     * 重置演示数据：清空业务表并重新灌入初始演示数据（不删除数据库文件）。
     */
    private void resetDemoData() {
        if (!confirm("确认重置演示数据？\n该操作会清空系统全部业务数据（含用户与日志）并重新灌入初始演示数据，且不可恢复。")) {
            return;
        }
        try {
            String message = systemService.resetDemoData();
            UiUtil.success(this, message);
            refreshData();
        } catch (BusinessException e) {
            UiUtil.warn(this, e.getMessage());
        } catch (RuntimeException e) {
            UiUtil.error(this, "重置演示数据失败：" + e.getMessage());
        }
    }

    /**
     * 重建数据库：删除数据库文件并重新初始化。
     */
    private void resetDatabase() {
        if (!confirm("确认重建数据库？\n将删除数据库文件并重新初始化，重置后需重新登录。")) {
            return;
        }
        try {
            String message = systemService.resetDatabaseFile(dbPath());
            UiUtil.info(this, message);
            refreshData();
        } catch (BusinessException e) {
            UiUtil.warn(this, e.getMessage());
        } catch (RuntimeException e) {
            UiUtil.error(this, "重建数据库失败：" + e.getMessage());
        }
    }

    /**
     * 打开系统参数配置对话框。
     */
    private void showConfigDialog() {
        ConfigDialog dialog = new ConfigDialog(SwingUtilities.getWindowAncestor(this), systemService);
        dialog.setVisible(true);
    }

    /**
     * 查看系统运行状态（数据库文件、各表记录数、当前登录用户等）。
     */
    private void showSystemStatus() {
        List<String> lines;
        try {
            lines = systemService.databaseStatus();
        } catch (RuntimeException e) {
            UiUtil.error(this, "读取系统状态失败：" + e.getMessage());
            return;
        }
        StringBuilder text = new StringBuilder();
        for (String line : lines) {
            text.append(line).append('\n');
        }

        JTextArea area = new JTextArea(text.toString());
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        area.setBackground(Color.WHITE);
        area.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        JScrollPane scrollPane = new JScrollPane(area);
        scrollPane.setPreferredSize(new Dimension(540, 320));

        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(owner, "系统运行状态", JDialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new BorderLayout());
        dialog.add(scrollPane, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        bottom.setBackground(Color.WHITE);
        JButton close = UiUtil.button("关闭", new Color(0x6B7280));
        close.addActionListener(e -> dialog.dispose());
        bottom.add(close);
        dialog.add(bottom, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    /**
     * 清空全部操作日志（需二次确认）。
     */
    private void clearLogs() {
        if (!confirm("确认清空全部操作日志？清空后不可恢复。")) {
            return;
        }
        try {
            int count = systemService.clearLogs();
            UiUtil.success(this, "已清空 " + count + " 条操作日志");
            refreshData();
        } catch (BusinessException e) {
            UiUtil.warn(this, e.getMessage());
        } catch (RuntimeException e) {
            UiUtil.error(this, "清空日志失败：" + e.getMessage());
        }
    }

    /**
     * 读取关键系统参数的默认说明文字（保存参数时 Config 需要携带说明）。
     *
     * @param key 参数键
     * @return 参数说明
     */
    private static String defaultDescription(String key) {
        switch (key) {
            case Config.KEY_PASS_LINE:
                return "及格线（0~100）";
            case Config.KEY_EXCELLENT_LINE:
                return "优秀线（0~100）";
            case Config.KEY_USUAL_RATIO:
                return "平时成绩占比（0~1）";
            case Config.KEY_EXAM_RATIO:
                return "期末成绩占比（0~1）";
            case Config.KEY_ATTENDANCE_WARN:
                return "缺勤预警次数（1~100）";
            case Config.KEY_PAGE_SIZE:
                return "列表默认每页条数（5~200）";
            default:
                return "系统参数";
        }
    }

    /**
     * 系统参数配置对话框（本面板内部使用）。
     *
     * <p>以表格展示 {@code sys_config} 表中的全部参数，"参数值"列可直接编辑，
     * 点击【保存】逐行写回数据库；点击【恢复默认】把关键参数恢复为课程设计的默认取值。</p>
     *
     * @author 课程设计第 2 小组 - 成员 D
     * @version 1.0.0
     */
    private static class ConfigDialog extends JDialog {

        private static final long serialVersionUID = 1L;

        /** 参数配置业务服务 */
        private final SystemService service;
        /** 表格模型：仅"参数值"列可编辑 */
        private final DefaultTableModel model;
        /** 参数表格 */
        private final JTable configTable;

        /**
         * 构造参数配置对话框。
         *
         * @param owner   父窗口
         * @param service 系统维护业务服务
         */
        ConfigDialog(Window owner, SystemService service) {
            super(owner, "系统参数配置", JDialog.ModalityType.APPLICATION_MODAL);
            this.service = service;
            this.model = new DefaultTableModel(new Object[]{"参数键", "参数值", "说明"}, 0) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean isCellEditable(int row, int column) {
                    return column == 1;
                }
            };
            this.configTable = new JTable(model);
            buildUi();
            loadConfigs();
        }

        /**
         * 装配对话框界面。
         */
        private void buildUi() {
            UiUtil.styleTable(configTable);
            configTable.setFillsViewportHeight(true);
            configTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
            configTable.getColumnModel().getColumn(0).setPreferredWidth(180);
            configTable.getColumnModel().getColumn(1).setPreferredWidth(140);
            configTable.getColumnModel().getColumn(2).setPreferredWidth(260);

            JLabel tip = new JLabel("双击“参数值”单元格即可修改，修改完成后点击【保存】写入数据库。");
            tip.setFont(UiTheme.pickChineseFont(12f));
            tip.setForeground(new Color(0x6B7280));
            tip.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

            JScrollPane scrollPane = new JScrollPane(configTable);
            scrollPane.setPreferredSize(new Dimension(580, 300));
            scrollPane.getViewport().setBackground(Color.WHITE);

            JPanel content = new JPanel(new BorderLayout());
            content.setBackground(Color.WHITE);
            content.setBorder(BorderFactory.createEmptyBorder(12, 14, 6, 14));
            content.add(tip, BorderLayout.NORTH);
            content.add(scrollPane, BorderLayout.CENTER);

            JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
            bar.setBackground(Color.WHITE);
            JButton save = UiUtil.button("保存", UiTheme.PRIMARY);
            save.addActionListener(e -> saveConfigs());
            JButton restore = UiUtil.button("恢复默认", UiTheme.WARNING);
            restore.addActionListener(e -> restoreDefaults());
            JButton close = UiUtil.button("关闭", new Color(0x6B7280));
            close.addActionListener(e -> dispose());
            bar.add(save);
            bar.add(restore);
            bar.add(close);

            setLayout(new BorderLayout());
            add(content, BorderLayout.CENTER);
            add(bar, BorderLayout.SOUTH);
            setMinimumSize(new Dimension(620, 420));
            pack();
            setLocationRelativeTo(getOwner());
        }

        /**
         * 从数据库加载全部系统参数。
         */
        private void loadConfigs() {
            model.setRowCount(0);
            for (Config config : service.findAllConfigs()) {
                model.addRow(new Object[]{config.getConfigKey(), config.getConfigValue(), config.getDescription()});
            }
        }

        /**
         * 逐行保存参数值；某一行的取值非法时提示并中止后续保存。
         */
        private void saveConfigs() {
            if (configTable.isEditing()) {
                configTable.getCellEditor().stopCellEditing();
            }
            for (int row = 0; row < model.getRowCount(); row++) {
                String key = String.valueOf(model.getValueAt(row, 0));
                Object rawValue = model.getValueAt(row, 1);
                String value = rawValue == null ? "" : String.valueOf(rawValue).trim();
                Object rawDescription = model.getValueAt(row, 2);
                String description = rawDescription == null ? "" : String.valueOf(rawDescription);
                try {
                    service.saveConfig(new Config(key, value, description));
                } catch (BusinessException e) {
                    // 例如参数越界：取值应在 0~100 之间
                    UiUtil.warn(this, e.getMessage());
                    loadConfigs();
                    return;
                } catch (RuntimeException e) {
                    UiUtil.error(this, "保存参数失败：" + e.getMessage());
                    loadConfigs();
                    return;
                }
            }
            UiUtil.success(this, "系统参数已保存");
            loadConfigs();
        }

        /**
         * 把关键参数恢复为默认取值（需二次确认）。
         */
        private void restoreDefaults() {
            if (!UiUtil.confirm(this, "确认恢复系统参数默认值？\n及格线 60、优秀线 90、"
                    + "平时成绩占比 0.3、期末成绩占比 0.7、缺勤预警 3 次、每页 15 条。")) {
                return;
            }
            Map<String, String> defaults = new LinkedHashMap<>();
            defaults.put(Config.KEY_PASS_LINE, "60");
            defaults.put(Config.KEY_EXCELLENT_LINE, "90");
            defaults.put(Config.KEY_USUAL_RATIO, "0.3");
            defaults.put(Config.KEY_EXAM_RATIO, "0.7");
            defaults.put(Config.KEY_ATTENDANCE_WARN, "3");
            defaults.put(Config.KEY_PAGE_SIZE, "15");
            try {
                for (Map.Entry<String, String> entry : defaults.entrySet()) {
                    service.saveConfig(new Config(entry.getKey(), entry.getValue(),
                            defaultDescription(entry.getKey())));
                }
                UiUtil.success(this, "已恢复默认参数");
                loadConfigs();
            } catch (BusinessException e) {
                UiUtil.warn(this, e.getMessage());
                loadConfigs();
            } catch (RuntimeException e) {
                UiUtil.error(this, "恢复默认参数失败：" + e.getMessage());
                loadConfigs();
            }
        }
    }
}
