param(
    [string]$OutputDirectory = [Environment]::GetFolderPath('Desktop')
)

$ErrorActionPreference = 'Stop'

function Get-UniquePath {
    param([string]$Path)
    if (-not (Test-Path -LiteralPath $Path)) {
        return $Path
    }
    $dir = Split-Path -Parent $Path
    $name = [IO.Path]::GetFileNameWithoutExtension($Path)
    $ext = [IO.Path]::GetExtension($Path)
    for ($i = 1; $i -lt 1000; $i++) {
        $candidate = Join-Path $dir ("{0}_{1}{2}" -f $name, $i, $ext)
        if (-not (Test-Path -LiteralPath $candidate)) {
            return $candidate
        }
    }
    throw "无法生成唯一文件名：$Path"
}

$title = '基于 Spring Boot 的作业自动评测与管理系统设计与实现'
$englishTitle = 'Design and Implementation of an Assignment Auto Grading and Management System Based on Spring Boot'
$outputPath = Get-UniquePath (Join-Path $OutputDirectory '作业自动评测与管理系统论文初稿.docx')

$word = New-Object -ComObject Word.Application
$word.Visible = $false
$word.DisplayAlerts = 0

$doc = $word.Documents.Add()
$selection = $word.Selection

$wdAlignLeft = 0
$wdAlignCenter = 1
$wdPageBreak = 7
$wdStyleNormal = -1
$wdStyleHeading1 = -2
$wdStyleHeading2 = -3
$wdStyleHeading3 = -4
$wdFormatXMLDocument = 16

$doc.PageSetup.TopMargin = $word.CentimetersToPoints(2.5)
$doc.PageSetup.BottomMargin = $word.CentimetersToPoints(2.5)
$doc.PageSetup.LeftMargin = $word.CentimetersToPoints(3.0)
$doc.PageSetup.RightMargin = $word.CentimetersToPoints(2.0)

function Set-BaseFont {
    param([int]$Size = 12, [bool]$Bold = $false)
    $selection.Font.NameFarEast = '宋体'
    $selection.Font.Name = 'Times New Roman'
    $selection.Font.Size = $Size
    $selection.Font.Bold = [int]$Bold
    $selection.ParagraphFormat.LineSpacingRule = 1
    $selection.ParagraphFormat.SpaceBefore = 0
    $selection.ParagraphFormat.SpaceAfter = 0
}

function Add-Paragraph {
    param(
        [string]$Text,
        [int]$Style = $wdStyleNormal,
        [int]$Align = $wdAlignLeft,
        [int]$Size = 12,
        [bool]$Bold = $false
    )
    $selection.Style = $Style
    Set-BaseFont -Size $Size -Bold:$Bold
    $selection.ParagraphFormat.Alignment = $Align
    $selection.TypeText($Text)
    $selection.TypeParagraph()
}

function Add-Blank {
    param([int]$Count = 1)
    for ($i = 0; $i -lt $Count; $i++) {
        $selection.TypeParagraph()
    }
}

function Add-PageBreak {
    $selection.InsertBreak($wdPageBreak)
}

function Add-CodeBlock {
    param([string]$Text)
    $selection.Style = $wdStyleNormal
    # Some Word versions reject assigning a Latin monospace font to NameFarEast.
    # Set the general font name only; Chinese comments remain readable with fallback fonts.
    $selection.Font.Name = 'Consolas'
    $selection.Font.Size = 9
    $selection.Font.Bold = 0
    $selection.ParagraphFormat.Alignment = $wdAlignLeft
    $selection.ParagraphFormat.LineSpacingRule = 0
    $selection.ParagraphFormat.SpaceBefore = 3
    $selection.ParagraphFormat.SpaceAfter = 3
    foreach ($line in ($Text -split "`r?`n")) {
        $selection.TypeText($line)
        $selection.TypeParagraph()
    }
    Set-BaseFont
}

function Add-Table {
    param(
        [string]$Caption,
        [string[]]$Headers,
        [object[]]$Rows
    )
    Add-Paragraph -Text $Caption -Align $wdAlignCenter -Size 10 -Bold:$true
    $range = $selection.Range
    $table = $doc.Tables.Add($range, $Rows.Count + 1, $Headers.Count)
    $table.Borders.Enable = 1
    $table.Range.Font.NameFarEast = '宋体'
    $table.Range.Font.Name = 'Times New Roman'
    $table.Range.Font.Size = 10
    for ($c = 0; $c -lt $Headers.Count; $c++) {
        $cell = $table.Cell(1, $c + 1)
        $cell.Range.Text = $Headers[$c]
        $cell.Range.Bold = 1
    }
    for ($r = 0; $r -lt $Rows.Count; $r++) {
        for ($c = 0; $c -lt $Headers.Count; $c++) {
            $table.Cell($r + 2, $c + 1).Range.Text = [string]$Rows[$r][$c]
        }
    }
    $table.AutoFitBehavior(1)
    $selection.SetRange($table.Range.End, $table.Range.End)
    $selection.TypeParagraph()
}

function Add-Markdown {
    param([string]$Text)
    $inCode = $false
    $code = New-Object System.Text.StringBuilder
    foreach ($rawLine in ($Text -split "`r?`n")) {
        $line = $rawLine.TrimEnd()
        if ($line -eq '```') {
            if ($inCode) {
                Add-CodeBlock $code.ToString().TrimEnd()
                [void]$code.Clear()
                $inCode = $false
            } else {
                $inCode = $true
            }
            continue
        }
        if ($inCode) {
            [void]$code.AppendLine($rawLine)
            continue
        }
        if ($line.Trim().Length -eq 0) {
            Add-Blank 1
        } elseif ($line.StartsWith('# ')) {
            Add-PageBreak
            Add-Paragraph -Text $line.Substring(2).Trim() -Style $wdStyleHeading1 -Size 16 -Bold:$true
        } elseif ($line.StartsWith('## ')) {
            Add-Paragraph -Text $line.Substring(3).Trim() -Style $wdStyleHeading2 -Size 15 -Bold:$true
        } elseif ($line.StartsWith('### ')) {
            Add-Paragraph -Text $line.Substring(4).Trim() -Style $wdStyleHeading3 -Size 14 -Bold:$true
        } else {
            Add-Paragraph -Text $line -Size 12
        }
    }
    if ($inCode -and $code.Length -gt 0) {
        Add-CodeBlock $code.ToString().TrimEnd()
    }
}

Set-BaseFont

# 封面
Add-Blank 3
Add-Paragraph -Text '郑州轻工业大学' -Align $wdAlignCenter -Size 22 -Bold:$true
Add-Blank 1
Add-Paragraph -Text '本科毕业设计（论文）' -Align $wdAlignCenter -Size 22 -Bold:$true
Add-Blank 4
Add-Paragraph -Text "题    目    $title" -Align $wdAlignCenter -Size 16 -Bold:$true
Add-Blank 3
Add-Paragraph -Text '学生姓名    【请填写】' -Align $wdAlignCenter -Size 14
Add-Paragraph -Text '专业班级    软件工程 22-0X' -Align $wdAlignCenter -Size 14
Add-Paragraph -Text '学    号    【请填写】' -Align $wdAlignCenter -Size 14
Add-Paragraph -Text '学    院    软件学院' -Align $wdAlignCenter -Size 14
Add-Paragraph -Text '指导教师（职称）    【请填写】' -Align $wdAlignCenter -Size 14
Add-Blank 2
Add-Paragraph -Text '完成时间    2026年5月18日' -Align $wdAlignCenter -Size 14
Add-PageBreak

