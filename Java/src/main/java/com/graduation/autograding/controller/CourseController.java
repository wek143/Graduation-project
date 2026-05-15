package com.graduation.autograding.controller;

import com.graduation.autograding.auth.AuthInterceptor;
import com.graduation.autograding.auth.AuthenticatedUser;
import com.graduation.autograding.dto.CourseEnrollmentResponse;
import com.graduation.autograding.dto.CourseResponse;
import com.graduation.autograding.dto.CourseStatisticsResponse;
import com.graduation.autograding.service.CourseService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    public List<CourseResponse> listAccessibleCourses(
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthenticatedUser currentUser) {
        return courseService.listAccessibleCourses(currentUser).stream()
                .map(CourseResponse::fromEntity)
                .toList();
    }

    @GetMapping("/available")
    public List<CourseResponse> listAvailableCourses(
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthenticatedUser currentUser,
            @RequestParam(required = false) String term) {
        return courseService.listAvailableCourses(term).stream()
                .map(CourseResponse::fromEntity)
                .toList();
    }

    @GetMapping("/statistics/overview")
    public List<CourseStatisticsResponse> listCourseStatistics(
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthenticatedUser currentUser) {
        return courseService.listCourseStatistics(currentUser);
    }

    @PostMapping("/{courseId}/join")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void joinCourse(
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthenticatedUser currentUser,
            @PathVariable Long courseId) {
        courseService.joinCourse(currentUser, courseId);
    }

    @DeleteMapping("/{courseId}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leaveCourse(
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthenticatedUser currentUser,
            @PathVariable Long courseId) {
        courseService.leaveCourse(currentUser, courseId);
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
