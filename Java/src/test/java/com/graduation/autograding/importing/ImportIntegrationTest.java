package com.graduation.autograding.importing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class ImportIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void importUsersSupportsQuotedFieldsAndSkipsInvalidRows() throws Exception {
        String teacherToken = login("teacher1", "123456");
        String username = "import_user_" + System.nanoTime();
        String invalidUsername = "import_invalid_" + System.nanoTime();

        String csvContent = String.join("\n",
                "username,password,role,fullName,className",
                username + ",123456,STUDENT,\"Student, Alpha\",\"SE, Class\"",
                invalidUsername + ",123456,INVALID_ROLE,Bad Role,Class A",
                "broken,line"
        );

        JsonNode importResult = readJson(
                mockMvc.perform(post("/api/import/users")
                                .header(AUTHORIZATION, bearer(teacherToken))
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("csvContent", csvContent))))
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                        .andReturn()
        );

        assertEquals("users", importResult.get("importType").asText());
        assertEquals(1, importResult.get("createdCount").asInt());
        assertEquals(2, importResult.get("skippedCount").asInt());
        assertEquals(2, importResult.get("details").size());

        JsonNode users = readJson(
                mockMvc.perform(get("/api/users")
                                .header(AUTHORIZATION, bearer(teacherToken)))
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                        .andReturn()
        );

        JsonNode importedUser = findByUsername(users, username);
        assertNotNull(importedUser);
        assertEquals("Student, Alpha", importedUser.get("fullName").asText());
        assertEquals("SE, Class", importedUser.get("className").asText());
    }

    @Test
    void importCoursesAndEnrollmentsSkipBadRowsWithoutAbortingBatch() throws Exception {
        String teacherToken = login("teacher1", "123456");
        String studentUsername = "import_student_" + System.nanoTime();
        long studentId = register(studentUsername, "123456", "STUDENT").get("id").asLong();
        String courseCode = "IMP" + System.nanoTime();

        String coursesCsv = String.join("\n",
                "code,name,term,className",
                courseCode + ",\"Programming, Basics\",2026 Spring,\"Software, Class\"",
                "broken,line"
        );

        JsonNode courseImportResult = readJson(
                mockMvc.perform(post("/api/import/courses")
                                .header(AUTHORIZATION, bearer(teacherToken))
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("csvContent", coursesCsv))))
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                        .andReturn()
        );

        assertEquals("courses", courseImportResult.get("importType").asText());
        assertEquals(1, courseImportResult.get("createdCount").asInt());
        assertEquals(1, courseImportResult.get("skippedCount").asInt());

        JsonNode courses = readJson(
                mockMvc.perform(get("/api/courses")
                                .header(AUTHORIZATION, bearer(teacherToken)))
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                        .andReturn()
        );

        JsonNode importedCourse = findByCode(courses, courseCode);
        assertNotNull(importedCourse);
        assertEquals("Programming, Basics", importedCourse.get("name").asText());
        assertEquals("Software, Class", importedCourse.get("className").asText());

        String enrollmentsCsv = String.join("\n",
                "courseCode,studentUsername",
                courseCode + "," + studentUsername,
                courseCode + ",missing_student"
        );

        JsonNode enrollmentImportResult = readJson(
                mockMvc.perform(post("/api/import/enrollments")
                                .header(AUTHORIZATION, bearer(teacherToken))
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("csvContent", enrollmentsCsv))))
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                        .andReturn()
        );

        assertEquals("enrollments", enrollmentImportResult.get("importType").asText());
        assertEquals(1, enrollmentImportResult.get("createdCount").asInt());
        assertEquals(1, enrollmentImportResult.get("skippedCount").asInt());

        JsonNode enrollments = readJson(
                mockMvc.perform(get("/api/courses/{courseId}/enrollments", importedCourse.get("id").asLong())
                                .header(AUTHORIZATION, bearer(teacherToken)))
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                        .andReturn()
        );

        JsonNode enrollment = findByStudentId(enrollments, studentId);
        assertNotNull(enrollment);
        assertEquals(studentUsername, enrollment.get("studentUsername").asText());
    }

    @Test
    void teacherCannotImportAdminUsersButAdminCan() throws Exception {
        String teacherToken = login("teacher1", "123456");
        String adminToken = login("admin1", "123456");
        String teacherImportedAdmin = "csv_admin_teacher_" + System.nanoTime();
        String adminImportedAdmin = "csv_admin_admin_" + System.nanoTime();

        String teacherCsv = String.join("\n",
                "username,password,role,fullName,className",
                teacherImportedAdmin + ",123456,ADMIN,Teacher Blocked,Platform"
        );

        JsonNode teacherImportResult = readJson(
                mockMvc.perform(post("/api/import/users")
                                .header(AUTHORIZATION, bearer(teacherToken))
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("csvContent", teacherCsv))))
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                        .andReturn()
        );
        assertEquals(0, teacherImportResult.get("createdCount").asInt());
        assertEquals(1, teacherImportResult.get("skippedCount").asInt());
        assertTrue(teacherImportResult.get("details").toString().contains("非管理员"));

        String adminCsv = String.join("\n",
                "username,password,role,fullName,className",
                adminImportedAdmin + ",123456,ADMIN,Admin Allowed,Platform"
        );

        JsonNode adminImportResult = readJson(
                mockMvc.perform(post("/api/import/users")
                                .header(AUTHORIZATION, bearer(adminToken))
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("csvContent", adminCsv))))
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                        .andReturn()
        );
        assertEquals(1, adminImportResult.get("createdCount").asInt());
        assertEquals(0, adminImportResult.get("skippedCount").asInt());

        JsonNode users = readJson(
                mockMvc.perform(get("/api/users")
                                .header(AUTHORIZATION, bearer(adminToken)))
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                        .andReturn()
        );

        assertEquals(null, findByUsername(users, teacherImportedAdmin));
        JsonNode importedAdmin = findByUsername(users, adminImportedAdmin);
        assertNotNull(importedAdmin);
        assertEquals("ADMIN", importedAdmin.get("role").asText());
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

    private JsonNode register(String username, String password, String role) throws Exception {
        return readJson(
                mockMvc.perform(post("/api/auth/register")
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of(
                                        "username", username,
                                        "password", password,
                                        "role", role
                                ))))
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                        .andReturn()
        );
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode findByUsername(JsonNode items, String username) {
        return StreamSupport.stream(items.spliterator(), false)
                .filter(item -> username.equals(item.get("username").asText()))
                .findFirst()
                .orElse(null);
    }

    private JsonNode findByCode(JsonNode items, String code) {
        return StreamSupport.stream(items.spliterator(), false)
                .filter(item -> code.equals(item.get("code").asText()))
                .findFirst()
                .orElse(null);
    }

    private JsonNode findByStudentId(JsonNode items, long studentId) {
        return StreamSupport.stream(items.spliterator(), false)
                .filter(item -> studentId == item.get("studentId").asLong())
                .findFirst()
                .orElse(null);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
