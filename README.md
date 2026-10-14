# 学生信息管理系统（Student Information Management System, SIMS）

> **Java 程序设计课程设计** · 团队项目（5 人，1 周）
> Java 17 + Swing（FlatLaf）+ JDBC + H2 内嵌数据库 + Apache POI + JUnit 5 + Maven

---

## 一、项目简介

本系统是一个面向高校教务场景的桌面端管理系统，实现了从**用户登录认证、学生档案、班级专业、课程、成绩（含绩点）、考勤、统计分析、数据导出、系统日志到系统维护**的完整业务闭环。程序自带内嵌数据库与演示数据，**双击 `run.bat` 即可运行，无需安装数据库服务器**。

| 项目 | 说明 |
|---|---|
| 系统名称 | 学生信息管理系统 v1.0.0 |
| 技术栈 | Java 17、Swing + FlatLaf 3.4.1、JDBC、H2 2.2.224、Apache POI 5.2.5、JUnit 5.10.2、Maven 3.9.9 |
| 代码规模 | 主源码 69 个文件 / 16,651 行；测试源码 12 个文件 / 3,407 行（合计 20,058 行） |
| 功能模块 | **10 个**（要求 ≥5） |
| 数据表 | 9 张（major、clazz、student、course、score、attendance、sys_user、sys_log、sys_config） |
| 自动化测试 | **154 个用例全部通过（0 失败 0 错误，通过率 100%）** |
| 运行环境 | Windows 10/11 + JDK 17 及以上，内存 4G 以上，磁盘 200MB |

---

## 二、快速开始

### 1. 直接运行（推荐，无需 Maven）

```
双击  run.bat
```

脚本会自动按 ①项目内便携 JDK → ②环境变量 `JAVA_HOME` → ③系统 `PATH` 的顺序寻找 JDK 并启动程序。

### 2. 命令行运行

```bash
java -Dfile.encoding=UTF-8 -jar target/sims.jar
```

### 3. 重新编译

```
双击  build.bat          # 等价于 mvn clean package
```

### 4. 执行全部测试

```
双击  test.bat           # 等价于 mvn test，并生成 HTML 测试报告
```

### 5. 用 IntelliJ IDEA 打开

1. `File → Open` 选择本项目根目录（含 `pom.xml`）；
2. 等待 Maven 自动导入依赖；
3. 将 Project SDK 设为 JDK 17 或更高；
4. 运行 `com.course.sims.App` 的 `main` 方法即可。

### 演示账号

| 账号 | 密码 | 角色 | 可见模块数 |
|---|---|---|---|
| `admin` | `123456` | 系统管理员 | 10 个（全部） |
| `teacher` | `123456` | 教师 | 7 个 |
| `zs` | `123456` | 辅导员 | 8 个 |
| `t2024001` | `2024001` | 学生 | 3 个（只读本人数据） |

> 学生账号的初始密码为本人学号。

---

## 三、功能模块

| 编号 | 模块 | 界面类 | 主要功能 |
|---|---|---|---|
| M1 | 用户登录与权限管理 | `LoginFrame` / `UserPanel` | 登录认证、MD5 密码加密、四类角色权限、菜单动态装配、账号启禁用、修改与重置密码 |
| M2 | 学生信息管理 | `StudentPanel` | 学生档案增删改查、多条件检索、格式校验、学籍状态、成绩单查看与导出 |
| M3 | 班级与专业管理 | `ClassPanel` | 专业与班级维护、代码唯一校验、班级人数与容量统计、删除前关联校验 |
| M4 | 课程管理 | `CoursePanel` | 课程维护、学分学时校验、课程类型与考核方式、课程统计 |
| M5 | 成绩管理 | `ScoreEntryPanel` / `ScoreQueryPanel` | 成绩录入与批量录入、Excel 导入、总评自动计算、绩点与 GPA、不及格名单 |
| M6 | 考勤管理 | `AttendancePanel` | 考勤登记与整班批量登记、出勤率统计、缺勤超阈值预警 |
| M7 | 统计分析报表 | `DashboardPanel` / `StatsPanel` | 数据看板、班级平均分排名、课程统计、五档分布、分数段分布、学生排名、教师工作量 |
| M8 | 数据导出与打印 | `ReportService` | 学生名册、成绩表、成绩单、考勤表、统计报表导出 Excel（多工作表） |
| M9 | 系统日志管理 | `SystemPanel` | 登录与增删改自动留痕、日志分页与检索、清空日志 |
| M10 | 系统维护与配置 | `SystemPanel` | 数据库备份、重置演示数据、重建数据库、系统参数配置、运行状态查看 |

