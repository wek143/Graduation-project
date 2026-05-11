# 作业自动评测与管理系统测试用例

## 1. 文档说明

本文档用于测试当前项目的前后端功能，覆盖以下范围：

- 启动页、登录页、注册页、教师端、学生端前端流程
- 用户认证、会话、权限控制
- 作业创建、修改、删除、测试用例管理
- 学生提交、自动评测、评测结果查询
- 统计查询、历史记录、异常输入与边界情况

说明：

- 本文档基于当前代码实现编写，不是理想化需求文档。
- 当前代码已对未登录和越权场景分别返回 `401` 与 `403`。
- 学生端查询作业时不会返回测试用例答案，教师端与管理员端保留完整测试用例信息。
- 演示账号与演示课程数据仅在 `demo` profile 下自动写入。

## 2. 测试环境

- 操作系统：Windows 10/11
- JDK：17+
- Maven：3.9+
- 数据库：MySQL，或 `demo` 配置下的 H2
- 服务端口：`8080`
- 浏览器：Chrome / Edge 最新版

## 3. 默认测试数据

使用 `demo` profile 启动后，系统会自动创建以下账号：

| 账号 | 密码 | 角色 |
| --- | --- | --- |
| `admin1` | `123456` | 管理员 |
| `teacher1` | `123456` | 教师 |
| `student1` | `123456` | 学生 |

建议另外注册以下测试账号，便于权限隔离测试：

| 账号 | 密码 | 角色 | 用途 |
| --- | --- | --- | --- |
| `teacher2` | `123456` | 教师 | 测试教师间数据隔离 |
| `student2` | `123456` | 学生 | 测试学生间数据隔离 |

## 4. 建议测试作业数据

建议由 `teacher1` 创建以下作业：

### A1. 两数求和作业

- 标题：`A1-两数求和`
- 描述：输入两个整数，输出它们的和
- 状态：`PUBLISHED`
- 截止时间：当前时间后 1 天
- 测试用例：

| 序号 | 输入 | 期望输出 |
| --- | --- | --- |
| 1 | `1 2` | `3` |
| 2 | `6 9` | `15` |

### A2. 草稿作业

- 标题：`A2-草稿作业`
- 状态：`DRAFT`
- 截止时间：当前时间后 1 天

### A3. 已关闭作业

- 标题：`A3-关闭作业`
- 状态：`CLOSED`
- 截止时间：当前时间后 1 天

### A4. 无测试用例作业

- 标题：`A4-无用例作业`
- 状态：`PUBLISHED`
- 截止时间：当前时间后 1 天
- 测试用例：不添加

### A5. 临近截止作业

- 标题：`A5-临近截止作业`
- 状态：`PUBLISHED`
- 截止时间：当前时间后 1 分钟
- 用途：等待截止后验证超期禁止提交

## 5. 提交代码样例

以下代码可直接用于学生端测试。

### 5.1 正确通过代码

```java
public class Main {
    public static void main(String[] args) {
        java.util.Scanner scanner = new java.util.Scanner(System.in);
        int a = scanner.nextInt();
        int b = scanner.nextInt();
        System.out.println(a + b);
    }
}
```

### 5.2 错误答案代码

```java
public class Main {
    public static void main(String[] args) {
        java.util.Scanner scanner = new java.util.Scanner(System.in);
        int a = scanner.nextInt();
        int b = scanner.nextInt();
        System.out.println(a - b);
    }
}
```

### 5.3 部分通过代码

```java
public class Main {
    public static void main(String[] args) {
        java.util.Scanner scanner = new java.util.Scanner(System.in);
        int a = scanner.nextInt();
        int b = scanner.nextInt();
        if (a == 1 && b == 2) {
            System.out.println(3);
        } else {
            System.out.println(0);
        }
    }
}
```

### 5.4 编译错误代码

```java
public class Main {
    public static void main(String[] args) {
        System.out.println("hello")
    }
}
```

### 5.5 运行错误代码

```java
public class Main {
    public static void main(String[] args) {
        String value = null;
        System.out.println(value.length());
    }
}
```

