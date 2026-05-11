package com.graduation.autograding.service;

import com.graduation.autograding.auth.AuthSession;
import com.graduation.autograding.auth.AuthTokenService;
import com.graduation.autograding.auth.AuthenticatedUser;
import com.graduation.autograding.auth.PasswordService;
import com.graduation.autograding.domain.AssignmentStatus;
import com.graduation.autograding.domain.User;
import com.graduation.autograding.domain.UserRole;
import com.graduation.autograding.dto.AuthRequest;
import com.graduation.autograding.dto.RegisterRequest;
import com.graduation.autograding.dto.SystemOverviewResponse;
import com.graduation.autograding.exception.NotFoundException;
import com.graduation.autograding.repository.AssignmentRepository;
import com.graduation.autograding.repository.SubmissionRepository;
import com.graduation.autograding.repository.UserRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final PasswordService passwordService;
    private final AuthTokenService authTokenService;
    private final AuditLogService auditLogService;

    public UserService(UserRepository userRepository,
                       AssignmentRepository assignmentRepository,
                       SubmissionRepository submissionRepository,
                       PasswordService passwordService,
                       AuthTokenService authTokenService,
                       AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
        this.passwordService = passwordService;
        this.authTokenService = authTokenService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public AuthSession register(RegisterRequest request) {
        String username = normalize(request.username());
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("用户名已存在。");
        }
        UserRole role = parseRole(request.role());
        User user = userRepository.save(new User(
                username,
                passwordService.encode(request.password()),
                role,
                normalizeNullable(request.fullName()),
                normalizeNullable(request.className())
        ));
        auditLogService.record(AuthenticatedUser.fromEntity(user), "USER_REGISTERED", "USER",
                String.valueOf(user.getId()), "注册账号：" + user.getUsername());
        return authTokenService.issueToken(user);
    }

    @Transactional
    public AuthSession login(AuthRequest request) {
        User user = userRepository.findByUsername(normalize(request.username()))
                .orElseThrow(() -> new NotFoundException("用户不存在。"));
        if (!user.isActive()) {
            throw new IllegalStateException("账号已被禁用，请联系管理员。");
        }
        if (!passwordService.matches(request.password(), user.getPassword())) {
            throw new IllegalArgumentException("密码错误。");
        }
        if (passwordService.needsUpgrade(user.getPassword())) {
            user.setPassword(passwordService.encode(request.password()));
            user = userRepository.save(user);
        }
        auditLogService.record(AuthenticatedUser.fromEntity(user), "USER_LOGGED_IN", "USER",
                String.valueOf(user.getId()), "用户登录：" + user.getUsername());
        return authTokenService.issueToken(user);
    }

    @Transactional(readOnly = true)
    public List<User> listUsers(AuthenticatedUser currentUser) {
        ensureTeacherOrAdmin(currentUser);
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<User> listUsersByRole(AuthenticatedUser currentUser, String role) {
        ensureTeacherOrAdmin(currentUser);
        return userRepository.findByRoleOrderByIdAsc(parseRole(role));
    }

    @Transactional(readOnly = true)
    public Page<User> searchUsersForAdmin(AuthenticatedUser currentUser, String keyword, String role, Pageable pageable) {
        ServiceHelper.ensureAdmin(currentUser);
        return userRepository.searchForAdmin(ServiceHelper.normalizeBlankToNull(keyword), parseOptionalRole(role), pageable);
    }

    @Transactional(readOnly = true)
    public SystemOverviewResponse getSystemOverview(AuthenticatedUser currentUser) {
        ensureTeacherOrAdmin(currentUser);
        return new SystemOverviewResponse(
                userRepository.countByRole(UserRole.TEACHER),
                userRepository.countByRole(UserRole.STUDENT),
                assignmentRepository.count(),
                assignmentRepository.countByStatus(AssignmentStatus.PUBLISHED),
                submissionRepository.count()
        );
    }

    @Transactional(readOnly = true)
    public User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("用户不存在。"));
    }

    @Transactional
    public User updateUserActiveStatus(AuthenticatedUser currentUser, Long userId, boolean active) {
        ensureAdmin(currentUser);
        User user = getUser(userId);
        if (!active && currentUser.id().equals(userId)) {
            throw new IllegalArgumentException("不能禁用当前管理员账号。");
        }
        user.setActive(active);
        User savedUser = userRepository.save(user);
        if (!active) {
            authTokenService.invalidateAllForUser(userId);
        }
        auditLogService.record(currentUser, active ? "USER_ENABLED" : "USER_DISABLED", "USER", String.valueOf(userId),
                (active ? "启用用户：" : "禁用用户：") + savedUser.getUsername());
        return savedUser;
    }

    @Transactional
    public User resetPassword(AuthenticatedUser currentUser, Long userId, String newPassword) {
        ensureAdmin(currentUser);
        User user = getUser(userId);
        user.setPassword(passwordService.encode(newPassword));
        User savedUser = userRepository.save(user);
        authTokenService.invalidateAllForUser(userId);
        auditLogService.record(currentUser, "USER_PASSWORD_RESET", "USER", String.valueOf(userId),
                "重置密码：" + savedUser.getUsername());
        return savedUser;
    }

    private UserRole parseRole(String role) {
        try {
            return UserRole.valueOf(normalize(role).toUpperCase(Locale.ROOT));
        } catch (Exception exception) {
            throw new IllegalArgumentException("角色必须是 ADMIN、TEACHER 或 STUDENT。");
        }
    }

    private void ensureTeacherOrAdmin(AuthenticatedUser currentUser) {
        ServiceHelper.ensureTeacher(currentUser);
    }

    private void ensureAdmin(AuthenticatedUser currentUser) {
        ServiceHelper.ensureAdmin(currentUser);
    }

    private String normalize(String value) {
        return ServiceHelper.normalize(value);
    }

    private String normalizeNullable(String value) {
        return ServiceHelper.normalizeBlankToNull(value);
    }

    private UserRole parseOptionalRole(String role) {
        String normalizedRole = ServiceHelper.normalizeBlankToNull(role);
        return normalizedRole == null ? null : parseRole(normalizedRole);
    }

    private String normalizeBlankToNull(String value) {
        return ServiceHelper.normalizeBlankToNull(value);
    }
}
