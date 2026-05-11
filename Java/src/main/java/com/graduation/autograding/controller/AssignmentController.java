package com.graduation.autograding.controller;

import com.graduation.autograding.auth.AuthInterceptor;
import com.graduation.autograding.auth.AuthenticatedUser;
import com.graduation.autograding.dto.AssignmentCreateRequest;
import com.graduation.autograding.dto.AssignmentResponse;
import com.graduation.autograding.dto.AssignmentStatisticsResponse;
import com.graduation.autograding.dto.AssignmentUpdateRequest;
import com.graduation.autograding.dto.TestCaseRequest;
import com.graduation.autograding.service.AssignmentService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assignments")
public class AssignmentController {

    private final AssignmentService assignmentService;

    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @PostMapping
    public AssignmentResponse createAssignment(
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthenticatedUser currentUser,
            @Valid @RequestBody AssignmentCreateRequest request) {
        return toResponse(currentUser, assignmentService.createAssignment(currentUser, request));
    }

    @GetMapping
    public List<AssignmentResponse> listAssignments(
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthenticatedUser currentUser) {
        return assignmentService.listAssignments(currentUser).stream()
                .map(assignment -> toResponse(currentUser, assignment))
                .toList();
    }

    @GetMapping("/published")
    public List<AssignmentResponse> listPublishedAssignments(
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthenticatedUser currentUser) {
        return assignmentService.listPublishedAssignments(currentUser).stream()
                .map(assignment -> toResponse(currentUser, assignment))
                .toList();
    }

    @GetMapping("/{assignmentId}")
    public AssignmentResponse getAssignment(
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthenticatedUser currentUser,
            @PathVariable Long assignmentId) {
        return toResponse(currentUser, assignmentService.getAssignment(currentUser, assignmentId));
    }

    @PutMapping("/{assignmentId}")
    public AssignmentResponse updateAssignment(
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthenticatedUser currentUser,
            @PathVariable Long assignmentId,
            @Valid @RequestBody AssignmentUpdateRequest request) {
        return toResponse(currentUser, assignmentService.updateAssignment(currentUser, assignmentId, request));
    }

    @DeleteMapping("/{assignmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAssignment(
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthenticatedUser currentUser,
            @PathVariable Long assignmentId) {
        assignmentService.deleteAssignment(currentUser, assignmentId);
    }

    @PostMapping("/{assignmentId}/test-cases")
    public AssignmentResponse addTestCase(
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthenticatedUser currentUser,
            @PathVariable Long assignmentId,
            @Valid @RequestBody TestCaseRequest request) {
        return toResponse(currentUser, assignmentService.addTestCase(currentUser, assignmentId, request));
    }

    @PutMapping("/{assignmentId}/test-cases/{testCaseId}")
    public AssignmentResponse updateTestCase(
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthenticatedUser currentUser,
            @PathVariable Long assignmentId,
            @PathVariable Long testCaseId,
            @Valid @RequestBody TestCaseRequest request) {
        return toResponse(
                currentUser,
                assignmentService.updateTestCase(currentUser, assignmentId, testCaseId, request)
        );
    }

    @DeleteMapping("/{assignmentId}/test-cases/{testCaseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTestCase(
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthenticatedUser currentUser,
            @PathVariable Long assignmentId,
            @PathVariable Long testCaseId) {
        assignmentService.deleteTestCase(currentUser, assignmentId, testCaseId);
    }

    @GetMapping("/statistics/overview")
    public List<AssignmentStatisticsResponse> listAssignmentStatistics(
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthenticatedUser currentUser) {
        return assignmentService.listAssignmentStatistics(currentUser);
    }

    @GetMapping("/{assignmentId}/grades/export")
    public ResponseEntity<byte[]> exportAssignmentGrades(
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthenticatedUser currentUser,
            @PathVariable Long assignmentId) {
        byte[] content = assignmentService.exportAssignmentGradesCsv(currentUser, assignmentId)
                .getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"assignment-" + assignmentId + "-grades.csv\"")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(content);
    }

    private AssignmentResponse toResponse(AuthenticatedUser currentUser,
                                          com.graduation.autograding.domain.Assignment assignment) {
        return AssignmentResponse.fromEntity(assignment, !currentUser.isStudent());
    }
}
