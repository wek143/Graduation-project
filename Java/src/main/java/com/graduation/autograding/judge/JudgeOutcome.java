package com.graduation.autograding.judge;

import com.graduation.autograding.domain.JudgeCaseResult;
import com.graduation.autograding.domain.SubmissionStatus;
import java.util.List;

public record JudgeOutcome(
        String className,
        SubmissionStatus status,
        Integer score,
        String compileMessage,
        String runtimeMessage,
        List<JudgeCaseResult> caseResults
) {
}
