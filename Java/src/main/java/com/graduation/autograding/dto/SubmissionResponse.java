package com.graduation.autograding.dto;

import com.graduation.autograding.domain.Submission;
import java.time.LocalDateTime;
import java.util.List;

public record SubmissionResponse(
        Long id,
        Long assignmentId,
        String assignmentTitle,
        Long studentId,
        String studentName,
        String sourceCode,
        String className,
        String status,
        Integer score,
        String compileMessage,
        String runtimeMessage,
        LocalDateTime submittedAt,
        List<JudgeCaseResultResponse> caseResults
) {
    public static SubmissionResponse fromEntity(Submission submission) {
        return new SubmissionResponse(
                submission.getId(),
                submission.getAssignment().getId(),
                submission.getAssignment().getTitle(),
                submission.getStudent().getId(),
                submission.getStudent().getUsername(),
                submission.getSourceCode(),
                submission.getClassName(),
                submission.getStatus().name(),
                submission.getScore(),
                submission.getCompileMessage(),
                submission.getRuntimeMessage(),
                submission.getSubmittedAt(),
                submission.getCaseResults().stream().map(JudgeCaseResultResponse::fromEntity).toList()
        );
    }
}