# 任务书
Add-Paragraph -Text '毕业设计（论文）任务书' -Style $wdStyleHeading1 -Align $wdAlignCenter -Size 16 -Bold:$true
Add-Paragraph -Text "题目：$title"
Add-Paragraph -Text '专业：【请填写】    学号：【请填写】    姓名：【请填写】'
Add-Paragraph -Text '主要内容：本课题面向高校程序设计类课程的作业发布、代码提交、自动评测和成绩管理场景，设计并实现一套基于 Spring Boot 的作业自动评测与管理系统。系统围绕管理员、教师和学生三类用户展开，提供用户认证、课程管理、选课管理、作业发布、测试用例配置、Java 代码提交、异步自动评测、提交结果查看、重新判题、成绩统计、成绩导出、批量导入、操作审计和 AI 辅助诊断等功能。系统采用浏览器静态前端与后端 REST 接口交互的方式组织业务流程，后端使用 Spring Boot、Spring MVC、Spring Data JPA、MySQL/H2、Flyway、Maven 等技术完成业务处理、数据持久化和测试验证。'
Add-Paragraph -Text '基本要求：系统应能够完成教师发布课程作业、学生在线提交代码、系统自动编译运行并返回评测结果、教师查看提交和统计成绩的核心闭环；应支持管理员、教师、学生三类角色的权限隔离；应保存课程、选课、作业、测试用例、提交记录、评测结果、登录令牌和审计日志等关键数据；应提供可运行的前端页面和可复现的演示数据；应编写自动化测试验证主要业务流程和异常场景。'
Add-Paragraph -Text '思政要求：系统设计与实现过程中应体现严谨规范的软件工程意识，重视用户数据安全、教学公平性和系统可维护性；在自动评测过程中应保持评分规则统一和过程可追踪；在论文撰写中应如实描述系统功能、测试情况、工程边界和不足之处。'
Add-Paragraph -Text '完成期限：2026年5月18日'
Add-Paragraph -Text '指导教师签名：________________    专业负责人签名：________________'
Add-Paragraph -Text '2026年5月'
Add-PageBreak

# 摘要
Add-Paragraph -Text $title -Align $wdAlignCenter -Size 16 -Bold:$true
Add-Blank 1
Add-Paragraph -Text '摘  要' -Style $wdStyleHeading1 -Align $wdAlignCenter -Size 16 -Bold:$true
Add-Paragraph -Text '随着程序设计类课程实践教学任务的增多，传统人工批改代码作业的方式在效率、反馈速度、评分一致性和过程追踪方面逐渐暴露出不足。教师需要重复完成代码查看、编译运行、输入输出比对和成绩登记等工作，学生也难以及时获得针对性反馈。针对上述问题，本文设计并实现了一套面向课程教学场景的作业自动评测与管理系统。系统基于 Spring Boot 构建单体 Web 应用，使用 Spring MVC 提供 REST 接口，使用 Spring Data JPA 完成数据持久化，支持 MySQL 正式运行环境和 H2 演示测试环境，并通过内置静态前端页面提供管理员、教师和学生三个工作台。'
Add-Paragraph -Text '系统实现了用户登录与令牌认证、PBKDF2 密码加密、账号启停与密码重置、课程与选课管理、作业发布与测试用例维护、学生 Java 代码提交、异步自动评测、评测详情查询、重新判题、成绩统计与 CSV 导出、批量导入、操作审计以及 AI 辅助诊断等功能。在自动评测模块中，系统能够检测 Java 类名，限制源码长度和高风险 API，调用 JavaCompiler 编译学生源码，在独立临时目录中运行程序，控制运行时间和输出长度，并根据测试用例输出比对结果生成通过、部分通过、失败、编译错误、运行错误和超时等状态。'
Add-Paragraph -Text '系统测试覆盖认证鉴权、课程选课、作业提交流程、评测结果、统计导出、批量导入、管理员管理、前端结构和演示数据初始化等场景。当前项目已经形成可运行、可演示、可测试的教学辅助系统，能够较完整地支撑从课程作业发布到学生提交、自动批改、结果反馈和教师统计的业务闭环。'
Add-Paragraph -Text '关键词：Spring Boot；作业自动评测；Java 判题；课程管理；AI 辅助诊断'
Add-PageBreak

Add-Paragraph -Text 'ABSTRACT' -Style $wdStyleHeading1 -Align $wdAlignCenter -Size 16 -Bold:$true
Add-Paragraph -Text 'With the increasing amount of programming practice in college courses, traditional manual grading of coding assignments has shown limitations in efficiency, feedback timeliness, grading consistency and traceability. Teachers often need to repeatedly inspect source code, compile and run programs, compare outputs and record grades, while students cannot always receive immediate feedback. To address these problems, this thesis designs and implements an assignment auto grading and management system for programming courses. The system is built as a Spring Boot web application, provides RESTful APIs through Spring MVC, persists data with Spring Data JPA, supports MySQL for normal deployment and H2 for demonstration and testing, and provides built-in static front-end workspaces for administrators, teachers and students.'
Add-Paragraph -Text 'The system implements token-based authentication, PBKDF2 password encryption, account activation management, course and enrollment management, assignment publishing, test case maintenance, Java code submission, asynchronous automatic judging, result query, rejudging, grade statistics, CSV export, batch import, audit logging and AI-assisted diagnosis. In the judging module, the system detects the Java class name, validates source code size and restricted APIs, compiles source code through JavaCompiler, executes the program in an isolated temporary directory, controls execution time and output size, and compares actual outputs with expected outputs to generate accepted, partial accepted, failed, compilation error, runtime error and time limit exceeded statuses.'
Add-Paragraph -Text 'The tests cover authentication, authorization, course enrollment, assignment submission, judging results, statistics export, batch import, administrator management, front-end structure and demo data initialization. The implemented system has become a runnable, demonstrable and testable teaching-support system, and can support the complete workflow from assignment publishing to student submission, automatic grading, feedback and teacher-side statistics.'
Add-Paragraph -Text 'KEY WORDS: Spring Boot; Assignment Auto Grading; Java Judge; Course Management; AI-assisted Diagnosis'
Add-PageBreak

# 目录
Add-Paragraph -Text '目  录' -Style $wdStyleHeading1 -Align $wdAlignCenter -Size 16 -Bold:$true
$tocRange = $selection.Range
$toc = $doc.TablesOfContents.Add($tocRange, $true, 1, 3)
$selection.SetRange($toc.Range.End, $toc.Range.End)
Add-PageBreak

$body = @'
# 1 绪论
## 1.1 课题背景
程序设计类课程具有实践性强、作业频率高、结果可验证等特点。学生需要通过不断编写、提交、调试和修改代码来形成编程能力，教师则需要通过作业批改了解学生对语法、流程控制、输入输出、异常处理和算法思想的掌握情况。在传统教学中，代码作业通常依赖教师手动查看源码、手动运行程序、构造输入数据、比对输出结果并登记成绩。这种方式在小规模班级中尚可维持，但当课程人数增加、作业频率提高后，教师批改压力明显增大，学生反馈周期也会变长。

自动评测系统能够把重复性的编译、运行、输出比对和评分工作交给程序完成，使教师可以把更多精力投入题目设计、疑难讲解和学习过程分析中。对于学生而言，提交后尽快获得编译信息、运行信息、得分和测试用例结果，有助于形成“编写代码、提交评测、查看反馈、修改完善”的学习闭环。本课题基于当前项目代码实现，围绕课程教学中的作业管理和 Java 程序自动批改需求，设计并实现一套作业自动评测与管理系统。

