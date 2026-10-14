package com.course.sims;

import com.course.sims.config.DatabaseInitializer;
import com.course.sims.config.UiTheme;
import com.course.sims.ui.LoginFrame;
import com.course.sims.ui.ScreenshotRunner;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * 学生信息管理系统程序入口。
 *
 * <p>启动流程：① 应用外观主题与中文字体 → ② 初始化数据库（建表 + 首次灌入演示数据）
 * → ③ 打开登录窗口 → ④ 登录成功后进入主窗体。</p>
 *
 * <p>命令行参数：</p>
 * <ul>
 *   <li>{@code --screenshot}：自动生成各模块界面截图（成品展示与文档插图用），
 *       截图输出到 screenshots 目录，完成后自动退出；</li>
 *   <li>{@code --help} / {@code -h}：仅打印使用说明与参数列表后退出，不启动界面；</li>
 *   <li>{@code --version}：仅打印版本与运行环境后退出；</li>
 *   <li>{@code -Dsims.db.path=...}：指定数据库文件位置。</li>
 * </ul>
 *
 * @author 课程设计第 2 小组 - 成员 A
 * @version 1.0.0
 */
public final class App {

    /** 系统名称 */
    public static final String APP_NAME = "学生信息管理系统";
    /** 系统版本 */
    public static final String APP_VERSION = "1.0.0";

    private App() {
    }

    /**
     * 程序主方法。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        System.setProperty("file.encoding", "UTF-8");
        if (hasArg(args, "--help") || hasArg(args, "-h")) {
            printUsage();
            return;
        }
        try {
            UiTheme.apply();
        } catch (Exception e) {
            System.err.println("[警告] 外观主题加载失败，使用系统默认外观：" + e.getMessage());
        }
        printBanner();
        if (hasArg(args, "--version")) {
            return;
        }
        try {
            DatabaseInitializer.init();
            System.out.println("[启动] 数据库初始化完成");
        } catch (Exception e) {
            System.err.println("[错误] 数据库初始化失败：" + e.getMessage());
            SwingUtilities.invokeLater(() -> javax.swing.JOptionPane.showMessageDialog(null,
                    "数据库初始化失败：\n" + e.getMessage(), "启动失败", javax.swing.JOptionPane.ERROR_MESSAGE));
            System.exit(1);
        }

        boolean screenshotMode = args.length > 0 && "--screenshot".equalsIgnoreCase(args[0]);
        SwingUtilities.invokeLater(() -> {
            if (screenshotMode) {
                ScreenshotRunner.run();
            } else {
                new LoginFrame().setVisible(true);
            }
        });
    }

    /**
     * 判断命令行参数中是否包含指定选项。
     *
     * @param args 参数数组
     * @param flag 目标选项
     * @return 包含返回 true
     */
    private static boolean hasArg(String[] args, String flag) {
        if (args == null) {
            return false;
        }
        for (String arg : args) {
            if (flag.equalsIgnoreCase(arg)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 打印使用说明（--help 参数）。
     */
    private static void printUsage() {
        System.out.println(APP_NAME + " v" + APP_VERSION);
        System.out.println();
        System.out.println("用法：java -jar sims.jar [选项]");
        System.out.println();
        System.out.println("选项：");
        System.out.println("  （无参数）        启动图形界面");
        System.out.println("  --screenshot     自动生成各模块界面截图到 screenshots 目录后退出");
        System.out.println("  --version        仅打印版本与运行环境后退出");
        System.out.println("  --help, -h       打印本使用说明后退出");
        System.out.println();
        System.out.println("JVM 参数：");
        System.out.println("  -Dsims.db.path=路径   指定数据库位置，默认 db/sims（文件模式）");
        System.out.println("                        例如 -Dsims.db.path=jdbc:h2:mem:demo（内存模式）");
        System.out.println();
        System.out.println("演示账号：admin / teacher / zs，密码 123456；学生账号 t2024001，密码为学号");
        System.out.println("推荐用法：直接双击 run.bat 启动");
    }

    /**
     * 输出启动横幅（便于课程设计答辩演示与日志留痕）。
     */
    private static void printBanner() {
        System.out.println("======================================================");
        System.out.println("           " + APP_NAME + "  v" + APP_VERSION);
        System.out.println("    Student Information Management System");
        System.out.println("    Java 17 + Swing + H2 + JDBC | 课程设计 4-5 人团队项目");
        System.out.println("------------------------------------------------------");
        System.out.println("  演示账号：admin / teacher / zs      密码：123456");
        System.out.println("  学生账号：t2024001                   密码：2024001");
        System.out.println("  运行环境：JDK " + System.getProperty("java.version")
                + "  |  外观：" + UIManager.getLookAndFeel().getName());
        System.out.println("======================================================");
    }
}
