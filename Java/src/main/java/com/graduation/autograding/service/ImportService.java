package com.graduation.autograding.service;

import com.graduation.autograding.auth.AuthenticatedUser;
import com.graduation.autograding.auth.PasswordService;
import com.graduation.autograding.domain.Course;
import com.graduation.autograding.domain.CourseEnrollment;
import com.graduation.autograding.domain.User;
import com.graduation.autograding.domain.UserRole;
import com.graduation.autograding.dto.ImportResultResponse;
import com.graduation.autograding.exception.NotFoundException;
import com.graduation.autograding.repository.CourseEnrollmentRepository;
import com.graduation.autograding.repository.CourseRepository;
import com.graduation.autograding.repository.UserRepository;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ImportService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final PasswordService passwordService;
    private final AuditLogService auditLogService;

    public ImportService(UserRepository userRepository,
                         CourseRepository courseRepository,
                         CourseEnrollmentRepository courseEnrollmentRepository,
                         PasswordService passwordService,
                         AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.courseEnrollmentRepository = courseEnrollmentRepository;
        this.passwordService = passwordService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public ImportResultResponse importUsers(AuthenticatedUser currentUser, String csvContent) {
        ensureTeacher(currentUser);
        List<String> details = new ArrayList<>();
        int created = 0;
        int skipped = 0;
        for (String line : normalizeLines(csvContent)) {
            try {
                String[] columns = splitCsv(line, 5);
                String username = requireValue(columns[0], "用户名");
                String password = requireValue(columns[1], "密码");
                UserRole userRole = parseUserRole(columns[2]);
                String fullName = columns[3];
                String className = columns[4];
                if (userRole == UserRole.ADMIN && !currentUser.isAdmin()) {
                    skipped++;
                    details.add("已跳过：非管理员不允许导入 ADMIN 角色用户：" + username);
                    continue;
                }
                if (userRepository.existsByUsername(username)) {
                    skipped++;
                    details.add("已跳过已存在的用户：" + username);
                    continue;
                }
                userRepository.save(new User(
                        username,
                        passwordService.encode(password),
                        userRole,
                        blankToNull(fullName),
                        blankToNull(className)
                ));
                created++;
            } catch (IllegalArgumentException exception) {
                skipped++;
                details.add("已跳过无效的用户行：" + line + "，" + exception.getMessage());
            }
        }
        auditLogService.record(currentUser, "USERS_IMPORTED", "USER", null,
                "导入用户：成功 " + created + " 条，跳过 " + skipped + " 条");
        return new ImportResultResponse("users", created, skipped, details);
    }

    @Transactional
    public ImportResultResponse importCourses(AuthenticatedUser currentUser, String csvContent) {
        ensureTeacher(currentUser);
        User teacher = userRepository.findById(currentUser.id())
                .orElseThrow(() -> new NotFoundException("教师不存在。"));
        List<String> details = new ArrayList<>();
        int created = 0;
        int skipped = 0;
        for (String line : normalizeLines(csvContent)) {
            try {
                String[] columns = splitCsv(line, 4);
                String code = requireValue(columns[0], "课程代码");
                String name = requireValue(columns[1], "课程名称");
                String term = requireValue(columns[2], "学期");
                String className = requireValue(columns[3], "班级");
                if (courseRepository.findByCode(code).isPresent()) {
                    skipped++;
                    details.add("已跳过已存在的课程：" + code);
                    continue;
                }
                courseRepository.save(new Course(code, name, term, className, teacher));
                created++;
            } catch (IllegalArgumentException exception) {
                skipped++;
                details.add("已跳过无效的课程行：" + line + "，" + exception.getMessage());
            }
        }
        auditLogService.record(currentUser, "COURSES_IMPORTED", "COURSE", null,
                "导入课程：成功 " + created + " 条，跳过 " + skipped + " 条");
        return new ImportResultResponse("courses", created, skipped, details);
    }

    @Transactional
    public ImportResultResponse importEnrollments(AuthenticatedUser currentUser, String csvContent) {
        ensureTeacher(currentUser);
        List<String> details = new ArrayList<>();
        int created = 0;
        int skipped = 0;
        for (String line : normalizeLines(csvContent)) {
            try {
                String[] columns = splitCsv(line, 2);
                String courseCode = requireValue(columns[0], "课程代码");
                String studentUsername = requireValue(columns[1], "学生账号");
                Course course = courseRepository.findByCode(courseCode).orElse(null);
                User student = userRepository.findByUsername(studentUsername).orElse(null);
                if (course == null || student == null || student.getRole() != UserRole.STUDENT) {
                    skipped++;
                    details.add("已跳过无法识别的选课行：" + line);
                    continue;
                }
                if (!currentUser.isAdmin() && !course.getTeacher().getId().equals(currentUser.id())) {
                    skipped++;
                    details.add("已跳过无权限的选课行：" + line);
                    continue;
                }
                if (courseEnrollmentRepository.existsByCourseIdAndStudentId(course.getId(), student.getId())) {
                    skipped++;
                    details.add("已跳过已存在的选课行：" + line);
                    continue;
                }
                courseEnrollmentRepository.save(new CourseEnrollment(course, student));
                created++;
            } catch (IllegalArgumentException exception) {
                skipped++;
                details.add("已跳过无效的选课行：" + line + "，" + exception.getMessage());
            }
        }
        auditLogService.record(currentUser, "ENROLLMENTS_IMPORTED", "COURSE_ENROLLMENT", null,
                "导入选课：成功 " + created + " 条，跳过 " + skipped + " 条");
        return new ImportResultResponse("enrollments", created, skipped, details);
    }

    private List<String> normalizeLines(String csvContent) {
        return csvContent.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .filter(line -> !line.startsWith("#"))
                .filter(line -> !isHeaderLine(line))
                .toList();
    }

    private String[] splitCsv(String line, int... expectedColumns) {
        List<String> columns = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int index = 0; index < line.length(); index++) {
            char currentChar = line.charAt(index);
            if (currentChar == '"') {
                if (inQuotes && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    current.append('"');
                    index++;
                } else {
                    inQuotes = !inQuotes;
                }
                continue;
            }
            if (currentChar == ',' && !inQuotes) {
                columns.add(current.toString().trim());
                current.setLength(0);
                continue;
            }
            current.append(currentChar);
        }
        if (inQuotes) {
            throw new IllegalArgumentException("引号未正确闭合");
        }
        columns.add(current.toString().trim());
        if (Arrays.stream(expectedColumns).noneMatch(expected -> expected == columns.size())) {
            throw new IllegalArgumentException("列数不正确");
        }
        return columns.toArray(String[]::new);
    }

    private String requireValue(String value, String fieldName) {
        String normalized = blankToNull(value);
        if (normalized == null) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
        return normalized;
    }

    private UserRole parseUserRole(String role) {
        String normalizedRole = requireValue(role, "角色");
        try {
            return UserRole.valueOf(normalizedRole.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("角色必须是 ADMIN、TEACHER 或 STUDENT");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private boolean isHeaderLine(String line) {
        String normalized = line.toLowerCase(Locale.ROOT).replace(" ", "");
        return normalized.startsWith("username,")
                || normalized.startsWith("code,")
                || normalized.startsWith("coursecode,");
    }

    private void ensureTeacher(AuthenticatedUser currentUser) {
        ServiceHelper.ensureTeacher(currentUser);
    }
}
