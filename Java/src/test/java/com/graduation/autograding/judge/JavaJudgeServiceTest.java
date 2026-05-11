package com.graduation.autograding.judge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.graduation.autograding.domain.SubmissionStatus;
import com.graduation.autograding.domain.TestCase;
import java.util.List;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;

class JavaJudgeServiceTest {

    private final JavaJudgeService judgeService = new JavaJudgeService(5, 30_000, 12_000);

    @Test
    void returnsAcceptedForCorrectSolution() {
        assumeTrue(ToolProvider.getSystemJavaCompiler() != null, "JDK compiler is required for judge tests");

        JudgeOutcome outcome = judgeService.judge(
                """
                public class Main {
                    public static void main(String[] args) {
                        java.util.Scanner scanner = new java.util.Scanner(System.in);
                        int a = scanner.nextInt();
                        int b = scanner.nextInt();
                        System.out.println(a + b);
                    }
                }
                """,
                List.of(
                        new TestCase("1 2", "3"),
                        new TestCase("4 5", "9")
                )
        );

        assertEquals("Main", outcome.className());
        assertEquals(SubmissionStatus.ACCEPTED, outcome.status());
        assertEquals(100, outcome.score());
        assertEquals("编译成功", outcome.compileMessage());
        assertEquals(2, outcome.caseResults().size());
        assertTrue(outcome.caseResults().stream().allMatch(result -> result.isPassed()));
    }

    @Test
    void rejectsPackageDeclaration() {
        JudgeOutcome outcome = judgeService.judge(
                """
                package demo;
                public class Main {
                    public static void main(String[] args) {
                        System.out.println("hello");
                    }
                }
                """,
                List.of(new TestCase("", "hello"))
        );

        assertEquals(SubmissionStatus.COMPILE_ERROR, outcome.status());
        assertTrue(outcome.compileMessage().contains("不允许声明 package"));
    }

    @Test
    void rejectsOversizedSourceCode() {
        String oversizedSource = "public class Main { /*" + "a".repeat(30_001) + "*/ }";

        JudgeOutcome outcome = judgeService.judge(
                oversizedSource,
                List.of(new TestCase("", ""))
        );

        assertEquals(SubmissionStatus.COMPILE_ERROR, outcome.status());
        assertTrue(outcome.compileMessage().contains("源代码长度超出限制"));
    }

    @Test
    void returnsRuntimeErrorWhenOutputExceedsLimit() {
        JavaJudgeService limitedJudgeService = new JavaJudgeService(5, 30_000, 20);

        JudgeOutcome outcome = limitedJudgeService.judge(
                """
                public class Main {
                    public static void main(String[] args) {
                        System.out.println("abcdefghijklmnopqrstuvwxyz");
                    }
                }
                """,
                List.of(new TestCase("", "abcdefghijklmnopqrstuvwxyz"))
        );

        assertEquals(SubmissionStatus.RUNTIME_ERROR, outcome.status());
        assertTrue(outcome.runtimeMessage().contains("程序输出超出限制"));
        assertTrue(outcome.caseResults().get(0).getActualOutput().contains("[输出已截断]"));
    }
    @Test
    void returnsTimeLimitExceededAndSkipsRemainingCases() {
        JudgeOutcome outcome = judgeService.judge(
                """
                public class Main {
                    public static void main(String[] args) {
                        while (true) {
                        }
                    }
                }
                """,
                List.of(
                        new TestCase("", ""),
                        new TestCase("", "")
                )
        );

        assertEquals(SubmissionStatus.TIME_LIMIT_EXCEEDED, outcome.status());
        assertEquals(2, outcome.caseResults().size());
        assertTrue(outcome.caseResults().get(0).getErrorMessage() != null);
        assertTrue(outcome.caseResults().get(1).getErrorMessage() != null);
        assertTrue(!outcome.caseResults().get(1).getErrorMessage().isBlank());
    }
}
