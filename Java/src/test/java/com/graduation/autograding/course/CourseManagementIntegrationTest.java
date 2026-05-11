package com.graduation.autograding.course;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = "app.startup.open-browser=false")
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class CourseManagementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void teacherCanUpdateAndDeleteCourseWithoutAssignments() throws Exception {
        String teacherToken = login("teacher1", "123456");
        JsonNode createdCourse = createCourse(teacherToken, uniqueCode("CS"), "原始课程");
        long courseId = createdCourse.get("id").asLong();

        String updatedCode = uniqueCode("UPD");
        JsonNode updatedCourse = readJson(
                mockMvc.perform(put("/api/courses/{courseId}", courseId)
                                .header(AUTHORIZATION, bearer(teacherToken))
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of(
                                        "code", updatedCode,
                                        "name", "更新后的课程",
                                        "term", "2026 秋",
                                        "className", "软件工程 2 班",
                                        "active", false
                                ))))
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                        .andReturn()
        );

        assertEquals(courseId, updatedCourse.get("id").asLong());
        assertEquals(updatedCode, updatedCourse.get("code").asText());
        assertEquals("更新后的课程", updatedCourse.get("name").asText());
        assertEquals("2026 秋", updatedCourse.get("term").asText());
        assertEquals("软件工程 2 班", updatedCourse.get("className").asText());
        assertFalse(updatedCourse.get("active").asBoolean());

        JsonNode listedCourses = readJson(
                mockMvc.perform(get("/api/courses")
                                .header(AUTHORIZATION, bearer(teacherToken)))
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                        .andReturn()
        );
        JsonNode listedCourse = findById(listedCourses, courseId);
        assertNotNull(listedCourse);
        assertEquals(updatedCode, listedCourse.get("code").asText());
        assertFalse(listedCourse.get("active").asBoolean());

        mockMvc.perform(delete("/api/courses/{courseId}", courseId)
                        .header(AUTHORIZATION, bearer(teacherToken)))
                .andExpect(status().isNoContent());

        JsonNode coursesAfterDelete = readJson(
                mockMvc.perform(get("/api/courses")
                                .header(AUTHORIZATION, bearer(teacherToken)))
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                        .andReturn()
        );
        assertFalse(containsId(coursesAfterDelete, courseId));
    }

    @Test
    void teacherCannotDeleteCourseThatAlreadyHasAssignments() throws Exception {
        String teacherToken = login("teacher1", "123456");
        JsonNode createdCourse = createCourse(teacherToken, uniqueCode("DEL"), "不可删除课程");
        long courseId = createdCourse.get("id").asLong();
        createAssignment(teacherToken, courseId, "课程删除保护作业");

        mockMvc.perform(delete("/api/courses/{courseId}", courseId)
                        .header(AUTHORIZATION, bearer(teacherToken)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("当前课程下已有作业，不能直接删除。"));
    }

    @Test
    void studentOnlySeesEnrolledCourses() throws Exception {
        String teacherToken = login("teacher1", "123456");
        String studentToken = login("student1", "123456");

        JsonNode visibleCourse = createCourse(teacherToken, uniqueCode("VIS"), "Student Visible Course");
        JsonNode hiddenCourse = createCourse(teacherToken, uniqueCode("HID"), "Student Hidden Course");
        long visibleCourseId = visibleCourse.get("id").asLong();

        JsonNode currentUser = readJson(
                mockMvc.perform(get("/api/auth/me")
                                .header(AUTHORIZATION, bearer(studentToken)))
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                        .andReturn()
        );
        long studentId = currentUser.get("id").asLong();

        mockMvc.perform(post("/api/courses/{courseId}/enrollments/{studentId}", visibleCourseId, studentId)
                        .header(AUTHORIZATION, bearer(teacherToken)))
                .andExpect(status().isOk());

        JsonNode studentCourses = readJson(
                mockMvc.perform(get("/api/courses")
                                .header(AUTHORIZATION, bearer(studentToken)))
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                        .andReturn()
        );

        assertTrue(containsId(studentCourses, visibleCourseId));
        assertFalse(containsId(studentCourses, hiddenCourse.get("id").asLong()));
    }

    private JsonNode createCourse(String token, String code, String name) throws Exception {
        return readJson(
                mockMvc.perform(post("/api/courses")
                                .header(AUTHORIZATION, bearer(token))
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of(
                                        "code", code,
                                        "name", name,
                                        "term", "2026 春",
                                        "className", "软件工程 1 班"
                                ))))
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                        .andReturn()
        );
    }

    private void createAssignment(String token, long courseId, String title) throws Exception {
        mockMvc.perform(post("/api/assignments")
                        .header(AUTHORIZATION, bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", title,
                                "description", "Delete guard assignment",
                                "deadline", LocalDateTime.now().plusDays(1).truncatedTo(ChronoUnit.SECONDS).toString(),
                                "courseId", courseId,
                                "status", "PUBLISHED",
                                "maxSubmissions", 5,
                                "lateSubmissionAllowed", false,
                                "gradingPolicy", "LATEST"
                        ))))
                .andExpect(status().isOk());
    }

    private String login(String username, String password) throws Exception {
        JsonNode response = readJson(
                mockMvc.perform(post("/api/auth/login")
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of(
                                        "username", username,
                                        "password", password
                                ))))
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                        .andReturn()
        );
        return response.get("token").asText();
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode findById(JsonNode items, long id) {
        return StreamSupport.stream(items.spliterator(), false)
                .filter(item -> item.get("id").asLong() == id)
                .findFirst()
                .orElse(null);
    }

    private boolean containsId(JsonNode items, long id) {
        return StreamSupport.stream(items.spliterator(), false)
                .anyMatch(item -> item.get("id").asLong() == id);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String uniqueCode(String prefix) {
        return prefix + System.nanoTime();
    }
}
