# 作业自动批改系统

这是一个面向毕业设计的教学管理系统，支持课程管理、作业发布、代码提交与 Java 自动评测。

当前项目是一个单体 Spring Boot 应用，包含：

- 后端 REST 接口
- 内置静态前端页面
- 基于编译、运行、比对的 Java 判题模块
- 教师、学生、管理员三类角色的基础流程

## 技术栈

- Java 17
- Spring Boot 3.3
- Spring Web
- Spring Data JPA
- MySQL / H2
- Maven

## 快速启动

### 方式一：使用 Demo 配置启动

这是本项目最推荐的本地启动方式，使用 H2 内存数据库，不依赖 MySQL。

```powershell
mvn spring-boot:run "-Dspring-boot.run.profiles=demo"
```

启动后访问：

`http://localhost:8080/#/login`

H2 控制台地址：

`http://localhost:8080/h2-console`

### 方式二：使用默认配置连接 MySQL

默认配置会连接 MySQL。启动前请先设置环境变量：

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/graduation_assignment_autograding_isolated?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="你的数据库密码"
mvn spring-boot:run
```

### 方式三：运行打包后的 Jar

```powershell
mvn clean package
java -jar target\assignment-auto-grading-0.0.1-SNAPSHOT.jar --spring.profiles.active=demo
```

## 演示账号

`demo` 配置启动后会自动写入以下账号：

- `admin1 / 123456`
- `teacher1 / 123456`
- `student1 / 123456`

默认配置连接 MySQL 时不会自动写入演示账号；如需演示数据，请显式使用 `demo` profile。

## 演示数据

`demo` 配置启动后会自动准备一套可直接答辩演示的数据：

- 课程：`CS101 · 程序设计基础`
- 教师：`teacher1`
- 学生：`student1`，已加入 `CS101`
- 作业：`演示：两数求和`，状态为 `PUBLISHED`
- 测试用例：`1 2 -> 3`、`6 9 -> 15`

## 答辩演示脚本

建议按下面顺序演示，时间短、链路完整，也最不容易现场翻车：

1. 使用 `admin1 / 123456` 登录，展示管理员端用户、课程、作业和操作日志列表，说明系统具备三类角色和基础审计能力。
2. 退出后使用 `teacher1 / 123456` 登录，进入“课程与选课”，展示 `CS101 · 程序设计基础` 与已选学生 `student1`。
3. 切换到“作业管理”或“创建作业”，展示 `演示：两数求和` 的作业信息、发布时间、所属课程和测试用例配置。
4. 退出后使用 `student1 / 123456` 登录，进入“课程选择”，确认学生端能看到 `CS101`，再进入“已发布作业”。
5. 选择 `演示：两数求和`，进入“代码提交”，使用页面默认 Java 代码提交，等待评测完成。
6. 在学生端查看“最近提交”和“评测详情”，展示 `ACCEPTED`、`100` 分和测试用例通过情况。
7. 回到教师端“查看提交”，选择该作业并加载提交列表，展示教师可以查看学生提交、源码、评测结果和统计信息。
8. 如答辩老师追问异常场景，可以把学生端代码中的 `a + b` 改成 `a - b` 或删除分号，分别演示错误答案或编译错误。

## 自动化验证

运行测试：

```powershell
mvn clean test
```

当前已覆盖的关键回归点包括：

- 登录与鉴权接口
- `401` 与 `403` 状态码返回
- 教师创建作业流程
- 学生提交代码流程
- 判题通过与编译错误结果
- 学生端作业数据脱敏
- 教师查看提交范围隔离
- `demo` 数据仅在 `demo` profile 生效

## 运行说明

- 默认关闭自动打开浏览器，配置项为 `app.startup.open-browser=false`
- Servlet 响应编码已固定为 UTF-8，避免中文乱码
- 默认关闭 SQL 语句打印，方便调试和演示时保持终端干净
- 当前判题器适合课程演示和毕业设计交付，不适合直接用于生产环境沙箱

## 主要接口分组

- `/api/auth`
- `/api/users`
- `/api/courses`
- `/api/assignments`
- `/api/submissions`
- `/api/import`
- `/api/audit-logs`

## 项目结构

- `src/main/java/com/graduation/autograding/controller`：控制器层
- `src/main/java/com/graduation/autograding/service`：业务逻辑层
- `src/main/java/com/graduation/autograding/repository`：数据访问层
- `src/main/java/com/graduation/autograding/domain`：实体与枚举
- `src/main/java/com/graduation/autograding/judge`：Java 判题模块
- `src/main/resources/static`：内置前端页面
- `src/test/java`：自动化测试代码

## 相关文档

- `PROJECT_GUIDE.md`：项目分阶段完善路线
- `TEST_CASES.md`：测试用例与验证清单