### 5.6 超时代码

```java
public class Main {
    public static void main(String[] args) {
        while (true) {
        }
    }
}
```

## 6. 前端页面测试用例

| 编号 | 模块 | 测试项 | 前置条件 | 测试步骤 | 预期结果 |
| --- | --- | --- | --- | --- | --- |
| UI-01 | 启动页 | 首页正常展示 | 服务已启动 | 访问 `/` | 显示欢迎页、系统名称、登录入口、注册入口、示例账号 |
| UI-02 | 启动页 | 首页跳转登录 | 服务已启动 | 点击“进入登录” | 跳转到登录页 |
| UI-03 | 启动页 | 首页跳转注册 | 服务已启动 | 点击“进入注册” | 跳转到注册页 |
| UI-04 | 启动页 | 示范账号填充-教师 | 服务已启动 | 点击“填入教师账号” | 自动跳转登录页，登录表单填入 `teacher1/123456` |
| UI-05 | 启动页 | 示范账号填充-学生 | 服务已启动 | 点击“填入学生账号” | 自动跳转登录页，登录表单填入 `student1/123456` |
| UI-06 | 登录注册页 | 登录页签切换 | 进入认证页 | 点击“注册”，再点击“登录” | 页面在登录表单和注册表单之间切换 |
| UI-07 | 登录跳转 | 教师登录自动进入教师端 | `teacher1` 存在 | 登录 `teacher1` | 自动进入教师端页面，显示教师工作台 |
| UI-08 | 登录跳转 | 学生登录自动进入学生端 | `student1` 存在 | 登录 `student1` | 自动进入学生端页面，显示学生工作台 |
| UI-09 | 注销跳转 | 退出后返回首页 | 已登录 | 点击“退出登录” | 返回启动页，登录态清除 |
| UI-10 | 登录恢复 | 浏览器刷新后恢复会话 | 已登录且 token 未过期 | 刷新浏览器 | 保持登录状态，并自动回到对应角色工作台 |

## 7. 认证与用户测试用例

| 编号 | 模块 | 测试项 | 前置条件 | 测试步骤 | 预期结果 |
| --- | --- | --- | --- | --- | --- |
| AUTH-01 | 注册 | 学生注册成功 | `student2` 不存在 | 调用 `/api/auth/register`，传 `student2/123456/STUDENT` | 返回 token、用户名、角色、过期时间 |
| AUTH-02 | 注册 | 教师注册成功 | `teacher2` 不存在 | 调用 `/api/auth/register`，传 `teacher2/123456/TEACHER` | 返回 token、用户名、角色、过期时间 |
| AUTH-03 | 注册 | 重复用户名注册 | `student1` 已存在 | 使用 `student1` 再次注册 | 返回失败，消息 `Username already exists.` |
| AUTH-04 | 注册 | 角色非法 | 无 | 注册时 role 传 `ADMIN` | 返回失败，消息 `Role must be TEACHER or STUDENT.` |
| AUTH-05 | 注册 | 用户名为空 | 无 | 注册时 `username=""` | 返回 `400`，消息 `Validation failed` |
| AUTH-06 | 注册 | 密码为空 | 无 | 注册时 `password=""` | 返回 `400`，消息 `Validation failed` |
| AUTH-07 | 登录 | 教师登录成功 | `teacher1` 已存在 | 调用 `/api/auth/login` 使用正确密码 | 返回 token、角色 `TEACHER` |
| AUTH-08 | 登录 | 学生登录成功 | `student1` 已存在 | 调用 `/api/auth/login` 使用正确密码 | 返回 token、角色 `STUDENT` |
| AUTH-09 | 登录 | 用户不存在 | 无 | 使用不存在用户名登录 | 返回失败，消息 `User not found.` |
| AUTH-10 | 登录 | 密码错误 | `teacher1` 已存在 | 使用错误密码登录 | 返回失败，消息 `Incorrect password.` |
| AUTH-11 | 当前用户 | 获取当前用户信息 | 已登录 | 调用 `/api/auth/me` | 返回当前登录用户信息 |
| AUTH-12 | 注销 | 登出成功 | 已登录 | 调用 `/api/auth/logout` | 返回 `204` 或空响应 |
| AUTH-13 | 会话 | 登出后 token 失效 | 已登录并已登出 | 用原 token 调用受保护接口 | 返回 `401`，提示登录已过期 |
| AUTH-14 | 鉴权 | 不带 token 访问受保护接口 | 无 | 请求 `/api/users/overview` 不带 `Authorization` | 返回 `401`，提示缺少登录凭证 |