## 1.2 课题意义
从教学管理角度看，系统通过统一的课程、作业、测试用例和成绩模型，把教师端发布任务、学生端提交代码和系统端自动评测整合为一条完整业务链路。教师可以在系统中创建课程、维护选课学生、发布作业、配置测试用例、查看提交详情和导出成绩，减少人工批改和人工汇总的重复劳动。

从学生学习角度看，系统能够对学生提交的 Java 源码进行自动编译、运行和结果比对，并返回清晰的评测状态。学生不仅可以看到是否通过，还可以查看编译错误、运行错误、超时提示、各测试用例输入输出和得分情况。系统还集成 AI 辅助诊断入口，在不直接给出完整标准答案的前提下，为学生提供错误原因、修改建议和相关知识点提示。

从工程实践角度看，本系统不是单纯的增删改查项目，而是综合包含权限控制、数据建模、异步任务、进程执行、输入输出处理、异常处理、批量导入、成绩统计、前端交互和自动化测试等内容。特别是自动评测模块涉及源码校验、类名识别、编译诊断、进程运行、超时控制、输出截断、结果计算和临时文件清理，能够体现软件工程专业毕业设计的系统性和实践价值。

## 1.3 课题定位
本系统定位于高校程序设计课程辅助教学平台，而不是面向大规模公开竞赛的在线评测平台。因此，系统更关注课程、选课、作业、教师管理、学生反馈和成绩统计等教学流程，而不是题库排名、海量并发和分布式评测。当前评测器采用轻量级 Java 编译运行机制，配合源码限制、进程超时、输出长度限制和临时目录隔离降低风险，适合课程演示和毕业设计交付。生产级在线评测平台通常还需要容器沙箱、系统调用隔离、资源配额和更严格的安全策略，本文将在不足与展望中如实说明。

## 1.4 本文主要工作
本文围绕“作业自动评测与管理系统”完成以下工作。第一，分析管理员、教师、学生三类角色在课程教学场景中的功能需求和权限边界。第二，设计系统总体架构，将前端页面、REST 控制器、业务服务、数据访问、领域实体、自动评测和 AI 诊断进行分层组织。第三，设计数据库模型，建立用户、课程、选课、作业、测试用例、提交、评测结果、认证令牌、审计日志和 AI 设置等核心表。第四，实现用户认证、课程选课、作业管理、代码提交、自动评测、统计导出、批量导入、操作审计和 AI 辅助诊断等功能。第五，设计 demo profile 和演示数据，保证系统在本地无需 MySQL 也可快速启动演示。第六，编写自动化测试覆盖主要业务链路和异常场景，并记录当前测试执行结果和发现的问题。

## 1.5 论文结构
本文共分为六章及附录。第 1 章介绍课题背景、意义、定位和主要工作。第 2 章介绍系统开发涉及的相关技术与运行环境。第 3 章进行系统需求分析，说明角色、功能和非功能需求。第 4 章进行系统总体设计，包括架构、模块、数据库和权限设计。第 5 章介绍系统详细设计与实现，重点说明认证、课程、作业、提交、自动评测、统计、导入、AI 诊断和前端实现。第 6 章介绍系统测试方案、测试用例和当前执行结果。最后给出结束语、致谢和附录，附录中包含运行说明、接口清单以及 UML 图的 PlantUML 源码。

# 2 相关技术与开发环境
## 2.1 Java 与 Spring Boot
系统后端采用 Java 语言开发，项目配置目标版本为 Java 17。Java 具备成熟的类型系统、标准类库和工具链，适合构建教学管理类 Web 系统。本系统的自动评测模块还直接使用 JavaCompiler API 对学生提交的 Java 源码进行编译，因此 Java 语言既是系统实现语言，也是当前系统主要支持的评测语言。

Spring Boot 用于简化 Spring 应用搭建，提供自动配置、嵌入式 Web 容器和统一的项目组织方式。本项目以单体应用形式运行，入口类为 AssignmentAutoGradingApplication。系统通过 Spring Boot 整合 Web、JPA、Validation、WebFlux、Flyway、H2、MySQL 和测试框架，减少底层配置工作，使开发重点集中在业务流程和评测逻辑上。

## 2.2 Spring MVC 与 REST 接口
系统使用 Spring MVC 提供 REST 风格接口。Controller 层按业务域划分为认证、用户、课程、作业、提交、导入、审计和管理员管理等控制器。前端通过 fetch 调用这些接口，并在请求头中携带 Bearer token。后端使用统一异常处理器将参数校验错误、未认证、无权限、资源不存在和业务错误转换为结构化 JSON 响应，便于前端展示明确提示。

## 2.3 Spring Data JPA 与关系数据库
系统使用 Spring Data JPA 完成实体持久化和查询封装。领域模型包括 User、Course、CourseEnrollment、Assignment、TestCase、Submission、JudgeCaseResult、AuthTokenRecord、AuditLog 和 AiSettings。Repository 层通过 JpaRepository 和自定义 JPQL 查询实现分页搜索、统计投影和实体图加载，减少重复数据访问代码。正式配置使用 MySQL 数据库，并通过 Flyway 管理数据库迁移；demo 和测试场景使用 H2 数据库以降低环境依赖。

## 2.4 前端静态页面技术
系统前端位于 src/main/resources/static，由 index.html、styles.css 和 app.js 组成，由 Spring Boot 直接托管。前端没有引入复杂构建链路，而是使用原生 HTML、CSS 和 JavaScript 实现登录注册、角色工作台、门户式导航、表单提交、列表渲染、状态提示、提交轮询和成绩下载等功能。这种方式部署简单，便于毕业设计演示和本地复现。

## 2.5 自动评测相关技术
自动评测模块的核心是 JavaJudgeService。系统通过 JavaCompiler 编译学生提交的源码，通过 ProcessBuilder 启动独立 Java 进程运行已编译类，并向进程标准输入写入测试用例输入。系统读取标准输出和标准错误，控制执行时间和输出长度，再将实际输出与教师配置的期望输出进行规范化比对，最终生成评测状态、分数和用例明细。为降低风险，系统在评测前检查源码长度、package 声明和高风险 API，在运行时清空环境变量、设置临时目录、限制 JVM 内存并在结束后清理临时文件。

## 2.6 Maven 与自动化测试
项目使用 Maven 管理依赖、编译和测试。测试代码基于 Spring Boot Test、JUnit 5 和 MockMvc 编写，既包含 JavaJudgeService 的单元测试，也包含认证、课程、导入、作业提交、统计导出、管理员管理、AI 诊断和前端结构等集成测试。自动化测试能够帮助发现权限边界、评测结果和数据库迁移等问题，是系统稳定性验证的重要依据。

# 3 系统需求分析
## 3.1 用户角色分析
系统包含管理员、教师和学生三类角色。管理员负责平台基础管理，能够查看分页搜索后的用户、课程、作业和审计日志，管理用户启停、重置用户密码以及维护 AI 服务设置。教师负责课程教学流程，能够创建课程、维护选课、导入基础数据、创建和维护作业、配置测试用例、查看学生提交、重新判题、查看统计并导出成绩。学生负责课程学习流程，能够查看已选课程和已发布作业，提交 Java 代码，查看本人提交结果，并对本人已完成评测的提交发起 AI 辅助分析。

## 3.2 功能需求
系统的功能需求可以概括为以下几个方面。

