package com.graduation.autograding.service;

import com.graduation.autograding.auth.AuthenticatedUser;
import com.graduation.autograding.domain.Submission;
import com.graduation.autograding.dto.AiDiagnosisResponse;
import com.graduation.autograding.exception.ForbiddenException;
import com.graduation.autograding.exception.NotFoundException;
import com.graduation.autograding.repository.SubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiDiagnosisService {

    private final SubmissionRepository submissionRepository;
    private final AiDiagnosisPromptBuilder promptBuilder;
    private final AiClient aiClient;
    private final AuditLogService auditLogService;

    public AiDiagnosisService(SubmissionRepository submissionRepository,
                              AiDiagnosisPromptBuilder promptBuilder,
                              AiClient aiClient,
                              AuditLogService auditLogService) {
        this.submissionRepository = submissionRepository;
        this.promptBuilder = promptBuilder;
        this.aiClient = aiClient;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public AiDiagnosisResponse diagnoseSubmission(AuthenticatedUser currentUser, Long submissionId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new NotFoundException("提交记录不存在。"));
        ensureSubmissionAccessible(currentUser, submission);
        if ("PENDING".equals(submission.getStatus().name())) {
            throw new IllegalStateException("当前提交仍在评测中，请等待评测完成后再进行 AI 分析。");
        }

        String prompt = promptBuilder.buildPrompt(submission);
        AiDiagnosisResponse response = aiClient.diagnose(submission.getId(), submission.getStatus().name(), prompt);
        auditLogService.record(currentUser, "SUBMISSION_AI_DIAGNOSED", "SUBMISSION",
                String.valueOf(submissionId), "对提交 " + submissionId + " 发起 AI 辅助诊断");
        return response;
    }

    private void ensureSubmissionAccessible(AuthenticatedUser currentUser, Submission submission) {
        if (!currentUser.isStudent()) {
            throw new ForbiddenException("只有学生可以发起 AI 辅助分析。");
        }
        if (currentUser.isStudent() && !submission.getStudent().getId().equals(currentUser.id())) {
            throw new ForbiddenException("学生只能分析自己的提交记录。");
        }
    }
}