## 8. 教师端与作业管理测试用例

| 编号 | 模块 | 测试项 | 前置条件 | 测试步骤 | 预期结果 |
| --- | --- | --- | --- | --- | --- |
| TCH-01 | 统计概览 | 查看系统统计 | 使用 `teacher1` 登录 | 调用 `/api/users/overview` | 返回教师数、学生数、作业数、已发布作业数、提交总数 |
| TCH-02 | 用户管理 | 查看全部用户 | 使用 `teacher1` 登录 | 调用 `/api/users` | 返回用户列表 |
| TCH-03 | 用户管理 | 按角色查看学生 | 使用 `teacher1` 登录 | 调用 `/api/users/role/STUDENT` | 仅返回学生列表 |
| TCH-04 | 作业创建 | 创建已发布作业成功 | 使用 `teacher1` 登录 | 创建 A1 | 返回作业详情，状态为 `PUBLISHED` |
| TCH-05 | 作业创建 | 创建草稿作业成功 | 使用 `teacher1` 登录 | 创建 A2 | 返回状态 `DRAFT` |
| TCH-06 | 作业创建 | 创建关闭作业成功 | 使用 `teacher1` 登录 | 创建 A3 | 返回状态 `CLOSED` |
| TCH-07 | 作业创建 | 截止时间早于当前时间 | 使用 `teacher1` 登录 | 创建作业时 deadline 传过去时间 | 返回失败，消息 `Deadline must be later than the current time.` |
| TCH-08 | 作业创建 | 标题为空 | 使用 `teacher1` 登录 | 创建作业时 title 为空 | 返回 `400`，消息 `Validation failed` |
| TCH-09 | 作业创建 | 描述为空 | 使用 `teacher1` 登录 | 创建作业时 description 为空 | 返回 `400`，消息 `Validation failed` |
| TCH-10 | 作业创建 | teacherId 冒用他人账号 | 已存在 `teacher2` | `teacher1` 创建作业时传 `teacherId=teacher2.id` | 失败，消息 `Teachers can only create assignments under their own account.` |
| TCH-11 | 作业查询 | 查看所有作业 | 使用 `teacher1` 登录 | 调用 `/api/assignments` | 返回全部作业，包括 DRAFT/PUBLISHED/CLOSED |
| TCH-12 | 作业查询 | 查看单个作业详情 | A1 已创建 | 调用 `/api/assignments/{id}` | 返回对应作业完整信息 |
| TCH-13 | 作业修改 | 修改标题和描述 | A1 已创建 | 调用 `PUT /api/assignments/{id}` | 修改成功并返回新内容 |
| TCH-14 | 作业修改 | 状态从 PUBLISHED 切到 CLOSED | A1 已创建 | 更新 status 为 `CLOSED` | 返回状态 `CLOSED` |
| TCH-15 | 作业修改 | 非法状态值 | A1 已创建 | 更新 status 为 `OPEN` | 返回失败，消息 `Status must be DRAFT, PUBLISHED, or CLOSED.` |
| TCH-16 | 测试用例 | 新增测试用例 | A1 已创建 | 调用 `POST /api/assignments/{id}/test-cases` | 作业中新增一条测试用例 |
| TCH-17 | 测试用例 | 修改测试用例 | A1 已存在测试用例 | 调用 `PUT /api/assignments/{assignmentId}/test-cases/{testCaseId}` | 测试用例内容更新成功 |
| TCH-18 | 测试用例 | 删除测试用例 | A1 已存在测试用例 | 调用 `DELETE /api/assignments/{assignmentId}/test-cases/{testCaseId}` | 测试用例被删除 |
| TCH-19 | 作业删除 | 无提交记录的作业删除成功 | 新建一个未提交作业 | 调用 `DELETE /api/assignments/{id}` | 删除成功 |
| TCH-20 | 作业删除 | 有提交记录的作业禁止删除 | A1 已有学生提交 | 调用 `DELETE /api/assignments/{id}` | 返回失败，消息 `Assignments with submissions cannot be deleted.` |
| TCH-21 | 统计 | 查看作业统计概览 | 至少存在一个作业 | 调用 `/api/assignments/statistics/overview` | 返回每个作业的提交次数、参与学生数、平均分 |