---

## 四、目录结构

```
student-info-management-system/
├── pom.xml                     Maven 配置（依赖、编译、shade 打包）
├── run.bat                     一键启动（双击即可）
├── build.bat                   一键编译打包
├── test.bat                    一键执行测试 + 生成测试报告
├── db/                         运行后自动生成的数据库文件（sims.mv.db）
├── docs/                       课程设计文档（需求分析、概要/详细设计、测试用例与报告、答辩手册、日志）
├── screenshots/                程序真实运行截图（10 张，--screenshot 自动生成）
├── src/main/java/com/course/sims/
│   ├── App.java                程序入口（含 --screenshot 截图模式）
│   ├── config/                 UiTheme（外观主题）、DatabaseInitializer（建库与演示数据）、Session（登录会话）
│   ├── entity/                 9 个实体类，与 9 张表一一对应
│   ├── dao/                    DaoFactory + 9 个 DAO（JDBC 数据访问）
│   ├── service/                8 个业务服务 + BusinessException + 2 个 DTO
│   ├── ui/                     LoginFrame、MainFrame、SideBar、ScreenshotRunner
│   │   ├── common/             BasePanel、BaseDialog、UiUtil、ChartPanel、Refreshable
│   │   ├── panel/              10 个功能模块面板
│   │   └── dialog/             9 个编辑对话框
│   └── util/                   Db、MD5Util、PageResult、QueryBuilder、RowMapper、ExcelUtil、MultiSheetExcel
├── src/main/resources/db/
│   ├── schema.sql              建表脚本（9 张表、索引、约束、总评计算列）
│   └── data.sql                演示数据（用户 4、专业 5、班级 10、学生 40、课程 10、成绩 75、参数 8）
└── src/test/java/com/course/sims/
    ├── BaseTest.java           测试基类（内存库 + 每用例前重置演示数据）
    ├── FunctionalTest.java     端到端集成测试（7 个场景）
    ├── entity/ScoreTest.java   成绩算法测试（16 个）
    ├── service/                8 个业务服务测试（115 个）
    └── util/UtilTest.java      公共工具测试（13 个）
```

---

## 五、系统架构

采用经典**三层架构 + 公共工具层**：

```
表示层  ui / ui.common / ui.panel / ui.dialog
   ↓  只调用 Service，不直接访问 DAO
业务逻辑层  service（业务规则、校验、事务语义、日志）
   ↓  只调用 DAO，不拼接界面文本
数据访问层  dao + entity（参数化 SQL、行映射、分页与聚合）
   ↓
公共工具层  util（Db、QueryBuilder、PageResult、ExcelUtil…）
   ↓
H2 内嵌数据库（文件模式 db/sims.mv.db）
```

**关键横向机制**

- **分页**：所有列表统一返回 `PageResult<T>`（rows / total / pageNo / pageSize / totalPages / hasPrev / hasNext），默认每页 15 条。
- **动态条件**：`QueryBuilder` 链式拼接 `WHERE` 条件，全部使用 `?` 占位符 + 参数列表，从根本上杜绝 SQL 注入。
- **统计下推**：平均分、及格率、排名、分布等全部用 SQL 聚合在数据库端完成，不把全表读进内存。
- **异常分级**：`BusinessException`（可被用户理解并修正，界面弹提示）与 `RuntimeException`（系统级，记录并提示失败原因）分开处理，异常不会穿透到事件分发线程。
- **权限双重校验**：`Session` 保存登录用户，`SideBar` 按角色装配菜单（界面级），Service 方法入口再校验角色（业务级）。
- **幂等初始化**：`Db.initDatabase()` 执行 `schema.sql` 建表 + 表空时灌 `data.sql` + `restartIdentities()` 校正自增序列；考勤数据由 `DatabaseInitializer.generateAttendance()` 按固定随机种子批量生成，可重复执行。

