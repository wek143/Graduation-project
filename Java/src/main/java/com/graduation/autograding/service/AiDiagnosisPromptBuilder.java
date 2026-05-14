package com.graduation.autograding.service;

import com.graduation.autograding.config.AiAssistantProperties;
import com.graduation.autograding.domain.JudgeCaseResult;
import com.graduation.autograding.domain.Submission;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AiDiagnosisPromptBuilder {

    private final AiAssistantProperties properties;

    public AiDiagnosisPromptBuilder(AiAssistantProperties properties) {
        this.properties = properties;
    }

    public String buildPrompt(Submission submission) {
        String sourceCode = submission.getSourceCode() == null ? "" : submission.getSourceCode();
        if (sourceCode.length() > properties.getMaxSourceCodeChars()) {
            sourceCode = sourceCode.substring(0, properties.getMaxSourceCodeChars()) + System.lineSeparator() + "[源码已截断]";
        }

        StringBuilder builder = new StringBuilder();
        builder.append("你是编程作业自动评测系统中的教学辅助诊断助手。").append(System.lineSeparator());
        builder.append("你的任务是帮助学生理解错误，不直接给出完整标准答案。").append(System.lineSeparator());
        builder.append("请使用中文输出，并严格返回 JSON 对象，字段为：summary, possibleCauses, fixSuggestions, knowledgePoints。").append(System.lineSeparator());
        builder.append("要求：possibleCauses、fixSuggestions、knowledgePoints 都是字符串数组，每个数组返回 2 到 4 条。").append(System.lineSeparator());
        builder.append("避免输出 markdown 代码块，避免输出完整可直接提交的最终代码。").append(System.lineSeparator());
        builder.append(System.lineSeparator());
        builder.append("作业标题：").append(safe(submission.getAssignment().getTitle())).append(System.lineSeparator());
        builder.append("提交状态：").append(submission.getStatus().name()).append(System.lineSeparator());
        builder.append("得分：").append(submission.getScore() == null ? 0 : submission.getScore()).append(System.lineSeparator());
        builder.append("编译信息：").append(safe(submission.getCompileMessage())).append(System.lineSeparator());
        builder.append("运行信息：").append(safe(submission.getRuntimeMessage())).append(System.lineSeparator());
        builder.append("测试用例结果：").append(System.lineSeparator());

        List<JudgeCaseResult> caseResults = submission.getCaseResults();
        if (caseResults == null || caseResults.isEmpty()) {
            builder.append("- 当前没有测试用例结果").append(System.lineSeparator());
        } else {
            for (JudgeCaseResult result : caseResults) {
                builder.append("- 用例 ")
                        .append(result.getCaseOrder())
                        .append("，通过：")
                        .append(result.isPassed() ? "是" : "否")
                        .append("，输入：").append(safe(result.getInputData()))
                        .append("，期望：").append(safe(result.getExpectedOutput()))
                        .append("，实际：").append(safe(result.getActualOutput()))
                        .append("，错误：").append(safe(result.getErrorMessage()))
                        .append(System.lineSeparator());
            }
        }

        builder.append(System.lineSeparator());
        builder.append("学生源码：").append(System.lineSeparator());
        builder.append(sourceCode).append(System.lineSeparator());
        return builder.toString();
    }

    private String safe(String value) {
        if (value == null || value.isBlank()) {
            return "无";
        }
        return value.replace(System.lineSeparator(), " ").trim();
    }
}