## 9. 学生端与作业浏览测试用例

| 编号 | 模块 | 测试项 | 前置条件 | 测试步骤 | 预期结果 |
| --- | --- | --- | --- | --- | --- |
| STU-01 | 作业列表 | 学生只看到已发布作业 | 已存在 A1、A2、A3 | 使用 `student1` 调用 `/api/assignments` | 只返回 `PUBLISHED` 作业 |
| STU-02 | 作业列表 | 已登录学生查看已发布作业 | 已存在 A1、A2、A3 | 学生登录后调用 `/api/assignments/published` | 只返回 `PUBLISHED` 作业 |
| STU-03 | 作业详情 | 学生查看已发布作业详情 | A1 为 `PUBLISHED` | 调用 `/api/assignments/{A1}` | 成功返回 A1，且 `testCases` 不包含答案数据 |
| STU-04 | 作业详情 | 学生查看草稿作业 | A2 为 `DRAFT` | 调用 `/api/assignments/{A2}` | 返回 `403` |
| STU-05 | 作业详情 | 学生查看已关闭作业 | A3 为 `CLOSED` | 调用 `/api/assignments/{A3}` | 返回 `403` |

## 10. 提交与自动评测测试用例

| 编号 | 模块 | 测试项 | 前置条件 | 测试步骤 | 预期结果 |
| --- | --- | --- | --- | --- | --- |
| JDG-01 | 提交 | 正确代码全部通过 | A1 已发布，`student1` 登录 | 提交“正确通过代码” | 返回 `ACCEPTED`，分数 `100`，两个用例都通过 |
| JDG-02 | 提交 | 错误答案全部失败 | A1 已发布，`student1` 登录 | 提交“错误答案代码” | 返回 `FAILED`，分数 `0` |
| JDG-03 | 提交 | 部分通过 | A1 已发布，`student1` 登录 | 提交“部分通过代码” | 返回 `PARTIAL_ACCEPTED`，分数 `50` |
| JDG-04 | 提交 | 编译错误 | A1 已发布，`student1` 登录 | 提交“编译错误代码” | 返回 `COMPILE_ERROR`，分数 `0`，compileMessage 有具体报错行 |
| JDG-05 | 提交 | 运行错误 | A1 已发布，`student1` 登录 | 提交“运行错误代码” | 返回 `RUNTIME_ERROR`，分数 `0`，runtimeMessage 含异常信息 |
| JDG-06 | 提交 | 超时 | A1 已发布，`student1` 登录 | 提交“超时代码” | 返回 `TIME_LIMIT_EXCEEDED`，分数 `0`，runtimeMessage 含超时信息 |
| JDG-07 | 提交 | 作业无测试用例 | A4 已发布，`student1` 登录 | 提交任意可编译代码 | 返回 `FAILED`，分数 `0`，runtimeMessage 提示未配置测试用例 |
| JDG-08 | 提交 | 草稿作业禁止提交 | A2 为 `DRAFT` | `student1` 提交到 A2 | 返回失败，消息 `Only published assignments can accept submissions.` |
| JDG-09 | 提交 | 关闭作业禁止提交 | A3 为 `CLOSED` | `student1` 提交到 A3 | 返回失败，消息 `Only published assignments can accept submissions.` |
| JDG-10 | 提交 | 过期作业禁止提交 | A5 已超过截止时间 | `student1` 提交到 A5 | 返回失败，消息 `The assignment deadline has passed.` |
| JDG-11 | 提交 | assignmentId 为空 | 已登录学生 | 提交时 assignmentId 为空 | 返回 `400`，消息 `Validation failed` |
| JDG-12 | 提交 | sourceCode 为空 | 已登录学生 | 提交时源码为空 | 返回 `400`，消息 `Validation failed` |
| JDG-13 | 提交 | 冒用其他学生 ID 提交 | 已存在 `student2` | `student1` 提交时传 `studentId=student2.id` | 返回 `403`，提示只能为自己提交 |
| JDG-14 | 提交 | 指定不存在作业 | 已登录学生 | 提交到不存在 assignmentId | 返回失败，消息 `Assignment not found.` |

