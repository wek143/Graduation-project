package com.graduation.autograding.controller;

import com.graduation.autograding.auth.AuthInterceptor;
import com.graduation.autograding.auth.AuthenticatedUser;
import com.graduation.autograding.dto.CourseCreateRequest;
import com.graduation.autograding.dto.CourseEnrollmentResponse;
import com.graduation.autograding.dto.CourseResponse;
import com.graduation.autograding.dto.CourseStatisticsResponse;
import com.graduation.autograding.dto.CourseUpdateRequest;
import com.graduation.autograding.service.CourseService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    public List<CourseResponse> listTeacherCourses(
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthenticatedUser currentUser) {
        return courseService.listAccessibleCourses(currentUser).stream()
                .map(CourseResponse::fromEntity)
                .toList();
    }

    @GetMapping("/statistics/overview")
    public List<CourseStatisticsResponse> listCourseStatistics(
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthenticatedUser currentUser) {
        return courseService.listCourseStatistics(currentUser);
    }

    @PostMapping
    public CourseResponse createCourse(
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthenticatedUser currentUser,
            @Valid @RequestBody CourseCreateRequest request) {
        return CourseResponse.fromEntity(courseService.createCourse(currentUser, request));
    }

    @PutMapping("/{courseId}")
    public CourseResponse updateCourse(
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthenticatedUser currentUser,
            @PathVariable Long courseId,
            @Valid @RequestBody CourseUpdateRequest request) {
        return CourseResponse.fromEntity(courseService.updateCourse(currentUser, courseId, request));
    }

    @DeleteMapping("/{courseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCourse(
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthenticatedUser currentUser,
            @PathVariable Long courseId) {
        courseService.deleteCourse(currentUser, courseId);
    }

    @PostMapping("/{courseId}/enrollments/{studentId}")
    public void enrollStudent(
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthenticatedUser currentUser,
            @PathVariable Long courseId,
            @PathVariable Long studentId) {
        courseService.enrollStudent(currentUser, courseId, studentId);
    }

    @DeleteMapping("/{courseId}/enrollments/{studentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeEnrollment(
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthenticatedUser currentUser,
            @PathVariable Long courseId,
            @PathVariable Long studentId) {
        courseService.removeStudentEnrollment(currentUser, courseId, studentId);
    }

    @GetMapping("/{courseId}/enrollments")
    public List<CourseEnrollmentResponse> listCourseEnrollments(
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthenticatedUser currentUser,
            @PathVariable Long courseId) {
        return courseService.listCourseEnrollments(currentUser, courseId).stream()
                .map(CourseEnrollmentResponse::fromEntity)
                .toList();
    }
}
