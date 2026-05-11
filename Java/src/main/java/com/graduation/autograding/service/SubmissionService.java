package com.graduation.autograding.service;

import com.graduation.autograding.auth.AuthenticatedUser;
import com.graduation.autograding.domain.Assignment;
import com.graduation.autograding.domain.AssignmentGradingPolicy;
import com.graduation.autograding.domain.AssignmentStatus;
import com.graduation.autograding.domain.Submission;
import com.graduation.autograding.domain.SubmissionStatus;
import com.graduation.autograding.domain.User;
import com.graduation.autograding.domain.UserRole;
import com.graduation.autograding.dto.SubmissionCreateRequest;
import com.graduation.autograding.dto.SubmissionSummaryResponse;
import com.graduation.autograding.exception.ForbiddenException;
import com.graduation.autograding.exception.NotFoundException;
import com.graduation.autograding.repository.AssignmentRepository;
import com.graduation.autograding.repository.SubmissionRepository;
import com.graduation.autograding.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class SubmissionService {

    private static final String QUEUED_COMPILE_MESSAGE = "评测任务已进入队列。";
    private static final String QUEUED_RUNTIME_MESSAGE = "等待后台评测完成。";

    private final SubmissionRepository submissionRepository;
    private final AssignmentRepository assignmentRepository;
    private final UserRepository userRepository;
    private final CourseService courseService;
    private final AuditLogService auditLogService;
    private final SubmissionJudgeQueueService submissionJudgeQueueService;

    public SubmissionService(SubmissionRepository submissionRepository,
                             AssignmentRepository assignmentRepository,
                             UserRepository userRepository,
                             CourseService courseService,
                             AuditLogService auditLogService,
                             SubmissionJudgeQueueService submissionJudgeQueueService) {
        this.submissionRepository = submissionRepository;
        this.assignmentRepository = assignmentRepository;
        this.userRepository = userRepository;
        this.courseService = courseService;
        this.auditLogService = auditLogService;
        this.submissionJudgeQueueService = submissionJudgeQueueService;
    }

    @Transactional
    public Submission createSubmission(AuthenticatedUser currentUser, SubmissionCreateRequest request) {
        ensureStudent(currentUser);

        Assignment assignment = assignmentRepository.findById(request.assignmentId())
                .orElseThrow(() -> new NotFoundException("作业不存在。"));
        Long studentId = request.studentId() == null ? currentUser.id() : request.studentId();
        if (!studentId.equals(currentUser.id())) {
            throw new ForbiddenException("学生只能为自己的账号提交代码。");
        }

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new NotFoundException("学生不存在。"));
        if (student.getRole() != UserRole.STUDENT) {
            throw new IllegalArgumentException("只有学生可以提交作业。");
        }
        if (assignment.getCourse() != null) {
            courseService.ensureStudentEnrolled(assignment.getCourse().getId(), studentId);
        }
        if (assignment.getStatus() != AssignmentStatus.PUBLISHED) {
            throw new IllegalStateException("只有已发布的作业才允许提交。");
        }
        if (!assignment.isLateSubmissionAllowed() && LocalDateTime.now().isAfter(assignment.getDeadline())) {
            throw new IllegalStateException("当前作业已超过截止时间。");
        }
        if (submissionRepository.countByAssignmentIdAndStudentId(assignment.getId(), studentId) >= assignment.getMaxSubmissions()) {
            throw new IllegalStateException("当前作业已达到最大提交次数。");
        }

        Submission submission = new Submission(assignment, student, request.sourceCode(), "Main");
        markSubmissionPending(submission);
        Submission savedSubmission = submissionRepository.save(submission);
        enqueueJudgeAfterCommit(savedSubmission.getId());
        auditLogService.record(currentUser, "SUBMISSION_CREATED", "SUBMISSION", String.valueOf(savedSubmission.getId()),
                "Submitted code for assignment " + assignment.getTitle());
        return savedSubmission;
    }

    @Transactional(readOnly = true)
    public Submission getSubmission(AuthenticatedUser currentUser, Long submissionId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new NotFoundException("提交记录不存在。"));
        ensureSubmissionAccessible(currentUser, submission);
        return submission;
    }

    @Transactional
    public Submission rejudgeSubmission(AuthenticatedUser currentUser, Long submissionId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new NotFoundException("提交记录不存在。"));
        ensureTeacherCanManageSubmission(currentUser, submission);
        if (submission.getStatus() == SubmissionStatus.PENDING) {
            throw new IllegalStateException("当前提交正在评测中，无需重新判题。");
        }

        markSubmissionPending(submission);
        Submission savedSubmission = submissionRepository.save(submission);
        enqueueJudgeAfterCommit(savedSubmission.getId());
        auditLogService.record(currentUser, "SUBMISSION_REJUDGED", "SUBMISSION", String.valueOf(savedSubmission.getId()),
                "Rejudged submission " + savedSubmission.getId() + " for assignment "
                        + savedSubmission.getAssignment().getTitle());
        return savedSubmission;
    }

    @Transactional(readOnly = true)
    public List<Submission> listByStudent(AuthenticatedUser currentUser, Long studentId) {
        if (currentUser.isStudent() && !currentUser.id().equals(studentId)) {
            throw new ForbiddenException("学生只能查看自己的提交记录。");
        }
        return filterSubmissionsForViewer(
                currentUser,
                submissionRepository.findByStudentIdOrderBySubmittedAtDesc(studentId)
        );
    }

    @Transactional(readOnly = true)
    public List<Submission> listByAssignment(AuthenticatedUser currentUser, Long assignmentId) {
        ensureTeacherOwnsAssignment(currentUser, assignmentId);
        return submissionRepository.findByAssignmentIdOrderBySubmittedAtDesc(assignmentId);
    }

    @Transactional(readOnly = true)
    public Submission getLatestSubmission(AuthenticatedUser currentUser, Long assignmentId, Long studentId) {
        if (currentUser.isStudent() && !currentUser.id().equals(studentId)) {
            throw new ForbiddenException("学生只能查看自己的最新提交。");
        }
        if (currentUser.isTeacher() || currentUser.isAdmin()) {
            ensureTeacherOwnsAssignment(currentUser, assignmentId);
        }
        return submissionRepository.findFirstByAssignmentIdAndStudentIdOrderBySubmittedAtDesc(assignmentId, studentId)
                .orElseThrow(() -> new IllegalArgumentException("未找到该作业下该学生的提交记录。"));
    }

    @Transactional(readOnly = true)
    public List<SubmissionSummaryResponse> listStudentLatestSummaries(AuthenticatedUser currentUser, Long studentId) {
        if (currentUser.isStudent() && !currentUser.id().equals(studentId)) {
            throw new ForbiddenException("学生只能查看自己的最新提交概览。");
        }
        return filterSubmissionsForViewer(
                currentUser,
                submissionRepository.findByStudentIdOrderBySubmittedAtDesc(studentId)
        ).stream()
                .collect(java.util.stream.Collectors.toMap(
                        submission -> submission.getAssignment().getId(),
                        submission -> submission,
                        this::selectEffectiveSubmission,
                        java.util.LinkedHashMap::new
                ))
                .values()
                .stream()
                .map(this::toSummary)
                .toList();
    }

    private List<Submission> filterSubmissionsForViewer(AuthenticatedUser currentUser, List<Submission> submissions) {
        if (currentUser.isAdmin() || currentUser.isStudent()) {
            return submissions;
        }
        if (currentUser.isTeacher()) {
            return submissions.stream()
                    .filter(submission -> submission.getAssignment().getTeacher().getId().equals(currentUser.id()))
                    .toList();
        }
        return List.of();
    }

    private SubmissionSummaryResponse toSummary(Submission submission) {
        return new SubmissionSummaryResponse(
                submission.getId(),
                submission.getAssignment().getId(),
                submission.getAssignment().getTitle(),
                submission.getStudent().getId(),
                submission.getStudent().getUsername(),
                submission.getStatus().name(),
                submission.getScore(),
                submission.getSubmittedAt().toString()
        );
    }

    private Submission selectEffectiveSubmission(Submission first, Submission second) {
        if (first.getAssignment().getGradingPolicy() == AssignmentGradingPolicy.HIGHEST) {
            int firstScore = first.getScore() == null ? 0 : first.getScore();
            int secondScore = second.getScore() == null ? 0 : second.getScore();
            return firstScore >= secondScore ? first : second;
        }
        return first.getSubmittedAt().isAfter(second.getSubmittedAt()) ? first : second;
    }

    private void markSubmissionPending(Submission submission) {
        submission.setStatus(SubmissionStatus.PENDING);
        submission.setScore(0);
        submission.setCompileMessage(QUEUED_COMPILE_MESSAGE);
        submission.setRuntimeMessage(QUEUED_RUNTIME_MESSAGE);
        submission.clearCaseResults();
    }

    private void enqueueJudgeAfterCommit(Long submissionId) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    submissionJudgeQueueService.judgeSubmissionAsync(submissionId);
                }
            });
            return;
        }
        submissionJudgeQueueService.judgeSubmissionAsync(submissionId);
    }

    private void ensureSubmissionAccessible(AuthenticatedUser currentUser, Submission submission) {
        if (currentUser.isStudent() && !submission.getStudent().getId().equals(currentUser.id())) {
            throw new ForbiddenException("学生只能查看自己的提交记录。");
        }
        if (currentUser.isTeacher() && !submission.getAssignment().getTeacher().getId().equals(currentUser.id())) {
            throw new ForbiddenException("教师只能查看自己作业的提交记录。");
        }
    }

    private void ensureTeacherCanManageSubmission(AuthenticatedUser currentUser, Submission submission) {
        if (!currentUser.canManagePlatform()) {
            throw new ForbiddenException("只有教师或管理员可以重新判题。");
        }
        if (!currentUser.isAdmin() && !submission.getAssignment().getTeacher().getId().equals(currentUser.id())) {
            throw new ForbiddenException("你只能重新判题自己作业的提交记录。");
        }
    }

    private void ensureTeacherOwnsAssignment(AuthenticatedUser currentUser, Long assignmentId) {
        if (!currentUser.canManagePlatform()) {
            throw new ForbiddenException("只有教师或管理员可以查看作业提交。");
        }
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new NotFoundException("作业不存在。"));
        if (!currentUser.isAdmin() && !assignment.getTeacher().getId().equals(currentUser.id())) {
            throw new ForbiddenException("你只能查看自己创建作业的提交记录。");
        }
    }

    private void ensureStudent(AuthenticatedUser currentUser) {
        if (!currentUser.isStudent()) {
            throw new ForbiddenException("只有学生可以提交作业。");
        }
    }
}