## 11. 提交记录与结果查询测试用例

| 编号 | 模块 | 测试项 | 前置条件 | 测试步骤 | 预期结果 |
| --- | --- | --- | --- | --- | --- |
| RES-01 | 学生记录 | 查看本人提交列表 | `student1` 已有多次提交 | 调用 `/api/submissions/student/{student1.id}` | 返回本人提交记录，按时间倒序 |
| RES-02 | 学生记录 | 查看本人某次提交详情 | `student1` 已有提交 | 调用 `/api/submissions/{submissionId}` | 返回提交源码、编译信息、运行信息、caseResults |
| RES-03 | 学生记录 | 查看本人每个作业最新提交摘要 | `student1` 已有多次提交 | 调用 `/api/submissions/student/{student1.id}/latest` | 每个作业只返回最新一条摘要 |
| RES-04 | 学生记录 | 查看某作业最新一次提交 | `student1` 已在 A1 提交 | 调用 `/api/submissions/assignment/{A1}/student/{student1}/latest` | 返回该作业最近一次提交 |
| RES-05 | 教师记录 | 查看自己作业的全部提交 | A1 属于 `teacher1` 且已有提交 | `teacher1` 调用 `/api/submissions/assignment/{A1}` | 返回 A1 的所有提交，按时间倒序 |
| RES-06 | 教师记录 | 查看自己作业中的某条提交详情 | A1 有提交 | `teacher1` 调用 `/api/submissions/{submissionId}` | 成功返回详情 |
| RES-07 | 结果正确性 | 最新摘要去重正确 | `student1` 对同一作业提交 2 次以上 | 调用最新摘要接口 | 该作业只出现一次，且为最新时间的结果 |

## 12. 权限与数据隔离测试用例

| 编号 | 模块 | 测试项 | 前置条件 | 测试步骤 | 预期结果 |
| --- | --- | --- | --- | --- | --- |
| PERM-01 | 角色权限 | 学生访问教师统计接口 | `student1` 已登录 | 调用 `/api/users/overview` | 返回 `403` |
| PERM-02 | 角色权限 | 学生查看全部用户 | `student1` 已登录 | 调用 `/api/users` | 返回 `403` |
| PERM-03 | 角色权限 | 学生创建作业 | `student1` 已登录 | 调用 `POST /api/assignments` | 返回 `403` |
| PERM-04 | 角色权限 | 教师提交代码 | `teacher1` 已登录 | 调用 `POST /api/submissions` | 返回 `403`，提示仅学生可提交 |
| PERM-05 | 学生隔离 | 学生查看其他学生提交列表 | `student1`、`student2` 都存在 | `student1` 调用 `/api/submissions/student/{student2.id}` | 返回 `403` |
| PERM-06 | 学生隔离 | 学生查看其他学生提交详情 | `student2` 已有提交 | `student1` 调用 `/api/submissions/{student2SubmissionId}` | 返回 `403` |
| PERM-07 | 教师隔离 | 教师查看其他教师作业提交 | `teacher2` 有自己的作业 A6 | `teacher1` 调用 `/api/submissions/assignment/{A6}` | 返回 `403` |
| PERM-08 | 教师隔离 | 教师修改其他教师作业 | `teacher2` 有自己的作业 A6 | `teacher1` 调用 `PUT /api/assignments/{A6}` | 返回 `403` |
| PERM-09 | 教师隔离 | 教师删除其他教师作业 | `teacher2` 有自己的作业 A6 | `teacher1` 调用 `DELETE /api/assignments/{A6}` | 返回 `403` |
| PERM-10 | 教师隔离 | 教师查看不属于自己的提交详情 | `teacher2` 作业有学生提交 | `teacher1` 调用 `/api/submissions/{teacher2SubmissionId}` | 返回 `403` |

