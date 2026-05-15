package com.graduation.autograding.config;

import com.graduation.autograding.auth.PasswordService;
import com.graduation.autograding.domain.Assignment;
import com.graduation.autograding.domain.AssignmentGradingPolicy;
import com.graduation.autograding.domain.AssignmentStatus;
import com.graduation.autograding.domain.Course;
import com.graduation.autograding.domain.CourseEnrollment;
import com.graduation.autograding.domain.TestCase;
import com.graduation.autograding.domain.User;
import com.graduation.autograding.domain.UserRole;
import com.graduation.autograding.repository.AssignmentRepository;
import com.graduation.autograding.repository.CourseEnrollmentRepository;
import com.graduation.autograding.repository.CourseRepository;
import com.graduation.autograding.repository.UserRepository;
import java.time.LocalDateTime;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("demo")
public class DataSeeder {

    private static final String DEMO_ASSIGNMENT_TITLE = "演示：两数求和";
    private static final String DEMO_ASSIGNMENT_DESCRIPTION = "读取两个整数，输出它们的和。该作业用于答辩演示自动评测完整流程。";

    @Bean
    CommandLineRunner seedUsers(UserRepository userRepository,
                                CourseRepository courseRepository,
                                CourseEnrollmentRepository courseEnrollmentRepository,
                                AssignmentRepository assignmentRepository,
                                PasswordService passwordService) {
        return args -> {
            userRepository.findByUsername("admin1")
                    .orElseGet(() -> userRepository.save(new User(
                            "admin1",
                            passwordService.encode("123456"),
                            UserRole.ADMIN,
                            "系统管理员",
                            null
                    )));
            User teacher = userRepository.findByUsername("teacher1")
                    .orElseGet(() -> userRepository.save(new User(
                            "teacher1",
                            passwordService.encode("123456"),
                            UserRole.TEACHER,
                            "演示教师",
                            null
                    )));
            User student = userRepository.findByUsername("student1")
                    .orElseGet(() -> userRepository.save(new User(
                            "student1",
                            passwordService.encode("123456"),
                            UserRole.STUDENT,
                            "演示学生",
                            "软件工程01班"
                    )));
            Course course = courseRepository.findByCode("CS101")
                    .orElseGet(() -> {
                        Course c = new Course("程序设计基础-1班", "2025~2026第二学期", "软件工程01班");
                        c.setCode("CS101");
                        c.setTeacher(teacher);
                        return courseRepository.save(c);
                    });
            if (!courseEnrollmentRepository.existsByCourseIdAndStudentId(course.getId(), student.getId())) {
                courseEnrollmentRepository.save(new CourseEnrollment(course, student));
            }

            Assignment demoAssignment = assignmentRepository.findByTeacherIdOrderByDeadlineAsc(teacher.getId())
                    .stream()
                    .filter(assignment -> DEMO_ASSIGNMENT_TITLE.equals(assignment.getTitle())
                            && assignment.getCourse() != null
                            && assignment.getCourse().getId().equals(course.getId()))
                    .findFirst()
                    .orElseGet(() -> new Assignment(
                            DEMO_ASSIGNMENT_TITLE,
                            DEMO_ASSIGNMENT_DESCRIPTION,
                            LocalDateTime.now().plusDays(14),
                            teacher
                    ));
            prepareDemoAssignment(demoAssignment, teacher, course);
            assignmentRepository.save(demoAssignment);
        };
    }

    private void prepareDemoAssignment(Assignment assignment, User teacher, Course course) {
        assignment.setTitle(DEMO_ASSIGNMENT_TITLE);
        assignment.setDescription(DEMO_ASSIGNMENT_DESCRIPTION);
        assignment.setDeadline(LocalDateTime.now().plusDays(14));
        assignment.setTeacher(teacher);
        assignment.setCourse(course);
        assignment.setStatus(AssignmentStatus.PUBLISHED);
        assignment.setMaxSubmissions(5);
        assignment.setLateSubmissionAllowed(false);
        assignment.setGradingPolicy(AssignmentGradingPolicy.LATEST);
        assignment.getTestCases().clear();
        assignment.addTestCase(new TestCase("1 2", "3"));
        assignment.addTestCase(new TestCase("6 9", "15"));
    }
}