---

## 六、数据库设计

| 表名 | 说明 | 关键约束 |
|---|---|---|
| `major` | 专业 | `major_code` 唯一 |
| `clazz` | 班级 | `class_code` 唯一，外键 → `major` |
| `student` | 学生 | `sno` 唯一，外键 → `clazz` |
| `course` | 课程 | `course_code` 唯一 |
| `score` | 成绩 | `(student_id, course_id)` 唯一；`total_score` 为计算列 |
| `attendance` | 考勤 | `(student_id, course_id, att_date)` 唯一 |
| `sys_user` | 用户 | `username` 唯一，密码 MD5 密文 |
| `sys_log` | 操作日志 | `log_time` 建索引 |
| `sys_config` | 系统参数 | `config_key` 唯一 |

**总评成绩计算规则**（Java 与数据库计算列双实现并保持一致）：

```
有补考成绩：总评 = 补考成绩
否则      ：总评 = 平时成绩 × 30% + 期末成绩 × 70%   （四舍五入保留 1 位小数）
绩点（四等五级制）：≥90 → 4.0；80~89 → 3.0；70~79 → 2.0；60~69 → 1.0；<60 → 0
GPA = Σ(课程绩点 × 学分) / Σ学分
```

**演示数据规模**：专业 5、班级 10、学生 40、课程 10、成绩 75 条、考勤 900 条、用户 4 个、参数 8 项。

---

## 七、测试情况

执行命令 `mvn test`，结果如下：

| 测试类 | 用例数 | 耗时(s) | 覆盖内容 |
|---|---|---|---|
| `UtilTest` | 13 | 0.010 | MD5、分页计算、动态条件构造器 |
| `ScoreTest` | 16 | 0.102 | 总评 / 绩点 / 等级 / 及格判定算法 |
| `AuthServiceTest` | 15 | 0.339 | M1 登录认证与权限 |
| `StudentServiceTest` | 15 | 0.282 | M2 学生管理 |
| `TeachingServiceTest` | 15 | 0.284 | M3/M4 班级专业与课程 |
| `ScoreServiceTest` | 20 | 0.513 | M5 成绩与绩点 |
| `AttendanceServiceTest` | 16 | 0.481 | M6 考勤 |
| `StatsServiceTest` | 12 | 0.232 | M7 统计分析 |
| `ReportServiceTest` | 10 | 1.070 | M8 数据导出 |
| `SystemServiceTest` | 13 | 0.340 | M9/M10 日志与系统维护 |
| `FunctionalTest` | 9 | 4.150 | 端到端集成（业务闭环、权限矩阵、界面装配冒烟、性能、一致性） |
| **合计** | **154** | **≈8.3** | **Failures: 0, Errors: 0, Skipped: 0** |

测试采用 H2 内存库（`jdbc:h2:mem:sims_test;DB_CLOSE_DELAY=-1`），并在每个用例执行前把数据库恢复为初始演示数据，因此**用例之间完全独立、可任意顺序重复执行**，不会污染真实运行的 `db/sims.mv.db`。

详细测试用例与结果见：`docs/04-系统功能测试用例表.xlsx`、`docs/05-系统测试报告.docx`。

---

## 八、课程设计文档清单

| 文档 | 文件 | 内容 |
|---|---|---|
| 文档一 | `docs/01-需求分析与设计步骤说明书.docx` | 需求分析、角色权限矩阵、10 个模块功能需求、5 个典型用例、非功能性需求、**12 个设计步骤**、5 人分工与 1 周进度计划、需求确认清单 |
| 文档二 | `docs/02-概要设计说明书.docx` | 总体设计、技术选型、三层架构、模块划分 M1~M10、数据库设计（9 张表数据字典）、接口设计、出错处理、安全设计、性能与容量 |
| 文档三 | `docs/03-详细设计说明书.docx` | 程序流程、类设计（实体/DAO/Service/界面/工具）、关键算法实现、界面详细设计（含真实运行截图）、数据库详细设计、错误码清单、测试设计、部署运行 |
| 文档四 | `docs/04-系统功能测试用例表.xlsx` | 143 条功能测试用例（编号、前置条件、步骤、数据、预期结果、实际结果）、用例统计、缺陷记录、测试环境说明、JUnit 对照表 |
| 文档五 | `docs/05-系统测试报告.docx` | 测试环境与策略、按模块执行结果、12 条真实缺陷记录与分析、测试结论与质量评价、全部 154 个用例清单、界面截图抽查 |
| 文档六 | `docs/06-答辩准备手册.docx` | 答辩问答 30 组 + 25 个追问、五名成员各自的答辩要点、现场演示脚本、答辩前检查清单 |
| 日志 | `docs/07-课程设计日志-第1天.docx`、`docs/日志-第2天.txt` | 逐日开发日志（含团队分工表与签名栏） |

