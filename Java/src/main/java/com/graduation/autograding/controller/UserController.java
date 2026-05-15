package com.graduation.autograding.controller;

import com.graduation.autograding.auth.AuthInterceptor;
import com.graduation.autograding.auth.AuthenticatedUser;
import com.graduation.autograding.dto.UpdateProfileRequest;
import com.graduation.autograding.dto.UserPasswordResetRequest;
import com.graduation.autograding.dto.UserStatusUpdateRequest;
import com.graduation.autograding.dto.SystemOverviewResponse;
import com.graduation.autograding.dto.UserResponse;
import com.graduation.autograding.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserResponse> listUsers(
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthenticatedUser currentUser) {
        return userService.listUsers(currentUser).stream()
                .map(UserResponse::fromEntity)
                .toList();
    }

    @GetMapping("/role/{role}")
    public List<UserResponse> listUsersByRole(
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthenticatedUser currentUser,
            @PathVariable String role) {
        return userService.listUsersByRole(currentUser, role).stream()
                .map(UserResponse::fromEntity)
                .toList();
    }

    @GetMapping("/overview")
    public SystemOverviewResponse getOverview(
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthenticatedUser currentUser) {
        return userService.getSystemOverview(currentUser);
    }

    @GetMapping("/me")
    public UserResponse getMyProfile(
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthenticatedUser currentUser) {
        return UserResponse.fromEntity(userService.getMyProfile(currentUser));
    }

    @PutMapping("/me")
    public UserResponse updateMyProfile(
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthenticatedUser currentUser,
            @RequestBody UpdateProfileRequest request) {
        return UserResponse.fromEntity(userService.updateMyProfile(currentUser, request));
    }

    @PutMapping("/{userId}/status")
    public UserResponse updateUserStatus(
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthenticatedUser currentUser,
            @PathVariable Long userId,
            @Valid @RequestBody UserStatusUpdateRequest request) {
        return UserResponse.fromEntity(userService.updateUserActiveStatus(currentUser, userId, request.active()));
    }

    @PostMapping("/{userId}/reset-password")
    public UserResponse resetPassword(
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthenticatedUser currentUser,
            @PathVariable Long userId,
            @Valid @RequestBody UserPasswordResetRequest request) {
        return UserResponse.fromEntity(userService.resetPassword(currentUser, userId, request.newPassword()));
    }
}
