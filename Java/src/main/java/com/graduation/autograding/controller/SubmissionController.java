package com.graduation.autograding.controller;

import com.graduation.autograding.auth.AuthInterceptor;
import com.graduation.autograding.auth.AuthenticatedUser;
import com.graduation.autograding.dto.SubmissionCreateRequest;
import com.graduation.autograding.dto.SubmissionResponse;
import com.graduation.autograding.dto.SubmissionSummaryResponse;
import com.graduation.autograding.dto.AiDiagnosisResponse;
import com.graduation.autograding.service.AiDiagnosisService;
import com.graduation.autograding.service.SubmissionService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {

    private final SubmissionService submissionService;
    private final AiDiagnosisService aiDiagnosisService;

    public SubmissionController(SubmissionService submissionService,
                                AiDiagnosisService aiDiagnosisService) {
        this.submissionService = submissionService;
        this.aiDiagnosisService = aiDiagnosisService;
    }

    @PostMapping
    public SubmissionResponse submit(
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthenticatedUser currentUser,
            @Valid @RequestBody SubmissionCreateRequest request) {
        return SubmissionResponse.fromEntity(submissionService.createSubmission(currentUser, request));
    }

    @GetMapping("/{submissionId}")
    public SubmissionResponse getSubmission(
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthenticatedUser currentUser,
            @PathVariable Long submissionId) {
        return SubmissionResponse.fromEntity(submissionService.getSubmission(currentUser, submissionId));
    }

    @PostMapping("/{submissionId}/rejudge")
    public SubmissionResponse rejudgeSubmission(
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthenticatedUser currentUser,
            @PathVariable Long submissionId) {
        return SubmissionResponse.fromEntity(submissionService.rejudgeSubmission(currentUser, submissionId));
    }

    @PostMapping("/{submissionId}/ai-diagnosis")
    public AiDiagnosisResponse diagnoseSubmission(
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthenticatedUser currentUser,
            @PathVariable Long submissionId) {
        return aiDiagnosisService.diagnoseSubmission(currentUser, submissionId);
    }

    @GetMapping("/student/{studentId}")
    public List<SubmissionResponse> listByStudent(
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthenticatedUser currentUser,
            @PathVariable Long studentId) {
        return submissionService.listByStudent(currentUser, studentId).stream()
                .map(SubmissionResponse::fromEntity)
                .toList();
    }

    @GetMapping("/assignment/{assignmentId}")
    public List<SubmissionResponse> listByAssignment(
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthenticatedUser currentUser,
            @PathVariable Long assignmentId) {
        return submissionService.listByAssignment(currentUser, assignmentId).stream()
                .map(SubmissionResponse::fromEntity)
                .toList();
    }

    @GetMapping("/assignment/{assignmentId}/student/{studentId}/latest")
    public SubmissionResponse getLatestSubmission(
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthenticatedUser currentUser,
            @PathVariable Long assignmentId,
            @PathVariable Long studentId) {
        return SubmissionResponse.fromEntity(
                submissionService.getLatestSubmission(currentUser, assignmentId, studentId)
        );
    }

    @GetMapping("/student/{studentId}/latest")
    public List<SubmissionSummaryResponse> listStudentLatestSummaries(
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthenticatedUser currentUser,
            @PathVariable Long studentId) {
        return submissionService.listStudentLatestSummaries(currentUser, studentId);
    }
}