（1）用户认证与账号管理。系统应支持注册、登录、退出和当前用户查询；密码应加密保存；登录后应签发有过期时间的 token；账号被禁用或密码被重置后，旧 token 应失效。

（2）课程与选课管理。教师应能够创建、更新、停用和删除课程；课程包含课程代码、课程名称、学期、班级和任课教师；教师可以为课程添加或移除学生；学生只能看到自己已选且启用的课程。

（3）作业与测试用例管理。教师应能够创建作业，设置标题、说明、截止时间、状态、所属课程、最大提交次数、是否允许迟交和评分策略；教师可以新增、修改、删除测试用例；学生只能查看已发布且自己已选课程下的作业，浏览作业时不直接获得教师配置的测试用例集合。

（4）提交与自动评测。学生应能够提交 Java 源码；系统应校验作业状态、截止时间、选课关系和提交次数；提交创建后先进入 PENDING 状态，再由后台异步判题任务完成编译、运行、比对和评分。系统应支持 ACCEPTED、PARTIAL_ACCEPTED、FAILED、COMPILE_ERROR、RUNTIME_ERROR 和 TIME_LIMIT_EXCEEDED 等结果。

（5）结果查询与重新判题。学生只能查看本人提交记录和最新提交概览；教师只能查看自己作业下的提交记录；管理员具备平台管理视角。教师或管理员可以对非 PENDING 的提交发起重新判题，系统清空旧用例结果并重新进入异步评测流程。

（6）统计、导出与导入。教师应能够查看课程统计和作业统计，系统应根据 LATEST 或 HIGHEST 策略计算有效提交成绩；教师可以将某作业成绩导出为 CSV；教师可以批量导入用户、课程和选课关系，导入过程中应跳过无效行并返回明细。

（7）审计与 AI 诊断。系统应记录注册、登录、课程、作业、提交、判题、导入、账号管理和 AI 诊断等操作日志。学生可以对自己的评测结果发起 AI 辅助分析；管理员可以配置 AI 服务启用状态、地址、模型、API Key 和超时时间。

## 3.3 非功能需求
系统应具备基本安全性，能够区分未登录和越权访问，避免仅依赖前端隐藏按钮实现权限控制。系统应具备可演示性，demo profile 应能自动准备管理员、教师、学生、课程、选课、作业和测试用例。系统应具备可维护性，后端按 Controller、Service、Repository、Domain、DTO、Config、Auth、Judge 等包组织。系统应具备可测试性，核心业务应有自动化测试覆盖。系统应具备一定健壮性，对参数校验、资源不存在、提交超时、编译失败、运行失败、输出过长和 AI 配置缺失等情况返回明确结果。

## 3.4 业务流程分析
教师端主流程为：教师登录系统，创建课程并维护选课学生，创建作业并配置测试用例，发布作业后等待学生提交，查看提交列表、评测详情和统计结果，必要时修改测试用例并重新判题，最后导出成绩。学生端主流程为：学生登录系统，查看已选课程和已发布作业，选择作业并提交 Java 代码，系统后台自动评测，学生查看提交详情和最近提交概览，若需要进一步理解错误，可以发起 AI 辅助诊断。管理员端主流程为：管理员登录系统，查看用户、课程、作业和审计日志，进行分页搜索、用户启停、密码重置和 AI 设置维护。

# 4 系统总体设计
## 4.1 总体架构设计
系统采用浏览器前端与 Spring Boot 后端交互的单体架构。前端由静态页面组成，通过 hash 路由区分产品首页、登录注册页、管理员工作台、教师工作台和学生工作台。后端由 REST Controller 接收请求，AuthInterceptor 统一解析和校验 token，Service 层处理业务规则，Repository 层访问数据库，Domain 层表示核心实体。自动评测模块通过异步线程池执行，避免提交接口长时间阻塞。AI 诊断模块通过 WebClient 调用外部兼容聊天接口，并将返回 JSON 解析为结构化诊断结果。

## 4.2 模块划分
系统主要划分为八个模块。认证与用户模块负责注册、登录、token、密码加密、用户启停和密码重置。课程与选课模块负责课程创建、更新、删除、启停、选课和课程统计。作业模块负责作业发布、状态维护、测试用例管理、评分策略和成绩导出。提交模块负责代码提交、提交查询、最新摘要、重新判题和权限过滤。自动评测模块负责源码校验、编译、运行、输出比对和结果生成。导入模块负责 CSV 用户、课程和选课导入。审计模块负责操作日志保存和查询。AI 诊断模块负责 AI 设置、提示词构造、服务调用和结果展示。

## 4.3 数据库设计
系统数据库围绕教学业务闭环设计。users 表保存用户基本信息、角色和启用状态；auth_tokens 表保存登录令牌和过期时间；courses 表保存课程信息和任课教师；course_enrollments 表保存课程与学生的多对多选课关系；assignments 表保存作业基本信息、状态、评分策略和提交限制；test_cases 表保存作业测试用例；submissions 表保存学生源码、评测状态、成绩和编译运行信息；judge_case_results 表保存每个测试用例的输入、期望输出、实际输出和是否通过；audit_logs 表保存操作审计；ai_settings 表保存 AI 服务运行配置。

