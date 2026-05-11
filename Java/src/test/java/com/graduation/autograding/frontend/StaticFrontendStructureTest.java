package com.graduation.autograding.frontend;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class StaticFrontendStructureTest {

    @Test
    void studentCoursePanelBelongsToStudentSection() throws IOException {
        String html = new String(
                new ClassPathResource("static/index.html").getInputStream().readAllBytes(),
                StandardCharsets.UTF_8
        );

        int teacherSection = html.indexOf("id=\"teacherSection\"");
        int studentSection = html.indexOf("id=\"studentSection\"");
        int studentCourses = html.indexOf("id=\"studentCourses\"");

        assertTrue(teacherSection >= 0, "teacher section should exist");
        assertTrue(studentSection >= 0, "student section should exist");
        assertTrue(studentCourses >= 0, "student course panel should exist");
        assertTrue(studentCourses > studentSection, "student course panel should be rendered inside the student area");
        assertTrue(studentCourses > teacherSection, "student course panel should not be rendered in the teacher area");
    }
}
