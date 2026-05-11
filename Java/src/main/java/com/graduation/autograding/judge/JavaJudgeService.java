package com.graduation.autograding.judge;

import com.graduation.autograding.domain.JudgeCaseResult;
import com.graduation.autograding.domain.SubmissionStatus;
import com.graduation.autograding.domain.TestCase;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JavaJudgeService {

    private static final String COMPILE_SUCCESS_MESSAGE = "编译成功";
    private static final String SKIPPED_EXECUTION_MESSAGE = "已跳过执行阶段。";
    private static final String OUTPUT_TRUNCATED_MARKER = System.lineSeparator() + "[输出已截断]";
    private static final Pattern PACKAGE_DECLARATION_PATTERN =
            Pattern.compile("(?m)^\\s*package\\s+[A-Za-z_$][A-Za-z\\d_$.]*\\s*;");
    private static final Pattern PUBLIC_CLASS_PATTERN =
            Pattern.compile("public\\s+class\\s+([A-Za-z_$][A-Za-z\\d_$]*)");
    private static final Pattern CLASS_PATTERN =
            Pattern.compile("class\\s+([A-Za-z_$][A-Za-z\\d_$]*)");
    private static final List<Pattern> FORBIDDEN_SOURCE_PATTERNS = List.of(
            Pattern.compile("\\bProcessBuilder\\b"),
            Pattern.compile("\\bRuntime\\s*\\.\\s*getRuntime\\s*\\("),
            Pattern.compile("\\bSystem\\s*\\.\\s*exit\\s*\\("),
            Pattern.compile("\\bjava\\.io\\.File\\b"),
            Pattern.compile("\\bjava\\.nio\\.file\\b"),
            Pattern.compile("\\bFiles\\s*\\."),
            Pattern.compile("\\bPaths\\s*\\."),
            Pattern.compile("\\bSocket\\b"),
            Pattern.compile("\\bServerSocket\\b"),
            Pattern.compile("\\bURL\\b"),
            Pattern.compile("\\bURI\\b"),
            Pattern.compile("\\bClassLoader\\b"),
            Pattern.compile("\\bClass\\s*\\.\\s*forName\\s*\\("),
            Pattern.compile("\\bjava\\.lang\\.reflect\\b"),
            Pattern.compile("\\bMethodHandles\\b"),
            Pattern.compile("\\bUnsafe\\b"),
            Pattern.compile("\\bSystem\\s*\\.\\s*getenv\\s*\\("),
            Pattern.compile("\\bSystem\\s*\\.\\s*getProperty\\s*\\("),
            Pattern.compile("\\bSystem\\s*\\.\\s*getProperties\\s*\\("),
            Pattern.compile("\\bloadLibrary\\s*\\("),
            Pattern.compile("\\bnative\\b"),
            Pattern.compile("\\bThread\\b"),
            Pattern.compile("\\bExecutors\\b")
    );

    private final Duration executionTimeout;
    private final int maxSourceLengthChars;
    private final int maxOutputChars;

    public JavaJudgeService(@Value("${grading.execution-timeout-seconds:5}") long executionTimeoutSeconds,
                            @Value("${grading.max-source-length-chars:30000}") int maxSourceLengthChars,
                            @Value("${grading.max-output-chars:12000}") int maxOutputChars) {
        this.executionTimeout = Duration.ofSeconds(executionTimeoutSeconds);
        this.maxSourceLengthChars = maxSourceLengthChars;
        this.maxOutputChars = maxOutputChars;
    }

    public String detectClassName(String sourceCode) {
        Matcher publicMatcher = PUBLIC_CLASS_PATTERN.matcher(sourceCode);
        if (publicMatcher.find()) {
            return publicMatcher.group(1);
        }
        Matcher classMatcher = CLASS_PATTERN.matcher(sourceCode);
        if (classMatcher.find()) {
            return classMatcher.group(1);
        }
        return "Main";
    }

    public JudgeOutcome judge(String sourceCode, List<TestCase> testCases) {
        String className = detectClassName(sourceCode);
        String sourceValidationMessage = validateSourceCode(sourceCode);
        if (sourceValidationMessage != null) {
            return new JudgeOutcome(
                    className,
                    SubmissionStatus.COMPILE_ERROR,
                    0,
                    sourceValidationMessage,
                    SKIPPED_EXECUTION_MESSAGE,
                    List.of()
            );
        }
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            return new JudgeOutcome(
                    className,
                    SubmissionStatus.COMPILE_ERROR,
                    0,
                    "当前运行环境不可用 Java 编译器。",
                    SKIPPED_EXECUTION_MESSAGE,
                    List.of()
            );
        }

        Path workingDirectory = null;
        try {
            workingDirectory = Files.createTempDirectory("judge-");
            Path sourceFile = workingDirectory.resolve(className + ".java");
            Files.writeString(sourceFile, sourceCode, StandardCharsets.UTF_8);

            String compileMessage = compileSource(compiler, sourceFile, workingDirectory);
            if (!COMPILE_SUCCESS_MESSAGE.equals(compileMessage)) {
                return new JudgeOutcome(
                        className,
                        SubmissionStatus.COMPILE_ERROR,
                        0,
                        compileMessage,
                        SKIPPED_EXECUTION_MESSAGE,
                        List.of()
                );
            }

            if (testCases == null || testCases.isEmpty()) {
                return new JudgeOutcome(
                        className,
                        SubmissionStatus.FAILED,
                        0,
                        compileMessage,
                        "当前作业尚未配置测试用例。",
                        List.of()
                );
            }

            List<JudgeCaseResult> caseResults = new ArrayList<>();
            int passedCount = 0;
            boolean timedOut = false;
            boolean runtimeError = false;
            String runtimeMessage = "所有测试用例执行完毕。";
            // 记录第一次出现致命错误的位置，后续测试用例直接跳过执行
            boolean fatalErrorOccurred = false;

            for (int index = 0; index < testCases.size(); index++) {
                TestCase testCase = testCases.get(index);
                boolean passed = false;
                String errorMessage = null;
                String actualOutput = "";

                if (fatalErrorOccurred) {
                    // 前序测试用例已出现超时或运行时错误，跳过本用例的实际执行
                    errorMessage = "已跳过：前序测试用例出现致命错误。";
                    caseResults.add(new JudgeCaseResult(
                            index + 1,
                            testCase.getInputData(),
                            testCase.getExpectedOutput(),
                            actualOutput,
                            false,
                            errorMessage
                    ));
                    continue;
                }

                RunResult runResult = runClass(workingDirectory, className, testCase.getInputData());
                actualOutput = runResult.stdout();

                if (runResult.timedOut()) {
                    timedOut = true;
                    fatalErrorOccurred = true;
                    errorMessage = "程序执行超时，超过 " + executionTimeout.toSeconds() + " 秒。";
                    runtimeMessage = errorMessage;
                } else if (runResult.outputLimitExceeded()) {
                    runtimeError = true;
                    fatalErrorOccurred = true;
                    errorMessage = "程序输出超出限制，超过 " + maxOutputChars + " 个字符。";
                    runtimeMessage = errorMessage;
                } else if (runResult.exitCode() != 0) {
                    runtimeError = true;
                    fatalErrorOccurred = true;
                    errorMessage = runResult.stderr().isBlank() ? "程序运行失败。" : runResult.stderr();
                    runtimeMessage = errorMessage;
                } else {
                    passed = normalize(runResult.stdout()).equals(normalize(testCase.getExpectedOutput()));
                }

                if (passed) {
                    passedCount++;
                }

                caseResults.add(new JudgeCaseResult(
                        index + 1,
                        testCase.getInputData(),
                        testCase.getExpectedOutput(),
                        actualOutput,
                        passed,
                        errorMessage
                ));
            }

            int score = Math.round(passedCount * 100.0f / testCases.size());
            SubmissionStatus status;
            if (passedCount == testCases.size()) {
                status = SubmissionStatus.ACCEPTED;
            } else if (timedOut) {
                status = SubmissionStatus.TIME_LIMIT_EXCEEDED;
            } else if (runtimeError && passedCount == 0) {
                status = SubmissionStatus.RUNTIME_ERROR;
            } else if (passedCount > 0) {
                status = SubmissionStatus.PARTIAL_ACCEPTED;
            } else {
                status = SubmissionStatus.FAILED;
            }

            return new JudgeOutcome(className, status, score, compileMessage, runtimeMessage, caseResults);
        } catch (IOException exception) {
            return new JudgeOutcome(
                    className,
                    SubmissionStatus.RUNTIME_ERROR,
                    0,
                    COMPILE_SUCCESS_MESSAGE,
                    "判题引擎异常：" + exception.getMessage(),
                    List.of()
            );
        } finally {
            deleteDirectoryQuietly(workingDirectory);
        }
    }

    private String compileSource(JavaCompiler compiler, Path sourceFile, Path outputDirectory) throws IOException {
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager fileManager =
                     compiler.getStandardFileManager(diagnostics, Locale.CHINA, StandardCharsets.UTF_8)) {
            Iterable<? extends JavaFileObject> compilationUnits =
                    fileManager.getJavaFileObjects(sourceFile.toFile());
            List<String> options = List.of("-encoding", "UTF-8", "-d", outputDirectory.toString());
            boolean success = Boolean.TRUE.equals(
                    compiler.getTask(null, fileManager, diagnostics, options, null, compilationUnits).call()
            );
            if (success) {
                return COMPILE_SUCCESS_MESSAGE;
            }
            return diagnostics.getDiagnostics().stream()
                    .map(this::formatDiagnostic)
                    .collect(Collectors.joining(System.lineSeparator()));
        }
    }

    private String formatDiagnostic(Diagnostic<? extends JavaFileObject> diagnostic) {
        return "第 " + diagnostic.getLineNumber() + " 行：" + diagnostic.getMessage(Locale.CHINA);
    }

    private RunResult runClass(Path workingDirectory, String className, String inputData) throws IOException {
        Path isolatedTempDirectory = Files.createTempDirectory(workingDirectory, "runtime-tmp-");
        ProcessBuilder builder = new ProcessBuilder(
                resolveJavaCommand(),
                "-Xms32m",
                "-Xmx128m",
                "-XX:+UseSerialGC",
                "-XX:+DisableAttachMechanism",
                "-Djava.awt.headless=true",
                "-Dfile.encoding=UTF-8",
                "-Duser.home=" + workingDirectory,
                "-Djava.io.tmpdir=" + isolatedTempDirectory,
                "-cp",
                workingDirectory.toString(),
                className
        );
        builder.directory(workingDirectory.toFile());
        builder.environment().clear();

        Process process = builder.start();
        CompletableFuture<LimitedText> stdoutFuture =
                CompletableFuture.supplyAsync(() -> readText(process.getInputStream(), maxOutputChars));
        CompletableFuture<LimitedText> stderrFuture =
                CompletableFuture.supplyAsync(() -> readText(process.getErrorStream(), maxOutputChars));

        try (OutputStreamWriter writer =
                     new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8)) {
            if (inputData != null) {
                writer.write(inputData);
            }
        }

        boolean finished;
        try {
            finished = process.waitFor(executionTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("判题线程被中断。", exception);
        }

        if (!finished) {
            process.destroyForcibly();
            LimitedText stdout = stdoutFuture.join();
            LimitedText stderr = stderrFuture.join();
            return new RunResult(stdout.text(), stderr.text(), true, -1, stdout.exceeded() || stderr.exceeded());
        }

        LimitedText stdout = stdoutFuture.join();
        LimitedText stderr = stderrFuture.join();
        return new RunResult(
                stdout.text(),
                stderr.text(),
                false,
                process.exitValue(),
                stdout.exceeded() || stderr.exceeded()
        );
    }

    private String validateSourceCode(String sourceCode) {
        if (sourceCode == null || sourceCode.isBlank()) {
            return "源代码不能为空。";
        }
        if (sourceCode.length() > maxSourceLengthChars) {
            return "源代码长度超出限制，最多允许 " + maxSourceLengthChars + " 个字符。";
        }
        if (PACKAGE_DECLARATION_PATTERN.matcher(sourceCode).find()) {
            return "提交代码不允许声明 package。";
        }
        for (Pattern pattern : FORBIDDEN_SOURCE_PATTERNS) {
            if (pattern.matcher(sourceCode).find()) {
                return "提交的代码包含判题环境中不允许使用的 API。";
            }
        }
        return null;
    }

    private String resolveJavaCommand() {
        return Path.of(System.getProperty("java.home"), "bin", "java").toString();
    }

    private LimitedText readText(InputStream stream, int limit) {
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            StringBuilder builder = new StringBuilder(Math.min(limit, 1024));
            char[] buffer = new char[1024];
            boolean exceeded = false;
            int read;
            while ((read = reader.read(buffer)) != -1) {
                if (builder.length() < limit) {
                    int appendLength = Math.min(read, limit - builder.length());
                    builder.append(buffer, 0, appendLength);
                    if (appendLength < read) {
                        exceeded = true;
                    }
                } else {
                    exceeded = true;
                }
            }
            if (exceeded) {
                builder.append(OUTPUT_TRUNCATED_MARKER);
            }
            return new LimitedText(builder.toString(), exceeded);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private String normalize(String text) {
        return text == null ? "" : text.replace("\r\n", "\n").strip();
    }

    private void deleteDirectoryQuietly(Path path) {
        if (path == null || Files.notExists(path)) {
            return;
        }
        try (var walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder()).forEach(file -> {
                try {
                    Files.deleteIfExists(file);
                } catch (IOException ignored) {
                }
            });
        } catch (IOException ignored) {
        }
    }

    private record LimitedText(String text, boolean exceeded) {
    }

    private record RunResult(String stdout, String stderr, boolean timedOut, int exitCode, boolean outputLimitExceeded) {
    }
}