## 13. 接口异常与鲁棒性测试用例

| 编号 | 模块 | 测试项 | 前置条件 | 测试步骤 | 预期结果 |
| --- | --- | --- | --- | --- | --- |
| ERR-01 | 作业查询 | 查询不存在作业 | 已登录 | 调用 `/api/assignments/999999` | 返回失败，消息 `Assignment not found.` |
| ERR-02 | 提交查询 | 查询不存在提交 | 已登录 | 调用 `/api/submissions/999999` | 返回失败，消息 `Submission not found.` |
| ERR-03 | 最新提交 | 查询不存在最新提交 | 某学生从未提交某作业 | 调用最新提交接口 | 返回失败，消息 `No submission found for the assignment and student.` |
| ERR-04 | 测试用例 | 修改不存在测试用例 | 已登录教师 | 调用更新测试用例接口，使用不存在 testCaseId | 返回失败，消息 `Test case not found.` |
| ERR-05 | 测试用例 | 删除不存在测试用例 | 已登录教师 | 调用删除测试用例接口，使用不存在 testCaseId | 返回失败，消息 `Test case not found.` |
| ERR-06 | 测试用例归属 | 修改不属于作业的测试用例 | 至少有两个作业和不同 testCaseId | 用 A1 的 assignmentId 修改 A2 的 testCaseId | 返回失败，消息 `Test case does not belong to the assignment.` |

## 14. 前后端联动专项测试

| 编号 | 模块 | 测试项 | 前置条件 | 测试步骤 | 预期结果 |
| --- | --- | --- | --- | --- | --- |
| E2E-01 | 教师完整流程 | 教师从登录到查看统计 | `teacher1` 已存在 | 登录教师账号，创建作业，查看作业列表，查看统计 | 全流程成功，页面数据同步刷新 |
| E2E-02 | 学生完整流程 | 学生从登录到提交成功 | A1 已存在 | 登录学生账号，选择 A1，提交正确代码，查看结果详情 | 页面显示 `ACCEPTED`、100 分、用例全部通过 |
| E2E-03 | 学生异常流程 | 学生提交编译错误代码 | A1 已存在 | 登录学生账号，提交编译错误代码 | 页面显示 `COMPILE_ERROR` 和编译报错信息 |
| E2E-04 | 教师查看学生结果 | 教师查看某次提交详情 | `student1` 已对 A1 提交 | 登录教师账号，加载 A1 提交列表，查看一条详情 | 页面显示提交源码、编译信息、运行信息、测试用例结果 |
| E2E-05 | 注销后保护 | 登出后直接访问 hash 工作台地址 | 已登录后退出 | 浏览器输入 `#/teacher` 或 `#/student` | 跳回登录页或首页，不应继续显示受保护工作台 |

## 15. 建议执行顺序

建议按以下顺序执行测试，效率最高：

1. 环境启动与默认账号验证
2. 前端页面跳转与登录注册
3. 教师端作业创建与作业管理
4. 学生端提交与自动评测
5. 教师端查看提交记录与统计
6. 权限隔离与异常场景
7. 回归测试核心流程

## 16. 回归测试最小集合

如果时间不够，至少执行以下用例：

- `UI-01`
- `UI-07`
- `UI-08`
- `AUTH-07`
- `AUTH-08`
- `TCH-04`
- `TCH-13`
- `TCH-20`
- `STU-01`
- `JDG-01`
- `JDG-04`
- `JDG-05`
- `JDG-06`
- `RES-03`
- `PERM-01`
- `PERM-05`
- `E2E-01`
- `E2E-02`