## 4.4 权限控制设计
系统采用后端集中权限校验。所有 /api/** 接口除登录和注册外均经过 AuthInterceptor，未携带 token 的请求返回 401。Service 层根据当前用户角色和资源归属进一步校验权限，学生访问教师接口、教师访问其他教师作业、学生查看他人提交、教师提交学生代码、非管理员访问管理员接口等场景会返回 403。账号被禁用时，系统会清理该用户令牌并拒绝继续访问。管理员可以启停账号和重置密码，重置后旧 token 失效。

## 4.5 自动评测流程设计
学生提交代码后，SubmissionService 校验学生身份、作业状态、课程选课、截止时间和提交次数，然后创建 PENDING 提交记录。事务提交后，系统通过 SubmissionJudgeQueueService 将判题任务交给名为 judgeTaskExecutor 的异步线程池。JavaJudgeService 检测类名和源码安全限制，调用 JavaCompiler 编译源码；编译失败则返回 COMPILE_ERROR；编译成功后逐个运行测试用例。运行阶段通过 ProcessBuilder 启动 Java 进程，写入测试输入，读取输出和错误流，判断是否超时、是否输出过长、是否运行异常，再进行输出比对并计算通过数和分数。评测完成后系统更新提交状态、分数、编译信息、运行信息和用例结果，并记录审计日志。

## 4.6 前端交互设计
前端以角色工作台为核心。管理员工作台提供平台总览、用户列表、课程列表、作业列表、审计日志和 AI 设置。教师工作台提供课程与选课、创建作业、作业管理、查看提交、评分统计、批量导入和操作日志。学生工作台提供课程选择、已发布作业、代码提交、最近提交和评测详情。学生提交后前端会显示 PENDING 状态并轮询提交详情，直至后台评测完成。页面内置 demo 账号填充功能和默认 Java 代码，便于答辩快速演示。

# 5 系统详细设计与实现
## 5.1 用户认证与账号管理实现
系统认证接口位于 AuthController，包含注册、登录、退出和当前用户查询。注册和登录成功后，AuthTokenService 生成由 UUID 拼接形成的长 token，并写入 auth_tokens 表。token 包含签发时间和过期时间，默认有效期为 12 小时。每次访问受保护接口时，AuthInterceptor 从 Authorization: Bearer 或 X-Auth-Token 中提取 token，并调用 AuthTokenService.authenticate 校验有效性和用户启用状态。

密码管理由 PasswordService 实现。系统使用 PBKDF2WithHmacSHA256 对明文密码加盐哈希，存储格式包含算法前缀、salt 和 hash。登录时系统拒绝非 PBKDF2 格式的存储密码，避免向明文密码回退。管理员通过 UserController 可以禁用或启用用户，也可以重置密码。禁用账号或重置密码后，系统会删除该用户所有 token，使旧会话立即失效。

## 5.2 课程与选课模块实现
CourseService 负责课程和选课业务。教师创建课程时，系统校验课程代码唯一，并将当前用户作为课程教师。课程支持更新代码、名称、学期、班级和启用状态。删除课程前，系统检查该课程下是否已有作业；若已有作业则禁止删除，避免作业与提交数据失去归属。学生查询课程时，系统只返回该学生已选且 active 为 true 的课程。

选课关系由 CourseEnrollment 表维护，并通过唯一约束保证同一学生不能重复加入同一课程。教师可以在自己课程下添加或移除学生；管理员可以从平台视角管理课程。作业与课程关联后，学生查看作业或提交作业时，系统会校验学生是否选修该作业所属课程，从而实现课程范围内的数据隔离。

## 5.3 作业与测试用例模块实现
AssignmentService 负责作业创建、更新、删除、测试用例维护、统计和导出。作业包含 DRAFT、PUBLISHED、CLOSED 三种状态，只有 PUBLISHED 作业允许学生提交。作业还包含 maxSubmissions、lateSubmissionAllowed 和 gradingPolicy 字段。maxSubmissions 默认值为 5，取值范围为 1 到 100；lateSubmissionAllowed 控制作业截止后是否仍可提交；gradingPolicy 支持 LATEST 和 HIGHEST，分别表示按最后一次提交或最高分提交计算有效成绩。

测试用例由 TestCase 实体保存，包含 inputData 和 expectedOutput。教师可以创建作业时同时提交测试用例，也可以后续新增、修改或删除。学生浏览作业详情时，AssignmentController 会对学生请求隐藏 testCases 列表，避免学生直接获得教师配置的测试数据。学生提交后查看本人提交详情时，则可以看到本次评测产生的用例输入、期望输出、实际输出和通过状态，用于理解自己的错误。

## 5.4 提交与异步判题实现
SubmissionService 负责创建提交、查询提交、重新判题和最新摘要。学生提交时，系统强制要求当前用户为 STUDENT，且只能为自己提交。系统校验作业存在、所属课程已选、状态为 PUBLISHED、未超过截止时间或允许迟交、未达到最大提交次数后，创建 Submission 记录并设置为 PENDING。提交记录初始 compileMessage 为“评测任务已进入队列”，runtimeMessage 为“等待后台评测完成”。

为了避免 HTTP 请求被编译和运行过程阻塞，系统在事务提交后通过 TransactionSynchronization 调用 SubmissionJudgeQueueService.judgeSubmissionAsync。该方法使用 Spring @Async 和 judgeTaskExecutor 线程池执行后台判题。评测完成后，系统更新提交类名、状态、分数、编译信息、运行信息和 JudgeCaseResult 明细。若判题过程出现未捕获异常，系统将提交标记为 FAILED，避免提交永久停留在 PENDING。

## 5.5 Java 自动评测模块实现
JavaJudgeService 是系统技术含量最高的模块之一。首先，系统通过正则优先识别 public class 类名，若没有 public class 则识别普通 class，默认类名为 Main。其次，系统进行源码校验，包括源码不能为空、长度不能超过配置限制、不能声明 package、不能使用 ProcessBuilder、Runtime.getRuntime、System.exit、文件访问、网络、反射、Unsafe、环境变量读取、线程和执行器等高风险 API。

编译阶段，系统创建临时工作目录，将源码写入“类名.java”，调用 ToolProvider.getSystemJavaCompiler 获取编译器，并使用 UTF-8 编码和 -d 参数输出 class 文件。编译失败时，系统把诊断信息格式化为“第 X 行：错误信息”并返回 COMPILE_ERROR。若当前运行环境没有 Java 编译器，系统也会返回编译错误并提示运行环境不可用。

运行阶段，系统为每个测试用例启动独立 Java 进程，并设置 -Xms32m、-Xmx128m、UseSerialGC、DisableAttachMechanism、headless、file.encoding、user.home 和 java.io.tmpdir 等参数。系统清空进程环境变量，向标准输入写入测试数据，同时异步读取标准输出和标准错误。若进程超过配置的 5 秒超时限制，系统强制销毁进程并返回 TIME_LIMIT_EXCEEDED；若输出超过配置长度，返回运行错误；若退出码非 0，返回运行错误；否则比较实际输出与期望输出。系统使用 strip 和换行规范化减少末尾空白差异对结果的影响。

评分阶段，系统按通过用例数占总用例数的比例四舍五入计算分数。全部通过为 ACCEPTED，部分通过为 PARTIAL_ACCEPTED，全部不通过为 FAILED；若出现超时则优先返回 TIME_LIMIT_EXCEEDED；若运行错误且没有通过任何用例则返回 RUNTIME_ERROR。当前实现中，若前序用例发生超时或运行时致命错误，后续用例会记录为跳过，避免重复执行风险代码。

## 5.6 统计与成绩导出实现
系统提供作业统计和课程统计。作业统计包含作业 ID、标题、状态、提交总数、参与学生数和平均分。平均分不是简单对所有提交求平均，而是按学生分组后根据作业评分策略选择有效提交：LATEST 取最后一次提交，HIGHEST 取最高分提交。课程统计包含课程、选课人数、作业数量、已发布作业数量、提交学生数、提交总数和平均成绩。

成绩导出由 exportAssignmentGradesCsv 实现。系统根据作业所属课程获取应参与学生列表，若作业没有课程则根据已有提交学生生成列表。导出字段包括用户名、姓名、班级、状态、成绩、提交时间、提交次数和有效提交 ID。CSV 内容前添加 UTF-8 BOM，便于在常见表格软件中正确显示中文。状态在导出时转换为“通过、部分通过、未通过、编译错误、运行错误、超时、待评测、未提交”等中文描述。

## 5.7 批量导入与审计日志实现
ImportService 支持导入用户、课程和选课关系。用户 CSV 格式为 username,password,role,fullName,className；课程 CSV 格式为 code,name,term,className；选课 CSV 格式为 courseCode,studentUsername。导入器支持双引号包裹字段和双引号转义，能够处理包含逗号的姓名或班级。无效行、重复行、权限不足行不会中断整个批次，而是计入 skippedCount 并返回 details 明细。教师不能导入 ADMIN 角色用户，管理员可以导入。

AuditLogService 为关键操作记录审计日志，包括用户注册、登录、课程创建更新删除、选课变更、作业创建更新删除、测试用例变更、提交创建、判题完成、重新判题、批量导入、账号启停、密码重置和 AI 诊断。管理员可以查询最新日志，也可以通过管理员接口分页搜索日志。

## 5.8 AI 辅助诊断实现
AI 诊断模块由 AiSettings、AiSettingsService、AiDiagnosisPromptBuilder、AiDiagnosisService 和 DeepSeekAiClient 组成。管理员通过 /api/admin/ai-settings 查看和更新 AI 配置，包括是否启用、服务地址、模型、API Key 和超时时间。学生在查看本人提交详情时可以发起 AI 辅助分析，教师和管理员不能替学生发起该分析，学生也不能分析他人的提交。

提示词构造器会收集作业标题、提交状态、分数、编译信息、运行信息、测试用例结果和学生源码，并限制源码最大长度。系统要求 AI 输出 JSON 对象，包含 summary、possibleCauses、fixSuggestions 和 knowledgePoints 四类内容，同时要求不直接给出完整标准答案。DeepSeekAiClient 使用 WebClient 调用兼容 /chat/completions 的接口，并将返回 JSON 解析为 AiDiagnosisResponse。响应中包含固定免责声明：“AI 分析仅供学习参考，不参与正式评分。”

## 5.9 前端页面实现
前端页面以 index.html 为主体，styles.css 负责视觉样式和响应式布局，app.js 负责路由、状态管理、接口调用和渲染。系统前端提供产品首页、登录注册页、管理员工作台、教师工作台和学生工作台。登录后前端将 token 保存在 localStorage 中，并在后续 API 请求中加入 Authorization 请求头。刷新页面时，前端会调用 /api/auth/me 恢复会话，并根据角色跳转到对应工作台。

教师端页面包含课程与选课、创建作业、作业管理、查看提交、评分统计、批量导入和操作日志模块。学生端页面包含课程选择、已发布作业、代码提交、最近提交和评测详情模块。提交代码后，前端会显示评测中的详情面板，并通过轮询 /api/submissions/{id} 获取最终结果。管理员端页面包含系统概览、用户、课程、作业、审计日志和 AI 设置，并提供分页搜索和状态管理入口。

## 5.10 Demo 配置与演示数据实现
系统提供 demo profile，用于本地演示和答辩。application-demo.yml 将数据库切换为 H2 文件数据库，关闭 Flyway，使用 JPA 自动更新表结构，并启用 H2 Console。DataSeeder 仅在 demo profile 下生效，启动后会创建 admin1、teacher1、student1 三个账号，创建课程 CS101 · 程序设计基础，为 student1 建立选课关系，并创建“演示：两数求和”作业。该作业状态为 PUBLISHED，最大提交次数为 5，评分策略为 LATEST，测试用例为“1 2 -> 3”和“6 9 -> 15”。因此系统启动后即可直接演示管理员、教师和学生三类角色的核心流程。

# 6 系统测试
## 6.1 测试环境
当前项目使用 Maven 执行自动化测试。项目配置 Java 17，当前本机测试执行环境显示为 Java 21.0.10。测试主要使用 H2 数据库，demo 测试使用 demo profile，部分默认 profile 测试显式配置 H2 内存数据库。测试框架包括 Spring Boot Test、JUnit 5 和 MockMvc。自动评测相关测试会实际调用 Java 编译器并运行学生代码，因此能够验证编译、运行、超时和输出限制等逻辑。

## 6.2 测试范围
测试代码覆盖管理员管理、管理员分页搜索、AI 诊断、认证鉴权、demo 数据初始化、默认 profile 数据种子隔离、课程管理、作业提交完整流程、前端静态结构、批量导入、Java 判题、统计导出和用户管理等模块。测试既包含成功场景，也包含未登录、越权、非法参数、账号禁用、密码重置、课程删除限制、迟交限制、最大提交次数、编译错误、运行错误、超时和 AI 配置缺失等异常场景。

## 6.3 核心功能测试
认证测试验证 /api/auth/me 未登录返回 401，空用户名密码返回参数校验错误，学生访问教师统计接口返回 403，教师登录后能够获取当前用户信息。用户管理测试验证管理员可以禁用和启用账号，禁用后旧 token 失效，重置密码后旧密码不能继续登录，非管理员不能管理用户账号。管理员测试验证管理员可以分页搜索用户、课程、作业和审计日志，也可以读取和更新 AI 设置。

课程测试验证教师可以更新和删除无作业课程，不能删除已有作业的课程；学生只看到自己已选课程。导入测试验证用户、课程和选课 CSV 支持带引号字段，导入过程中能跳过无效行，教师不能导入管理员账号而管理员可以。前端结构测试验证学生课程面板位于学生区域，AI 诊断面板只出现在学生详情区域，管理员 AI 设置面板存在。

## 6.4 自动评测测试
JavaJudgeService 单元测试验证正确代码返回 ACCEPTED 和 100 分，包含 package 声明的代码被拒绝，超长源码被拒绝，输出超过限制时返回运行错误并带有“输出已截断”标记，死循环代码返回 TIME_LIMIT_EXCEEDED 且后续用例被跳过。提交流程集成测试验证教师创建作业后学生可完成提交并获得 ACCEPTED 结果；编译错误代码返回 COMPILE_ERROR；教师修改测试用例后可以重新判题，提交从 PENDING 变为新的 PARTIAL_ACCEPTED 结果；学生不能重新判题；教师只能看到自己作业范围内的学生提交。

## 6.5 统计与导出测试
统计导出测试验证教师可以查看课程统计并导出作业成绩 CSV。评分策略测试验证 HIGHEST 策略会在多次提交中选择最高分作为有效成绩，并在统计平均分和成绩导出中使用对应提交 ID。迟交测试验证超过截止时间时，默认禁止提交；若作业允许迟交，则仍可提交并进入评测。最大提交次数测试验证当作业提交次数上限为 1 时，第二次提交会被拒绝。

## 6.6 当前测试执行结果
本次在项目根目录执行 mvn test。测试报告显示，管理员、AI 诊断、认证、demo 数据、默认 profile 数据初始化隔离、课程、提交流程、前端结构、导入、Java 判题、统计导出和用户管理等测试均通过。当前统计为 41 个测试方法全部通过、0 个断言失败、0 个错误、0 个跳过，构建结果为 BUILD SUCCESS。

测试过程中曾发现 DataSeederProfileIntegrationTest.defaultProfileDoesNotSeedDemoUsersOrCourses 在默认 profile 下会触发 Flyway 迁移脚本执行，而该测试的目标仅是验证非 demo profile 不会自动初始化演示用户和课程。为避免 H2 临时数据库与 MySQL 迁移脚本方言差异影响该测试结论，最终在该测试中显式关闭 Flyway，并继续使用 Hibernate create-drop 生成隔离测试表结构。修复后单独执行该测试通过，随后执行全量 mvn test 也全部通过。

## 6.7 测试结论
综合测试结果可知，系统核心业务流程已经具备较好的可用性：教师可以创建课程、维护选课、发布作业和配置用例；学生可以查看课程作业并提交 Java 代码；系统能够异步完成自动评测并返回状态、分数和用例明细；教师可以查看提交、重新判题、统计和导出成绩；管理员可以完成平台基础管理和 AI 设置维护。当前自动化测试已经覆盖主要教学闭环和管理功能，后续仍可围绕生产级判题沙箱、更多编程语言支持和更细粒度的性能压测继续扩展。

# 结束语
本文设计并实现了一套基于 Spring Boot 的作业自动评测与管理系统。系统面向高校程序设计课程教学场景，围绕管理员、教师和学生三类角色，建立了课程、选课、作业、测试用例、提交、评测结果、统计、导入、审计和 AI 诊断等功能模块。通过该系统，教师能够发布作业并配置测试用例，学生能够在线提交 Java 代码，系统能够自动完成编译、运行、输出比对和评分，教师和学生能够分别查看权限范围内的评测结果。

从工程实现角度看，系统完成了后端 REST 接口、领域实体、数据访问、业务服务、认证拦截、异常处理、异步判题、静态前端和自动化测试的完整开发。系统通过 PBKDF2 加密密码和 token 持久化实现基础认证，通过 Service 层权限校验实现角色与资源归属隔离，通过异步线程池提升提交接口响应能力，通过 demo profile 和 DataSeeder 降低答辩演示环境依赖。

当前系统仍存在一些不足。首先，自动评测模块采用轻量级进程隔离和源码限制方式，尚未达到生产级沙箱安全标准。其次，当前主要支持 Java 语言评测，尚未扩展到 C、C++、Python 等多语言。再次，前端采用静态页面实现，虽然便于演示，但在组件复用、可视化统计和复杂交互方面仍有提升空间。最后，当前测试发现默认 profile 在 H2 环境下执行 MySQL 风格 Flyway 迁移存在兼容性问题，后续应进行工程修复。未来可以引入容器化评测沙箱、多语言判题、代码相似度检测、成绩可视化、题库管理和更完善的教学分析功能，使系统更加接近真实教学平台。

# 致谢
本论文和系统的完成离不开指导教师、学院老师和同学们的帮助。在毕业设计过程中，指导教师在选题确定、需求分析、系统设计、功能实现、测试验证和论文结构等方面给予了耐心指导，使我能够逐步明确项目目标并完成系统开发。学院提供的毕业设计规范和论文模板为本文写作提供了明确依据，也帮助我更加规范地组织论文内容。

感谢在学习和开发过程中给予帮助的同学们。与同学交流系统功能、页面演示和测试场景的过程，使我发现了许多容易忽略的问题，也促使我不断完善项目。通过本次毕业设计，我对 Java Web 开发、数据库设计、权限控制、异步任务、自动化测试和程序自动评测有了更加系统的认识，也进一步体会到软件工程实践中需求分析、稳定性验证和文档整理的重要性。

# 附录 A 系统运行说明
## A.1 Demo profile 启动
系统推荐使用 demo profile 进行本地演示。进入项目根目录后执行以下命令：

```
mvn spring-boot:run "-Dspring-boot.run.profiles=demo"
```

启动完成后访问：

```
http://localhost:8080/#/login
```

demo profile 会使用 H2 数据库，并自动准备账号：admin1 / 123456、teacher1 / 123456、student1 / 123456。系统还会自动创建课程 CS101 · 程序设计基础、学生选课关系和演示作业“演示：两数求和”。

## A.2 MySQL 默认配置启动
默认配置连接 MySQL，并启用 Flyway 数据库迁移。启动前可设置数据库环境变量：

```
$env:DB_URL="jdbc:mysql://localhost:3306/graduation_assignment_autograding_isolated?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="你的数据库密码"
mvn spring-boot:run
```

默认 profile 不会自动写入演示账号。若需要答辩演示，应显式使用 demo profile。

# 附录 B 主要接口清单
认证接口包括 POST /api/auth/register、POST /api/auth/login、POST /api/auth/logout 和 GET /api/auth/me。用户接口包括 GET /api/users、GET /api/users/role/{role}、GET /api/users/overview、PUT /api/users/{userId}/status 和 POST /api/users/{userId}/reset-password。课程接口包括 GET /api/courses、POST /api/courses、PUT /api/courses/{courseId}、DELETE /api/courses/{courseId}、POST /api/courses/{courseId}/enrollments/{studentId}、DELETE /api/courses/{courseId}/enrollments/{studentId}、GET /api/courses/{courseId}/enrollments 和 GET /api/courses/statistics/overview。

作业接口包括 POST /api/assignments、GET /api/assignments、GET /api/assignments/published、GET /api/assignments/{assignmentId}、PUT /api/assignments/{assignmentId}、DELETE /api/assignments/{assignmentId}、POST /api/assignments/{assignmentId}/test-cases、PUT /api/assignments/{assignmentId}/test-cases/{testCaseId}、DELETE /api/assignments/{assignmentId}/test-cases/{testCaseId}、GET /api/assignments/statistics/overview 和 GET /api/assignments/{assignmentId}/grades/export。提交接口包括 POST /api/submissions、GET /api/submissions/{submissionId}、POST /api/submissions/{submissionId}/rejudge、POST /api/submissions/{submissionId}/ai-diagnosis、GET /api/submissions/student/{studentId}、GET /api/submissions/assignment/{assignmentId}、GET /api/submissions/assignment/{assignmentId}/student/{studentId}/latest 和 GET /api/submissions/student/{studentId}/latest。导入接口包括 POST /api/import/users、POST /api/import/courses 和 POST /api/import/enrollments。管理员接口包括 GET /api/admin/users、GET /api/admin/courses、GET /api/admin/assignments、GET /api/admin/audit-logs、GET /api/admin/ai-settings 和 POST /api/admin/ai-settings。

# 附录 C UML 图 PlantUML 源码
## C.1 用例图
```
@startuml
left to right direction
actor 管理员 as Admin
actor 教师 as Teacher
actor 学生 as Student

rectangle 作业自动评测与管理系统 {
  usecase 登录与退出 as UCAuth
  usecase 用户启停与密码重置 as UCUser
  usecase AI设置维护 as UCAiSetting
  usecase 课程管理 as UCCourse
  usecase 选课管理 as UCEnroll
  usecase 作业发布与用例配置 as UCAssignment
  usecase 批量导入 as UCImport
  usecase 代码提交 as UCSubmit
  usecase 自动评测 as UCJudge
  usecase 查看提交与评测详情 as UCResult
  usecase 重新判题 as UCRejudge
  usecase 成绩统计与导出 as UCStat
  usecase AI辅助诊断 as UCAiDiag
  usecase 操作日志查询 as UCAudit
}

Admin --> UCAuth
Admin --> UCUser
Admin --> UCAiSetting
Admin --> UCAudit
Admin --> UCStat

Teacher --> UCAuth
Teacher --> UCCourse
Teacher --> UCEnroll
Teacher --> UCAssignment
Teacher --> UCImport
Teacher --> UCResult
Teacher --> UCRejudge
Teacher --> UCStat

Student --> UCAuth
Student --> UCSubmit
Student --> UCResult
Student --> UCAiDiag
UCSubmit --> UCJudge
@enduml
```

## C.2 系统组件图
```
@startuml
skinparam componentStyle rectangle

package 浏览器端 {
  [index.html] as Html
  [styles.css] as Css
  [app.js 工作台逻辑] as Js
}

package SpringBoot应用 {
  [AuthInterceptor] as Auth
  [Controller层] as Controller
  [Service业务层] as Service
  [Repository数据访问层] as Repo
  [JavaJudgeService] as Judge
  [SubmissionJudgeQueueService] as Queue
  [DeepSeekAiClient] as AiClient
}

database MySQL数据库 as MySQL
database H2演示数据库 as H2
cloud AI服务 as AI

Html --> Js
Js --> Controller : REST + Bearer Token
Controller --> Auth
Controller --> Service
Service --> Repo
Repo --> MySQL
Repo --> H2
Service --> Queue
Queue --> Judge
Service --> AiClient
AiClient --> AI
@enduml
```

## C.3 核心实体关系图
```
@startuml
class User {
  id
  username
  password
  fullName
  className
  role
  active
}

class Course {
  id
  code
  name
  term
  className
  active
}

class CourseEnrollment {
  id
  enrolledAt
}

class Assignment {
  id
  title
  description
  deadline
  status
  gradingPolicy
  maxSubmissions
  lateSubmissionAllowed
}

class TestCase {
  id
  inputData
  expectedOutput
}

class Submission {
  id
  sourceCode
  className
  status
  score
  compileMessage
  runtimeMessage
  submittedAt
}

class JudgeCaseResult {
  id
  caseOrder
  inputData
  expectedOutput
  actualOutput
  passed
  errorMessage
}

class AuthTokenRecord {
  token
  issuedAt
  expiresAt
}

class AuditLog {
  id
  actorUsername
  action
  targetType
  targetId
  summary
  createdAt
}

class AiSettings {
  id
  enabled
  baseUrl
  apiKey
  model
  timeoutSeconds
}

User "1" -- "0..*" Course : teaches
Course "1" -- "0..*" CourseEnrollment
User "1" -- "0..*" CourseEnrollment : enrolls
Course "0..1" -- "0..*" Assignment
User "1" -- "0..*" Assignment : publishes
Assignment "1" -- "0..*" TestCase
Assignment "1" -- "0..*" Submission
User "1" -- "0..*" Submission : submits
Submission "1" -- "0..*" JudgeCaseResult
User "1" -- "0..*" AuthTokenRecord
@enduml
```

## C.4 学生提交与异步判题时序图
```
@startuml
actor 学生 as Student
participant 前端页面 as FE
participant SubmissionController as SC
participant SubmissionService as SS
participant SubmissionRepository as SR
participant SubmissionJudgeQueueService as Queue
participant JavaJudgeService as Judge
database 数据库 as DB

Student -> FE : 选择作业并提交Java源码
FE -> SC : POST /api/submissions
SC -> SS : createSubmission(currentUser, request)
SS -> SS : 校验学生身份、选课、状态、截止时间、提交次数
SS -> SR : 保存PENDING提交
SR -> DB : insert submissions
SS -> Queue : 事务提交后加入异步判题
SC --> FE : 返回PENDING提交详情
Queue -> SR : 读取提交和测试用例
Queue -> Judge : judge(sourceCode, testCases)
Judge -> Judge : 源码校验、编译、运行、输出比对、评分
Judge --> Queue : JudgeOutcome
Queue -> SR : 更新状态、分数、编译运行信息和用例明细
FE -> SC : 轮询 GET /api/submissions/{id}
SC --> FE : 返回最终评测结果
FE --> Student : 展示得分和用例详情
@enduml
```

## C.5 AI 辅助诊断时序图
```
@startuml
actor 学生 as Student
participant 前端页面 as FE
participant SubmissionController as SC
participant AiDiagnosisService as ADS
participant AiDiagnosisPromptBuilder as Builder
participant DeepSeekAiClient as Client
cloud AI服务 as AI
database 数据库 as DB

Student -> FE : 点击AI辅助分析
FE -> SC : POST /api/submissions/{id}/ai-diagnosis
SC -> ADS : diagnoseSubmission(currentUser, submissionId)
ADS -> DB : 读取提交、作业和评测用例结果
ADS -> ADS : 校验只能分析本人提交且状态非PENDING
ADS -> Builder : 构造中文诊断提示词
Builder --> ADS : prompt
ADS -> Client : diagnose(submissionId, status, prompt)
Client -> AI : /chat/completions
AI --> Client : JSON分析结果
Client --> ADS : AiDiagnosisResponse
ADS -> DB : 写入审计日志
SC --> FE : 返回问题概述、原因、建议和知识点
FE --> Student : 展示AI分析结果和免责声明
@enduml
```

# 附录 D 答辩演示建议
建议按管理员、教师、学生的顺序演示。第一步使用 admin1 登录，展示用户、课程、作业、审计日志和 AI 设置。第二步使用 teacher1 登录，展示 CS101 课程、已选学生 student1、演示作业和测试用例。第三步使用 student1 登录，查看已选课程和已发布作业，提交页面默认的两数求和 Java 代码。第四步等待评测完成，展示 ACCEPTED、100 分和两个用例通过。第五步回到教师端查看提交列表、评测详情、统计和成绩导出。若需要展示异常场景，可将学生代码中的 a + b 修改为 a - b 演示错误答案，或删除分号演示编译错误。
'@

# 表格插入到正文开始之前，以保证文档有正式表格。先添加正文，再在相关章节后补表格会导致定位复杂，
# 因此这里在需求分析前后使用独立表格展示关键内容。
Add-Markdown $body

Add-PageBreak
Add-Paragraph -Text '附表 1 系统关键数据表说明' -Style $wdStyleHeading1 -Size 16 -Bold:$true
Add-Table -Caption '表 1 系统核心数据表' -Headers @('数据表', '主要字段', '作用') -Rows @(
    @('users', 'username、password、role、active', '保存用户、角色和账号状态'),
    @('courses', 'code、name、term、class_name、teacher_id', '保存课程和任课教师'),
    @('course_enrollments', 'course_id、student_id、enrolled_at', '保存课程选课关系'),
    @('assignments', 'title、deadline、status、grading_policy', '保存作业配置'),
    @('test_cases', 'input_data、expected_output、assignment_id', '保存测试用例'),
    @('submissions', 'source_code、status、score、submitted_at', '保存学生提交和评测概要'),
    @('judge_case_results', 'input_data、expected_output、actual_output、passed', '保存每个用例评测明细'),
    @('auth_tokens', 'token、user_id、issued_at、expires_at', '保存登录会话'),
    @('audit_logs', 'actor、action、target、summary', '保存审计日志'),
    @('ai_settings', 'enabled、base_url、api_key、model', '保存 AI 服务配置')
)

Add-Table -Caption '表 2 主要测试类与测试数量' -Headers @('测试类', '测试数量', '覆盖重点') -Rows @(
    @('AdminManagementIntegrationTest', '3', '管理员搜索、权限、AI 设置'),
    @('AdminSearchPaginationIntegrationTest', '2', '管理员分页搜索'),
    @('AiDiagnosisIntegrationTest', '3', 'AI 诊断配置与权限'),
    @('AuthIntegrationTest', '4', '认证、参数校验、角色访问'),
    @('CourseManagementIntegrationTest', '3', '课程更新删除与学生课程可见性'),
    @('AssignmentSubmissionFlowIntegrationTest', '9', '作业提交、评测、重判、迟交、次数限制'),
    @('StaticFrontendStructureTest', '3', '前端结构和 AI 面板位置'),
    @('ImportIntegrationTest', '3', 'CSV 导入和跳过无效行'),
    @('JavaJudgeServiceTest', '5', '编译、运行、超时、输出限制'),
    @('StatisticsExportIntegrationTest', '1', '课程统计与成绩导出'),
    @('UserManagementIntegrationTest', '3', '账号启停和密码重置'),
    @('DataSeeder 相关测试', '2', 'demo 数据与默认 profile 数据隔离')
)

# 更新目录、页眉和页码
$doc.Sections.Item(1).Headers.Item(1).Range.Text = $title
$doc.Sections.Item(1).Headers.Item(1).Range.ParagraphFormat.Alignment = $wdAlignCenter
$doc.Sections.Item(1).Headers.Item(1).Range.Font.Size = 9
$doc.Sections.Item(1).Footers.Item(1).PageNumbers.Add() | Out-Null

try {
    $toc.Update()
    $doc.Fields.Update() | Out-Null
} catch {
    # 目录更新失败不阻断文档生成。
}

$doc.SaveAs2($outputPath, $wdFormatXMLDocument)
$doc.Close($false)
$word.Quit()

[Runtime.InteropServices.Marshal]::ReleaseComObject($selection) | Out-Null
[Runtime.InteropServices.Marshal]::ReleaseComObject($doc) | Out-Null
[Runtime.InteropServices.Marshal]::ReleaseComObject($word) | Out-Null
[GC]::Collect()
[GC]::WaitForPendingFinalizers()

Write-Output $outputPath