---

## 九、团队分工（5 人）

| 成员 | 角色 | 负责模块 | 主要交付物 |
|---|---|---|---|
| 姜望舒 | 项目经理 / 系统架构师 | M1 用户登录与权限管理、公共工具层 | 架构设计、`App`/`Session`/`Db`/`MD5Util`/`PageResult`/`QueryBuilder`、`AuthService`、`UserDao` |
| 佟尚轩 | 数据库与 DAO 层开发 | M2 学生信息管理、M3 班级与专业管理 | `schema.sql`/`data.sql`、实体层 9 个类、`StudentDao`/`ClazzDao`/`MajorDao`、`StudentService`/`TeachingService`、`StudentPanel` |
| 陈孝鑫 | 业务逻辑层开发 | M4 课程管理、M5 成绩管理、M6 考勤管理 | `CourseDao`/`ScoreDao`/`AttendanceDao`、`ScoreService`/`AttendanceService`、`ScoreEntryPanel`/`ScoreQueryPanel`/`AttendancePanel`、成绩算法 |
| 王永涛 | 界面开发（主） | M9 日志管理、M10 系统维护、界面框架 | `MainFrame`/`SideBar`/`LoginFrame`/`UiTheme`/`BasePanel`/`BaseDialog`/`UiUtil`、`SystemPanel`/`UserPanel` |
| 刘壮 | 界面开发与测试 | M7 统计分析、M8 数据导出 | `StatsService`/`ReportService`、`ChartPanel`/`ExcelUtil`/`MultiSheetExcel`、`DashboardPanel`/`StatsPanel`、154 个自动化测试用例、全部文档 |

---

## 十、常见问题

**Q1：双击 `run.bat` 提示"未检测到可用的 JDK"？**
安装 JDK 17 或更高版本，并设置系统环境变量 `JAVA_HOME`；或把 JDK 解压到项目上一级目录的 `.tools\jdk` 下（脚本会自动识别）。

**Q2：数据库文件在哪里？如何恢复演示数据？**
数据库文件为 `db/sims.mv.db`，首次启动自动创建。恢复数据有两种方式：
① 用 `admin` 登录 → 「系统维护与日志」→「重置演示数据」（无需重启）；
② 直接删除 `db` 目录后重新启动程序。

**Q3：界面中文显示为方框？**
程序会自动在「微软雅黑 / 等线 / 宋体」中挑选可用中文字体。若仍异常，请确认系统已安装中文字体。

**Q4：成绩总评为什么与手算差 0.1 分？**
总评按"四舍五入保留 1 位小数"计算，与数据库计算列公式一致；测试用例 `TC-D-04` 专门校验了 Java 端与数据库端结果的一致性（容差 0.05）。

**Q5：如何批量录入一个班的成绩？**
「成绩录入」→ 选择课程与班级 → 「加载学生名单」→ 直接在表格中填写平时/期末/补考成绩（总评与等级自动重算）→ 「保存全部」；也可用「导入 Excel」按模板批量导入。

**Q6：`mvn test` 失败提示找不到依赖？**
首次执行需联网下载依赖，请确认网络可访问 Maven 中央仓库；离线环境可先在有网机器执行一次 `mvn dependency:go-offline` 并拷贝本地仓库。

---

## 十一、许可与用途

本项目为高校《Java 程序设计》课程设计作品，仅供学习与教学使用。
