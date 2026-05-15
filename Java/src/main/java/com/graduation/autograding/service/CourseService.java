package com.graduation.autograding.service;

import com.graduation.autograding.auth.AuthenticatedUser;
import com.graduation.autograding.domain.Assignment;
import com.graduation.autograding.domain.AssignmentGradingPolicy;
import com.graduation.autograding.domain.AssignmentStatus;
import com.graduation.autograding.domain.Course;
import com.graduation.autograding.domain.CourseEnrollment;
import com.graduation.autograding.domain.User;
import com.graduation.autograding.domain.UserRole;
import com.graduation.autograding.dto.AdminCourseCreateRequest;
import com.graduation.autograding.dto.AdminCourseUpdateRequest;
import com.graduation.autograding.dto.CourseCreateRequest;
import com.graduation.autograding.dto.CourseStatisticsResponse;
import com.graduation.autograding.dto.CourseUpdateRequest;
import com.graduation.autograding.exception.ForbiddenException;
import com.graduation.autograding.exception.NotFoundException;
import com.graduation.autograding.repository.AssignmentRepository;
import com.graduation.autograding.repository.CourseEnrollmentRepository;
import com.graduation.autograding.repository.CourseRepository;
import com.graduation.autograding.repository.SubmissionRepository;
import com.graduation.autograding.repository.UserRepository;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public CourseService(CourseRepository courseRepository,
                         CourseEnrollmentRepository courseEnrollmentRepository,
                         AssignmentRepository assignmentRepository,
                         SubmissionRepository submissionRepository,
                         UserRepository userRepository,
                         AuditLogService auditLogService) {
        this.courseRepository = courseRepository;
        this.courseEnrollmentRepository = courseEnrollmentRepository;
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public Course createCourse(AuthenticatedUser currentUser, CourseCreateRequest request) {
        ensureTeacher(currentUser);
        if (courseRepository.findByCode(normalize(request.code())).isPresent()) {
            throw new IllegalArgumentException("课程代码已存在。");
        }
        User teacher = userRepository.findById(currentUser.id())
                .orElseThrow(() -> new NotFoundException("教师不存在。"));
        Course course = courseRepository.save(new Course(
                normalize(request.code()),
                normalize(request.name()),
                normalize(request.term()),
                normalize(request.className()),
                teacher
        ));
        auditLogService.record(currentUser, "COURSE_CREATED", "COURSE", String.valueOf(course.getId()),
                "创建课程：" + course.getDisplayName());
        return course;
    }

    @Transactional
    public Course updateCourse(AuthenticatedUser currentUser, Long courseId, CourseUpdateRequest request) {
        ensureTeacher(currentUser);
        Course course = requireTeacherCourse(currentUser, courseId);
        String normalizedCode = normalize(request.code());
        courseRepository.findByCode(normalizedCode)
                .filter(existing -> !existing.getId().equals(courseId))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("课程代码已存在。");
                });

        course.setCode(normalizedCode);
        course.setName(normalize(request.name()));
        course.setTerm(normalize(request.term()));
        course.setClassName(normalize(request.className()));
        if (request.active() != null) {
            course.setActive(request.active());
        }

        Course savedCourse = courseRepository.save(course);
        auditLogService.record(currentUser, "COURSE_UPDATED", "COURSE", String.valueOf(savedCourse.getId()),
                "更新课程：" + savedCourse.getDisplayName());
        return savedCourse;
    }

    @Transactional
    public void deleteCourse(AuthenticatedUser currentUser, Long courseId) {
        ensureTeacher(currentUser);
        Course course = requireTeacherCourse(currentUser, courseId);
        if (assignmentRepository.countByCourseId(courseId) > 0) {
            throw new IllegalStateException("当前课程下已有作业，不能直接删除。");
        }

        String displayName = course.getDisplayName();
        courseEnrollmentRepository.deleteByCourseId(courseId);
        courseRepository.delete(course);
        auditLogService.record(currentUser, "COURSE_DELETED", "COURSE", String.valueOf(courseId),
                "删除课程：" + displayName);
    }

    @Transactional(readOnly = true)
    public List<Course> listTeacherCourses(AuthenticatedUser currentUser) {
        ensureTeacher(currentUser);
        if (currentUser.isAdmin()) {
            return courseRepository.findAll();
        }
        return courseRepository.findByTeacherIdOrderByNameAsc(currentUser.id());
    }

    @Transactional(readOnly = true)
    public List<Course> listAccessibleCourses(AuthenticatedUser currentUser) {
        if (currentUser.isStudent()) {
            return courseEnrollmentRepository.findByStudentId(currentUser.id()).stream()
                    .map(CourseEnrollment::getCourse)
                    .filter(Course::isActive)
                    .sorted(java.util.Comparator.comparing(Course::getName))
                    .toList();
        }
        return listTeacherCourses(currentUser);
    }

    @Transactional(readOnly = true)
    public Page<Course> searchCoursesForAdmin(AuthenticatedUser currentUser, String keyword, Pageable pageable) {
        ServiceHelper.ensureAdmin(currentUser);
        return courseRepository.searchForAdmin(ServiceHelper.normalizeBlankToNull(keyword), pageable);
    }

    @Transactional(readOnly = true)
    public List<CourseStatisticsResponse> listCourseStatistics(AuthenticatedUser currentUser) {
        ensureTeacher(currentUser);
        List<Course> courses = listTeacherCourses(currentUser);
        if (courses.isEmpty()) {
            return List.of();
        }

        List<Assignment> assignments = currentUser.isAdmin()
                ? assignmentRepository.findAll()
                : assignmentRepository.findByTeacherIdOrderByDeadlineAsc(currentUser.id());
        Map<Long, List<Assignment>> assignmentsByCourseId = assignments.stream()
                .filter(assignment -> assignment.getCourse() != null)
                .collect(Collectors.groupingBy(assignment -> assignment.getCourse().getId()));

        List<Long> courseIds = courses.stream().map(Course::getId).toList();
        List<Long> assignmentIds = assignments.stream().map(Assignment::getId).toList();

        Map<Long, Long> enrollmentCountByCourse = new HashMap<>();
        for (Object[] row : courseEnrollmentRepository.findEnrollmentCountsByCourseIds(courseIds)) {
            enrollmentCountByCourse.put((Long) row[0], (Long) row[1]);
        }

        Map<Long, List<Object[]>> scoresByAssignment = new HashMap<>();
        if (!assignmentIds.isEmpty()) {
            for (Object[] row : submissionRepository.findScoreProjectionsByAssignmentIds(assignmentIds)) {
                Long assignmentId = (Long) row[0];
                scoresByAssignment.computeIfAbsent(assignmentId, key -> new java.util.ArrayList<>()).add(row);
            }
        }

        return courses.stream()
                .map(course -> toCourseStatistics(course,
                        assignmentsByCourseId.getOrDefault(course.getId(), List.of()),
                        enrollmentCountByCourse.getOrDefault(course.getId(), 0L),
                        scoresByAssignment))
                .toList();
    }

    @Transactional
    public void enrollStudent(AuthenticatedUser currentUser, Long courseId, Long studentId) {
        ensureTeacher(currentUser);
        Course course = requireTeacherCourse(currentUser, courseId);
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new NotFoundException("学生不存在。"));
        if (student.getRole() != UserRole.STUDENT) {
            throw new IllegalArgumentException("只能为学生办理选课。");
        }
        if (courseEnrollmentRepository.existsByCourseIdAndStudentId(courseId, studentId)) {
            return;
        }
        courseEnrollmentRepository.save(new CourseEnrollment(course, student));
        auditLogService.record(currentUser, "COURSE_ENROLLMENT_CREATED", "COURSE", String.valueOf(courseId),
                "选课：" + student.getUsername() + " 加入 " + course.getDisplayName());
    }

    @Transactional
    public void removeStudentEnrollment(AuthenticatedUser currentUser, Long courseId, Long studentId) {
        ensureTeacher(currentUser);
        Course course = requireTeacherCourse(currentUser, courseId);
        CourseEnrollment enrollment = courseEnrollmentRepository.findByCourseIdAndStudentId(courseId, studentId)
                .orElseThrow(() -> new NotFoundException("未找到该选课记录。"));
        courseEnrollmentRepository.delete(enrollment);
        auditLogService.record(currentUser, "COURSE_ENROLLMENT_DELETED", "COURSE", String.valueOf(courseId),
                "退课：" + enrollment.getStudent().getUsername() + " 退出 " + course.getDisplayName());
    }

    @Transactional(readOnly = true)
    public List<CourseEnrollment> listCourseEnrollments(AuthenticatedUser currentUser, Long courseId) {
        ensureTeacher(currentUser);
        requireTeacherCourse(currentUser, courseId);
        return courseEnrollmentRepository.findByCourseIdOrderByEnrolledAtDesc(courseId);
    }

    @Transactional
    public Course resolveAssignmentCourse(AuthenticatedUser currentUser, Long courseId) {
        ensureTeacher(currentUser);
        if (courseId != null) {
            return requireTeacherCourse(currentUser, courseId);
        }
        return courseRepository.findFirstByTeacherIdOrderByIdAsc(currentUser.id())
                .orElseGet(() -> createDefaultCourse(currentUser));
    }

    @Transactional(readOnly = true)
    public void ensureStudentEnrolled(Long courseId, Long studentId) {
        if (!courseEnrollmentRepository.existsByCourseIdAndStudentId(courseId, studentId)) {
            throw new ForbiddenException("当前学生未选修该作业所属课程。");
        }
    }

    private Course requireTeacherCourse(AuthenticatedUser currentUser, Long courseId) {
        ensureTeacher(currentUser);
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("课程不存在。"));
        if (!currentUser.isAdmin() && (course.getTeacher() == null || !course.getTeacher().getId().equals(currentUser.id()))) {
            throw new ForbiddenException("你只能管理自己负责的课程。");
        }
        return course;
    }

    private Course createDefaultCourse(AuthenticatedUser currentUser) {
        User teacher = userRepository.findById(currentUser.id())
                .orElseThrow(() -> new NotFoundException("教师不存在。"));
        Course course = courseRepository.save(new Course(
                "AUTO-" + currentUser.id(),
                "默认课程",
                "当前学期",
                "默认班级",
                teacher
        ));
        auditLogService.record(currentUser, "COURSE_CREATED", "COURSE", String.valueOf(course.getId()),
                "自动创建默认课程：" + course.getDisplayName());
        return course;
    }

    private CourseStatisticsResponse toCourseStatistics(Course course, List<Assignment> assignments,
                                                         long enrollmentCount,
                                                         Map<Long, List<Object[]>> scoresByAssignment) {
        long publishedAssignmentCount = assignments.stream()
                .filter(assignment -> assignment.getStatus() == AssignmentStatus.PUBLISHED)
                .count();

        long totalSubmissions = 0;
        Set<Long> submittedStudentIds = new HashSet<>();
        Map<String, Integer> effectiveScores = new HashMap<>();

        for (Assignment assignment : assignments) {
            List<Object[]> rows = scoresByAssignment.getOrDefault(assignment.getId(), List.of());
            totalSubmissions += rows.size();
            for (Object[] row : rows) {
                Long studentId = (Long) row[1];
                Integer score = row[2] == null ? 0 : (Integer) row[2];
                submittedStudentIds.add(studentId);
                String key = assignment.getId() + ":" + studentId;
                if (assignment.getGradingPolicy() == AssignmentGradingPolicy.HIGHEST) {
                    effectiveScores.merge(key, score, (existing, candidate) -> candidate > existing ? candidate : existing);
                } else {
                    effectiveScores.putIfAbsent(key, score);
                }
            }
        }

        double averageScore = effectiveScores.isEmpty() ? 0.0
                : effectiveScores.values().stream().mapToInt(Integer::intValue).average().orElse(0.0);

        return new CourseStatisticsResponse(
                course.getId(),
                course.getCode(),
                course.getName(),
                course.getTerm(),
                course.getClassName(),
                course.isActive(),
                enrollmentCount,
                assignments.size(),
                publishedAssignmentCount,
                submittedStudentIds.size(),
                totalSubmissions,
                averageScore
        );
    }

    @Transactional
    public Course createCourseByAdmin(AuthenticatedUser currentUser, AdminCourseCreateRequest request) {
        ServiceHelper.ensureAdmin(currentUser);
        Course course = new Course(
                normalize(request.name()),
                normalize(request.term()),
                normalize(request.className())
        );
        if (request.code() != null && !request.code().isBlank()) {
            course.setCode(normalize(request.code()));
        }
        Course saved = courseRepository.save(course);
        auditLogService.record(currentUser, "COURSE_CREATED", "COURSE", String.valueOf(saved.getId()),
                "管理员创建课程班级：" + saved.getDisplayName());
        return saved;
    }

    @Transactional
    public Course updateCourseByAdmin(AuthenticatedUser currentUser, Long courseId, AdminCourseUpdateRequest request) {
        ServiceHelper.ensureAdmin(currentUser);
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("课程不存在。"));
        course.setName(normalize(request.name()));
        course.setTerm(normalize(request.term()));
        course.setClassName(normalize(request.className()));
        course.setCode(request.code() != null && !request.code().isBlank() ? normalize(request.code()) : null);
        if (request.active() != null) {
            course.setActive(request.active());
        }
        Course saved = courseRepository.save(course);
        auditLogService.record(currentUser, "COURSE_UPDATED", "COURSE", String.valueOf(saved.getId()),
                "管理员更新课程班级：" + saved.getDisplayName());
        return saved;
    }

    @Transactional
    public void deleteCourseByAdmin(AuthenticatedUser currentUser, Long courseId) {
        ServiceHelper.ensureAdmin(currentUser);
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("课程不存在。"));
        if (assignmentRepository.countByCourseId(courseId) > 0) {
            throw new IllegalStateException("当前课程下已有作业，不能直接删除。");
        }
        String displayName = course.getDisplayName();
        courseEnrollmentRepository.deleteByCourseId(courseId);
        courseRepository.delete(course);
        auditLogService.record(currentUser, "COURSE_DELETED", "COURSE", String.valueOf(courseId),
                "管理员删除课程班级：" + displayName);
    }

    @Transactional(readOnly = true)
    public List<Course> listAvailableCourses(String term) {
        if (term != null && !term.isBlank()) {
            return courseRepository.findAllByTermOrderByNameAsc(term.trim());
        }
        return courseRepository.findAllOrderByTermDescNameAsc();
    }

    @Transactional
    public void joinCourse(AuthenticatedUser currentUser, Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("课程不存在。"));
        if (currentUser.isTeacher()) {
            if (course.getTeacher() != null) {
                throw new IllegalStateException("该课程班级已有任课教师，无法加入。");
            }
            User teacher = userRepository.findById(currentUser.id())
                    .orElseThrow(() -> new NotFoundException("用户不存在。"));
            course.setTeacher(teacher);
            courseRepository.save(course);
            auditLogService.record(currentUser, "COURSE_TEACHER_JOINED", "COURSE", String.valueOf(courseId),
                    "教师加入课程班级：" + course.getDisplayName());
        } else if (currentUser.isStudent()) {
            if (courseEnrollmentRepository.existsByCourseIdAndStudentId(courseId, currentUser.id())) {
                return;
            }
            User student = userRepository.findById(currentUser.id())
                    .orElseThrow(() -> new NotFoundException("用户不存在。"));
            courseEnrollmentRepository.save(new CourseEnrollment(course, student));
            auditLogService.record(currentUser, "COURSE_ENROLLMENT_CREATED", "COURSE", String.valueOf(courseId),
                    "学生加入课程班级：" + course.getDisplayName());
        } else {
            throw new ForbiddenException("管理员无需加入课程班级。");
        }
    }

    @Transactional
    public void leaveCourse(AuthenticatedUser currentUser, Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("课程不存在。"));
        if (currentUser.isTeacher()) {
            if (course.getTeacher() == null || !course.getTeacher().getId().equals(currentUser.id())) {
                throw new ForbiddenException("你不是该课程班级的任课教师。");
            }
            course.setTeacher(null);
            courseRepository.save(course);
            auditLogService.record(currentUser, "COURSE_TEACHER_LEFT", "COURSE", String.valueOf(courseId),
                    "教师退出课程班级：" + course.getDisplayName());
        } else if (currentUser.isStudent()) {
            CourseEnrollment enrollment = courseEnrollmentRepository
                    .findByCourseIdAndStudentId(courseId, currentUser.id())
                    .orElseThrow(() -> new NotFoundException("未找到该选课记录。"));
            courseEnrollmentRepository.delete(enrollment);
            auditLogService.record(currentUser, "COURSE_ENROLLMENT_DELETED", "COURSE", String.valueOf(courseId),
                    "学生退出课程班级：" + course.getDisplayName());
        } else {
            throw new ForbiddenException("管理员无需退出课程班级。");
        }
    }

    private void ensureTeacher(AuthenticatedUser currentUser) {
        ServiceHelper.ensureTeacher(currentUser);
    }

    private String normalize(String value) {
        return ServiceHelper.normalize(value);
    }
}
