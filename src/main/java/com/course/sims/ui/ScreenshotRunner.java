package com.course.sims.ui;

import com.course.sims.config.Session;
import com.course.sims.config.UiTheme;
import com.course.sims.entity.User;
import com.course.sims.service.AuthService;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 界面截图生成器（成品展示与文档插图使用）。
 *
 * <p>以管理员身份静默登录，依次切换到每个功能模块，把窗体渲染为 PNG 图片保存到
 * screenshots 目录。由命令行参数 {@code --screenshot} 触发，
 * 使"文档中的界面截图"与真实运行界面完全一致，避免手工截图遗漏或版本不一致。</p>
 *
 * @author 课程设计第 2 小组 - 成员 D
 * @version 1.0.0
 */
public final class ScreenshotRunner {

    private ScreenshotRunner() {
    }

    /**
     * 执行截图流程。
     */
    public static void run() {
        File dir = new File("screenshots");
        if (!dir.exists() && !dir.mkdirs()) {
            System.err.println("[错误] 无法创建截图目录：" + dir.getAbsolutePath());
            System.exit(1);
        }
        try {
            User admin = new AuthService().login("admin", "123456");
            MainFrame frame = new MainFrame(admin);
            frame.setSize(1400, 860);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            System.out.println("[截图] 主窗体已打开，当前用户：" + admin.getRealName());
            List<String> keys = frame.visibleModuleKeys();
            captureSequentially(frame, keys, dir);
        } catch (Exception e) {
            System.err.println("[错误] 截图失败：" + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * 依次切换模块并截图。
     *
     * @param frame 主窗体
     * @param keys  模块 Key 列表
     * @param dir   输出目录
     */
    private static void captureSequentially(MainFrame frame, List<String> keys, File dir) {
        List<String> pending = new ArrayList<>(keys);
        javax.swing.Timer timer = new javax.swing.Timer(900, null);
        timer.addActionListener(e -> {
            if (pending.isEmpty()) {
                timer.stop();
                System.out.println("[截图] 全部完成，输出目录：" + dir.getAbsolutePath());
                Session.logout();
                System.exit(0);
            }
            String key = pending.remove(0);
            // 先切换面板并同步导航高亮，静置片刻等待布局与绘制稳定后再截图
            frame.showModule(key);
            frame.validate();
            frame.repaint();
            try {
                Thread.sleep(420);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            saveComponent(frame, new File(dir, imageName(key)), frame.getWidth(), frame.getHeight());
        });
        timer.setInitialDelay(1200);
        timer.start();
    }

    /**
     * 模块 Key 转换为截图文件名。
     *
     * @param key 模块 Key
     * @return 文件名
     */
    private static String imageName(String key) {
        switch (key) {
            case "dashboard":
                return "01-数据看板.png";
            case "student":
                return "02-学生信息管理.png";
            case "clazz":
                return "03-班级与专业管理.png";
            case "course":
                return "04-课程管理.png";
            case "scoreEntry":
                return "05-成绩录入.png";
            case "scoreQuery":
                return "06-成绩查询.png";
            case "attendance":
                return "07-考勤管理.png";
            case "stats":
                return "08-统计分析报表.png";
            case "user":
                return "09-用户与权限.png";
            case "system":
                return "10-系统维护与日志.png";
            default:
                return key + ".png";
        }
    }

    /**
     * 把组件渲染为 PNG 图片。
     *
     * @param component 组件
     * @param file      输出文件
     * @param width     宽度
     * @param height    高度
     */
    public static void saveComponent(Component component, File file, int width, int height) {
        try {
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g2 = image.createGraphics();
            g2.setColor(java.awt.Color.WHITE);
            g2.fillRect(0, 0, width, height);
            component.printAll(g2);
            g2.dispose();
            ImageIO.write(image, "png", file);
            System.out.println("[截图] 已保存 " + file.getName());
        } catch (Exception e) {
            System.err.println("[错误] 保存截图失败 " + file.getName() + "：" + e.getMessage());
        }
    }

    /**
     * 递归查找指定类型的子组件（测试辅助）。
     *
     * @param root  根容器
     * @param clazz 组件类型
     * @param <T>   类型参数
     * @return 组件列表
     */
    public static <T extends Component> List<T> findComponents(Container root, Class<T> clazz) {
        List<T> list = new ArrayList<>();
        for (Component c : root.getComponents()) {
            if (clazz.isInstance(c)) {
                list.add(clazz.cast(c));
            }
            if (c instanceof Container) {
                list.addAll(findComponents((Container) c, clazz));
            }
        }
        return list;
    }

    /**
     * 应用主题并静默登录（供外部工具调用）。
     *
     * @param username 用户名
     * @param password 密码
     * @return 主窗体
     */
    public static JFrame silentLogin(String username, String password) {
        UiTheme.apply();
        try {
            User user = new AuthService().login(username, password);
            MainFrame frame = new MainFrame(user);
            frame.setVisible(true);
            return frame;
        } catch (Exception e) {
            System.err.println("[错误] 静默登录失败：" + e.getMessage());
            SwingUtilities.invokeLater(() -> System.exit(1));
            return null;
        }
    }
}
