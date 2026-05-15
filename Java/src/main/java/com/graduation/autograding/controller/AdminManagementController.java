package com.graduation.autograding.controller;

import com.graduation.autograding.auth.AuthInterceptor;
import com.graduation.autograding.auth.AuthenticatedUser;
import com.graduation.autograding.dto.AdminAssignmentSummaryResponse;
import com.graduation.autograding.dto.AdminCourseCreateRequest;
import com.graduation.autograding.dto.AdminCourseUpdateRequest;
import com.graduation.autograding.dto.AiSettingsRequest;
import com.graduation.autograding.dto.AiSettingsResponse;
import com.graduation.autograding.dto.AuditLogResponse;
import com.graduation.autograding.dto.CourseResponse;
import com.graduation.autograding.dto.PageResponse;
import com.graduation.autograding.dto.UserResponse;
import com.graduation.autograding.service.AiSettingsService;
import com.graduation.autograding.service.AssignmentService;
import com.graduation.autograding.service.AuditLogService;
import com.graduation.autograding.service.CourseService;
import com.graduation.autograding.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminManagementController {

    private static final int DEFAULT_PAGE_SIZE = 6;
    private static final int MAX_PAGE_SIZE = 20;

    private final UserService userService;
    private final CourseService courseService;
    private final AssignmentService assignmentService;
    private final AuditLogService auditLogService;
    private final AiSettingsService aiSettingsService;

    public AdminManagementController(UserService userService,
                                     CourseService courseService,
                                     AssignmentService assignmentService,
                                     AuditLogService auditLogService,
                                     AiSettingsService aiSettingsService) {
        this.userService = userService;
        this.courseService = courseService;
        this.assignmentService = assignmentService;
        this.auditLogService = auditLogService;
        this.aiSettingsService = aiSettingsService;
    }

    @GetMapping("/users")
    public PageResponse<UserResponse> listUsers(
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthenticatedUser currentUser,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size) {
        return PageResponse.fromPage(
                userService.searchUsersForAdmin(currentUser, keyword, role, buildPageable(page, size, Sort.by("id").ascending())),
                UserResponse::fromEntity
        );
    }

    @GetMapping("/courses")
    public PageResponse<CourseResponse> listCourses(
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthenticatedUser currentUser,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size) {
        return PageResponse.fromPage(
                courseService.searchCoursesForAdmin(currentUser, keyword, buildPageable(page, size, Sort.by("id").ascending())),
                CourseResponse::fromEntity
        );
    }

    @GetMapping("/assignments")
    public PageResponse<AdminAssignmentSummaryResponse> listAssignments(
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthenticatedUser currentUser,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size) {
        return PageResponse.fromPage(
                assignmentService.searchAssignmentsForAdmin(
                        currentUser,
                        keyword,
                        buildPageable(page, size, Sort.by("deadline").ascending().and(Sort.by("id").ascending()))
                ),
                AdminAssignmentSummaryResponse::fromEntity
        );
    }

    @GetMapping("/audit-logs")
    public PageResponse<AuditLogResponse> listAuditLogs(
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthenticatedUser currentUser,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size) {
        return PageResponse.fromPage(
                auditLogService.searchForAdmin(
                        currentUser,
                        keyword,
                        buildPageable(page, size, Sort.by("createdAt").descending().and(Sort.by("id").descending()))
                ),
                AuditLogResponse::fromEntity
        );
    }

    @GetMapping("/ai-settings")
    public AiSettingsResponse getAiSettings(
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthenticatedUser currentUser) {
        return aiSettingsService.getSettings(currentUser);
    }

    @PostMapping("/ai-settings")
    public AiSettingsResponse updateAiSettings(
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthenticatedUser currentUser,
            @RequestBody AiSettingsRequest request) {
        return aiSettingsService.updateSettings(currentUser, request);
    }

    @PostMapping("/courses")
    @ResponseStatus(HttpStatus.CREATED)
    public CourseResponse createCourse(
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthenticatedUser currentUser,
            @Valid @RequestBody AdminCourseCreateRequest request) {
        return CourseResponse.fromEntity(courseService.createCourseByAdmin(currentUser, request));
    }

    @PutMapping("/courses/{courseId}")
    public CourseResponse updateCourse(
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthenticatedUser currentUser,
            @PathVariable Long courseId,
            @Valid @RequestBody AdminCourseUpdateRequest request) {
        return CourseResponse.fromEntity(courseService.updateCourseByAdmin(currentUser, courseId, request));
    }

    @DeleteMapping("/courses/{courseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCourse(
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthenticatedUser currentUser,
            @PathVariable Long courseId) {
        courseService.deleteCourseByAdmin(currentUser, courseId);
    }

    private Pageable buildPageable(int page, int size, Sort sort) {
        int resolvedPage = Math.max(page, 0);
        int resolvedSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        return PageRequest.of(resolvedPage, resolvedSize, sort);
    }
}
