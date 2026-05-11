package com.graduation.autograding.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.graduation.autograding.domain.AssignmentStatus;
import com.graduation.autograding.repository.AssignmentRepository;
import com.graduation.autograding.repository.CourseEnrollmentRepository;
import com.graduation.autograding.repository.CourseRepository;
import com.graduation.autograding.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:data_seeder_demo_profile_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.startup.open-browser=false"
})
@ActiveProfiles("demo")
@DirtiesContext
class DataSeederDemoProfileIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private CourseEnrollmentRepository courseEnrollmentRepository;

    @Test
    void demoProfileSeedsCompleteDefenseDemoData() {
        var teacher = userRepository.findByUsername("teacher1").orElseThrow();
        var student = userRepository.findByUsername("student1").orElseThrow();
        var course = courseRepository.findByCode("CS101").orElseThrow();

        assertTrue(courseEnrollmentRepository.existsByCourseIdAndStudentId(course.getId(), student.getId()));
        var assignment = assignmentRepository.findByTeacherIdOrderByDeadlineAsc(teacher.getId()).stream()
                .filter(candidate -> "演示：两数求和".equals(candidate.getTitle()))
                .findFirst()
                .orElseThrow();
        assertEquals(AssignmentStatus.PUBLISHED, assignment.getStatus());
        assertEquals(course.getId(), assignment.getCourse().getId());
        assertEquals(5, assignment.getMaxSubmissions());
        assertEquals(2, assignment.getTestCases().size());
        assertEquals("1 2", assignment.getTestCases().get(0).getInputData());
        assertEquals("3", assignment.getTestCases().get(0).getExpectedOutput());
        assertEquals("6 9", assignment.getTestCases().get(1).getInputData());
        assertEquals("15", assignment.getTestCases().get(1).getExpectedOutput());
    }
}
