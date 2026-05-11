package com.graduation.autograding.service;

import com.graduation.autograding.auth.AuthenticatedUser;
import com.graduation.autograding.domain.Assignment;
import com.graduation.autograding.domain.AssignmentGradingPolicy;
import com.graduation.autograding.domain.AssignmentStatus;
import com.graduation.autograding.domain.Course;
import com.graduation.autograding.domain.CourseEnrollment;
import com.graduation.autograding.domain.Submission;
import com.graduation.autograding.domain.TestCase;
import com.graduation.autograding.domain.User;
import com.graduation.autograding.domain.UserRole;
import com.graduation.autograding.dto.AssignmentCreateRequest;
import com.graduation.autograding.dto.AssignmentGradeExportRow;
import com.graduation.autograding.dto.AssignmentStatisticsResponse;
import com.graduation.autograding.dto.AssignmentUpdateRequest;
import com.graduation.autograding.dto.TestCaseRequest;
import com.graduation.autograding.exception.ForbiddenException;
import com.graduation.autograding.exception.NotFoundException;
import com.graduation.autograding.repository.AssignmentRepository;
import com.graduation.autograding.repository.CourseEnrollmentRepository;
import com.graduation.autograding.repository.SubmissionRepository;
import com.graduation.autograding.repository.TestCaseRepository;
import com.graduation.autograding.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final UserRepository userRepository;
    private final TestCaseRepository testCaseRepository;
    private final SubmissionRepository submissionRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final CourseService courseService;
    private final AuditLogService auditLogService;

    public AssignmentService(AssignmentRepository assignmentRepository,
                             UserRepository userRepository,
                             TestCaseRepository testCaseRepository,
                             SubmissionRepository submissionRepository,
                             CourseEnrollmentRepository courseEnrollmentRepository,
                             CourseService courseService,
                             AuditLogService auditLogService) {
        this.assignmentRepository = assignmentRepository;
        this.userRepository = userRepository;
        this.testCaseRepository = testCaseRepository;
        this.submissionRepository = submissionRepository;
        this.courseEnrollmentRepository = courseEnrollmentRepository;
        this.courseService = courseService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public Assignment createAssignment(AuthenticatedUser currentUser, AssignmentCreateRequest request) {
        ensureTeacher(currentUser);
        validateDeadline(request.deadline());

        Long teacherId = request.teacherId() == null ? currentUser.id() : request.teacherId();
        if (!currentUser.isAdmin() && !teacherId.equals(currentUser.id())) {
            throw new ForbiddenException("教师只能在自己的账号下创建作业。");
        }

        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new NotFoundException("教师不存在。"));
        if (teacher.getRole() != UserRole.TEACHER && teacher.getRole() != UserRole.ADMIN) {
            throw new IllegalArgumentException("只有教师或管理员可以发布作业。");
        }
        Course course = courseService.resolveAssignmentCourse(currentUser, request.courseId());

        Assignment assignment = new Assignment(
                request.title(),
                request.description(),
                request.deadline(),
                teacher
        );
        assignment.setStatus(parseStatus(request.status(), AssignmentStatus.PUBLISHED));
        assignment.setCourse(course);
        assignment.setMaxSubmissions(resolveMaxSubmissions(request.maxSubmissions()));
        assignment.setLateSubmissionAllowed(Boolean.TRUE.equals(request.lateSubmissionAllowed()));
        assignment.setGradingPolicy(parseGradingPolicy(request.gradingPolicy(), AssignmentGradingPolicy.LATEST));

        if (request.testCases() != null) {
            for (TestCaseRequest testCaseRequest : request.testCases()) {
                assignment.addTestCase(new TestCase(
                        testCaseRequest.inputData(),
                        testCaseRequest.expectedOutput()
                ));
            }
        }

        Assignment savedAssignment = assignmentRepository.save(assignment);
        auditLogService.record(currentUser, "ASSIGNMENT_CREATED", "ASSIGNMENT", String.valueOf(savedAssignment.getId()),
                "创建作业：" + savedAssignment.getTitle());
        return savedAssignment;
    }

    @Transactional(readOnly = true)
    public List<Assignment> listAssignments(AuthenticatedUser currentUser) {
        if (currentUser.isTeacher()) {
            return assignmentRepository.findByTeacherIdOrderByDeadlineAsc(currentUser.id());
        }
        if (currentUser.isAdmin()) {
            return assignmentRepository.findAll();
        }
        return assignmentRepository.findPublishedAssignmentsForStudent(currentUser.id(), AssignmentStatus.PUBLISHED);
    }

    @Transactional(readOnly = true)
    public List<Assignment> listPublishedAssignments(AuthenticatedUser currentUser) {
        if (currentUser.isTeacher()) {
            return assignmentRepository.findByTeacherIdOrderByDeadlineAsc(currentUser.id()).stream()
                    .filter(assignment -> assignment.getStatus() == AssignmentStatus.PUBLISHED)
                    .toList();
        }
        if (currentUser.isAdmin()) {
            return assignmentRepository.findAll().stream()
                    .filter(assignment -> assignment.getStatus() == AssignmentStatus.PUBLISHED)
                    .toList();
        }
        return assignmentRepository.findPublishedAssignmentsForStudent(currentUser.id(), AssignmentStatus.PUBLISHED);
    }

    @Transactional(readOnly = true)
    public Page<Assignment> searchAssignmentsForAdmin(AuthenticatedUser currentUser, String keyword, Pageable pageable) {
        ServiceHelper.ensureAdmin(currentUser);
        return assignmentRepository.searchForAdmin(ServiceHelper.normalizeBlankToNull(keyword), pageable);
    }

    @Transactional(readOnly = true)
    public Assignment getAssignment(AuthenticatedUser currentUser, Long assignmentId) {
        Assignment assignment = loadAssignment(assignmentId);
        if (currentUser.isStudent() && assignment.getStatus() != AssignmentStatus.PUBLISHED) {
            throw new ForbiddenException("学生只能查看已发布的作业。");
        }
        if (currentUser.isStudent() && assignment.getCourse() != null) {
            courseService.ensureStudentEnrolled(assignment.getCourse().getId(), currentUser.id());
        }
        return assignment;
    }

    @Transactional
    public Assignment addTestCase(AuthenticatedUser currentUser, Long assignmentId, TestCaseRequest request) {
        Assignment assignment = requireTeacherOwner(currentUser, assignmentId);
        assignment.addTestCase(new TestCase(request.inputData(), request.expectedOutput()));
        Assignment savedAssignment = assignmentRepository.save(assignment);
        auditLogService.record(currentUser, "TEST_CASE_CREATED", "ASSIGNMENT", String.valueOf(savedAssignment.getId()),
                "添加测试用例至作业：" + savedAssignment.getTitle());
        return savedAssignment;
    }

    @Transactional
    public Assignment updateAssignment(AuthenticatedUser currentUser, Long assignmentId, AssignmentUpdateRequest request) {
        Assignment assignment = requireTeacherOwner(currentUser, assignmentId);
        validateDeadlineForUpdate(assignment, request.deadline());
        assignment.setTitle(request.title());
        assignment.setDescription(request.description());
        assignment.setDeadline(request.deadline());
        assignment.setStatus(parseStatus(request.status(), assignment.getStatus()));
        if (request.courseId() != null) {
            assignment.setCourse(courseService.resolveAssignmentCourse(currentUser, request.courseId()));
        }
        assignment.setMaxSubmissions(resolveMaxSubmissions(
                request.maxSubmissions() == null ? assignment.getMaxSubmissions() : request.maxSubmissions()
        ));
        assignment.setLateSubmissionAllowed(
                request.lateSubmissionAllowed() == null ? assignment.isLateSubmissionAllowed() : request.lateSubmissionAllowed()
        );
        assignment.setGradingPolicy(parseGradingPolicy(request.gradingPolicy(), assignment.getGradingPolicy()));
        Assignment savedAssignment = assignmentRepository.save(assignment);
        auditLogService.record(currentUser, "ASSIGNMENT_UPDATED", "ASSIGNMENT", String.valueOf(savedAssignment.getId()),
                "更新作业：" + savedAssignment.getTitle());
        return savedAssignment;
    }

    @Transactional
    public void deleteAssignment(AuthenticatedUser currentUser, Long assignmentId) {
        Assignment assignment = requireTeacherOwner(currentUser, assignmentId);
        String title = assignment.getTitle();
        assignmentRepository.delete(assignment);
        auditLogService.record(currentUser, "ASSIGNMENT_DELETED", "ASSIGNMENT", String.valueOf(assignmentId),
                "删除作业：" + title);
    }

    @Transactional
    public Assignment updateTestCase(AuthenticatedUser currentUser, Long assignmentId, Long testCaseId,
                                     TestCaseRequest request) {
        Assignment assignment = requireTeacherOwner(currentUser, assignmentId);
        TestCase testCase = testCaseRepository.findById(testCaseId)
                .orElseThrow(() -> new NotFoundException("测试用例不存在。"));
        if (!testCase.getAssignment().getId().equals(assignment.getId())) {
            throw new IllegalArgumentException("测试用例不属于当前作业。");
        }
        testCase.setInputData(request.inputData());
        testCase.setExpectedOutput(request.expectedOutput());
        testCaseRepository.save(testCase);
        auditLogService.record(currentUser, "TEST_CASE_UPDATED", "ASSIGNMENT", String.valueOf(assignment.getId()),
                "更新测试用例，作业：" + assignment.getTitle());
        return assignment;
    }

    @Transactional
    public void deleteTestCase(AuthenticatedUser currentUser, Long assignmentId, Long testCaseId) {
        Assignment assignment = requireTeacherOwner(currentUser, assignmentId);
        TestCase testCase = testCaseRepository.findById(testCaseId)
                .orElseThrow(() -> new NotFoundException("测试用例不存在。"));
        if (!testCase.getAssignment().getId().equals(assignment.getId())) {
            throw new IllegalArgumentException("测试用例不属于当前作业。");
        }
        assignment.removeTestCase(testCase);
        assignmentRepository.save(assignment);
        auditLogService.record(currentUser, "TEST_CASE_DELETED", "ASSIGNMENT", String.valueOf(assignment.getId()),
                "删除测试用例，作业：" + assignment.getTitle());
    }

    @Transactional(readOnly = true)
    public List<AssignmentStatisticsResponse> listAssignmentStatistics(AuthenticatedUser currentUser) {
        ensureTeacher(currentUser);
        List<Assignment> assignments = assignmentRepository.findByTeacherIdOrderByDeadlineAsc(currentUser.id())
                .stream()
                .sorted(Comparator.comparing(Assignment::getId))
                .toList();

        if (assignments.isEmpty()) {
            return List.of();
        }

        List<Long> assignmentIds = assignments.stream().map(Assignment::getId).toList();

        // 一次查出所有作业的提交总数与提交学生数
        Map<Long, long[]> countMap = new HashMap<>();
        for (Object[] row : submissionRepository.findSubmissionCountsByAssignmentIds(assignmentIds)) {
            Long assignmentId = (Long) row[0];
            long total = (long) row[1];
            long distinct = (long) row[2];
            countMap.put(assignmentId, new long[]{total, distinct});
        }

        // 一次查出所有作业的轻量提交投影（assignmentId, studentId, score, submittedAt）
        // 按 submittedAt desc 已排序，用于 LATEST 策略直接取第一条
        Map<Long, List<Object[]>> scoresByAssignment = new HashMap<>();
        for (Object[] row : submissionRepository.findScoreProjectionsByAssignmentIds(assignmentIds)) {
            Long assignmentId = (Long) row[0];
            scoresByAssignment.computeIfAbsent(assignmentId, k -> new java.util.ArrayList<>()).add(row);
        }

        return assignments.stream()
                .map(assignment -> {
                    long[] counts = countMap.getOrDefault(assignment.getId(), new long[]{0L, 0L});
                    long totalSubmissions = counts[0];
                    long distinctStudentCount = counts[1];

                    double averageScore = 0.0;
                    List<Object[]> rows = scoresByAssignment.getOrDefault(assignment.getId(), List.of());
                    if (!rows.isEmpty()) {
                        // 按学生分组，根据评分策略选出有效提交的分数
                        Map<Long, Integer> effectiveScoreByStudent = new java.util.LinkedHashMap<>();
                        for (Object[] row : rows) {
                            Long studentId = (Long) row[1];
                            Integer score = row[2] == null ? 0 : (Integer) row[2];
                            LocalDateTime submittedAt = (LocalDateTime) row[3];
                            if (assignment.getGradingPolicy() == com.graduation.autograding.domain.AssignmentGradingPolicy.HIGHEST) {
                                effectiveScoreByStudent.merge(studentId, score,
                                        (existing, candidate) -> candidate > existing ? candidate : existing);
                            } else {
                                // LATEST：rows 已按 submittedAt desc 排序，第一次出现即为最新
                                effectiveScoreByStudent.putIfAbsent(studentId, score);
                            }
                        }
                        averageScore = effectiveScoreByStudent.values().stream()
                                .mapToInt(Integer::intValue)
                                .average()
                                .orElse(0.0);
                    }

                    return new AssignmentStatisticsResponse(
                            assignment.getId(),
                            assignment.getTitle(),
                            assignment.getStatus().name(),
                            totalSubmissions,
                            distinctStudentCount,
                            averageScore
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public String exportAssignmentGradesCsv(AuthenticatedUser currentUser, Long assignmentId) {
        Assignment assignment = requireTeacherOwner(currentUser, assignmentId);
        List<AssignmentGradeExportRow> rows = buildAssignmentGradeRows(assignment);
        StringBuilder builder = new StringBuilder();
        builder.append('\uFEFF');
        builder.append("用户名,姓名,班级,状态,成绩,提交时间,提交次数,有效提交ID").append(System.lineSeparator());
        rows.forEach(row -> builder.append(toCsvLine(row)).append(System.lineSeparator()));
        return builder.toString();
    }

    private Assignment loadAssignment(Long assignmentId) {
        return assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new NotFoundException("作业不存在。"));
    }

    private List<AssignmentGradeExportRow> buildAssignmentGradeRows(Assignment assignment) {
        List<Submission> submissions = submissionRepository.findByAssignmentIdOrderBySubmittedAtDesc(assignment.getId());
        Map<Long, List<Submission>> submissionsByStudentId = submissions.stream()
                .collect(Collectors.groupingBy(submission -> submission.getStudent().getId()));
        Map<Long, Submission> effectiveSubmissions = submissionsByStudentId.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> selectEffectiveSubmission(assignment.getGradingPolicy(), entry.getValue())
                ));

        List<User> students = assignment.getCourse() == null
                ? submissions.stream()
                        .map(Submission::getStudent)
                        .distinct()
                        .sorted(Comparator.comparing(User::getUsername))
                        .toList()
                : courseEnrollmentRepository.findByCourseIdOrderByEnrolledAtDesc(assignment.getCourse().getId()).stream()
                        .map(CourseEnrollment::getStudent)
                        .sorted(Comparator.comparing(User::getUsername))
                        .toList();

        return students.stream()
                .map(student -> {
                    Submission effectiveSubmission = effectiveSubmissions.get(student.getId());
                    List<Submission> studentSubmissions = submissionsByStudentId.getOrDefault(student.getId(), List.of());
                    return new AssignmentGradeExportRow(
                            student.getUsername(),
                            student.getFullName(),
                            student.getClassName(),
                            effectiveSubmission == null ? "NOT_SUBMITTED" : effectiveSubmission.getStatus().name(),
                            effectiveSubmission == null ? null : effectiveSubmission.getScore(),
                            effectiveSubmission == null ? null : effectiveSubmission.getSubmittedAt(),
                            studentSubmissions.size(),
                            effectiveSubmission == null ? null : effectiveSubmission.getId()
                    );
                })
                .toList();
    }

    private Assignment requireTeacherOwner(AuthenticatedUser currentUser, Long assignmentId) {
        ServiceHelper.ensureTeacher(currentUser);
        Assignment assignment = loadAssignment(assignmentId);
        if (!currentUser.isAdmin() && !assignment.getTeacher().getId().equals(currentUser.id())) {
            throw new ForbiddenException("你只能管理自己创建的作业。");
        }
        return assignment;
    }

    private AssignmentStatus parseStatus(String status, AssignmentStatus defaultStatus) {
        if (status == null || status.isBlank()) {
            return defaultStatus;
        }
        try {
            return AssignmentStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (Exception exception) {
            throw new IllegalArgumentException("作业状态必须是 DRAFT、PUBLISHED 或 CLOSED。");
        }
    }

    private AssignmentGradingPolicy parseGradingPolicy(String policy, AssignmentGradingPolicy defaultPolicy) {
        if (policy == null || policy.isBlank()) {
            return defaultPolicy;
        }
        try {
            return AssignmentGradingPolicy.valueOf(policy.trim().toUpperCase(Locale.ROOT));
        } catch (Exception exception) {
            throw new IllegalArgumentException("评分策略必须是 LATEST 或 HIGHEST。");
        }
    }

    private Integer resolveMaxSubmissions(Integer maxSubmissions) {
        if (maxSubmissions == null) {
            return 5;
        }
        if (maxSubmissions < 1 || maxSubmissions > 100) {
            throw new IllegalArgumentException("最大提交次数必须在 1 到 100 之间。");
        }
        return maxSubmissions;
    }

    private Submission selectEffectiveSubmission(AssignmentGradingPolicy gradingPolicy, Submission first, Submission second) {
        if (gradingPolicy == AssignmentGradingPolicy.HIGHEST) {
            int firstScore = first.getScore() == null ? 0 : first.getScore();
            int secondScore = second.getScore() == null ? 0 : second.getScore();
            if (secondScore > firstScore) {
                return second;
            }
            if (secondScore == firstScore && second.getSubmittedAt().isAfter(first.getSubmittedAt())) {
                return second;
            }
            return first;
        }
        return first.getSubmittedAt().isAfter(second.getSubmittedAt()) ? first : second;
    }

    private Submission selectEffectiveSubmission(AssignmentGradingPolicy gradingPolicy, List<Submission> submissions) {
        return submissions.stream()
                .reduce((first, second) -> selectEffectiveSubmission(gradingPolicy, first, second))
                .orElse(null);
    }

    private String toCsvLine(AssignmentGradeExportRow row) {
        return List.of(
                csvCell(row.username()),
                csvCell(row.fullName()),
                csvCell(row.className()),
                csvCell(translateExportStatus(row.status())),
                csvCell(row.score() == null ? "" : String.valueOf(row.score())),
                csvCell(row.submittedAt() == null ? "" : row.submittedAt().toString()),
                csvCell(String.valueOf(row.submissionCount())),
                csvCell(row.effectiveSubmissionId() == null ? "" : String.valueOf(row.effectiveSubmissionId()))
        ).stream().collect(Collectors.joining(","));
    }

    private String translateExportStatus(String status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case "NOT_SUBMITTED" -> "未提交";
            case "ACCEPTED" -> "通过";
            case "PARTIAL_ACCEPTED" -> "部分通过";
            case "FAILED" -> "未通过";
            case "COMPILE_ERROR" -> "编译错误";
            case "RUNTIME_ERROR" -> "运行错误";
            case "TIME_LIMIT_EXCEEDED" -> "超时";
            case "PENDING" -> "待评测";
            default -> status;
        };
    }

    private String csvCell(String value) {
        String safeValue = value == null ? "" : value;
        return "\"" + safeValue.replace("\"", "\"\"") + "\"";
    }

    private void validateDeadline(LocalDateTime deadline) {
        if (deadline.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("截止时间必须晚于当前时间。");
        }
    }

    private void validateDeadlineForUpdate(Assignment assignment, LocalDateTime deadline) {
        if (deadline == null) {
            throw new IllegalArgumentException("截止时间不能为空。");
        }
        if (deadline.isBefore(LocalDateTime.now()) && !deadline.equals(assignment.getDeadline())) {
            throw new IllegalArgumentException("截止时间必须晚于当前时间。");
        }
    }

    private void ensureTeacher(AuthenticatedUser currentUser) {
        ServiceHelper.ensureTeacher(currentUser);
    }

    private void ensureAdmin(AuthenticatedUser currentUser) {
        ServiceHelper.ensureAdmin(currentUser);
    }

    private String normalizeBlankToNull(String value) {
        return ServiceHelper.normalizeBlankToNull(value);
    }
}
