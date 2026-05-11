package com.graduation.autograding.controller;

import com.graduation.autograding.auth.AuthInterceptor;
import com.graduation.autograding.auth.AuthenticatedUser;
import com.graduation.autograding.dto.CsvImportRequest;
import com.graduation.autograding.dto.ImportResultResponse;
import com.graduation.autograding.service.ImportService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/import")
public class ImportController {

    private final ImportService importService;

    public ImportController(ImportService importService) {
        this.importService = importService;
    }

    @PostMapping("/users")
    public ImportResultResponse importUsers(
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthenticatedUser currentUser,
            @Valid @RequestBody CsvImportRequest request) {
        return importService.importUsers(currentUser, request.csvContent());
    }

    @PostMapping("/courses")
    public ImportResultResponse importCourses(
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthenticatedUser currentUser,
            @Valid @RequestBody CsvImportRequest request) {
        return importService.importCourses(currentUser, request.csvContent());
    }

    @PostMapping("/enrollments")
    public ImportResultResponse importEnrollments(
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthenticatedUser currentUser,
            @Valid @RequestBody CsvImportRequest request) {
        return importService.importEnrollments(currentUser, request.csvContent());
    }
}
