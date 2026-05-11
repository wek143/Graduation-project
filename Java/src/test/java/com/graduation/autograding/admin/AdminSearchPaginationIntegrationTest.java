package com.graduation.autograding.admin;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
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
class AdminSearchPaginationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void adminCanSearchUsersWithPaginationAndRoleFilter() throws Exception {
        String adminToken = login("admin1", "123456");
        String prefix = "paged_user_" + System.nanoTime();

        register(prefix + "_student_a", "123456", "STUDENT");
        register(prefix + "_student_b", "123456", "STUDENT");
        register(prefix + "_teacher", "123456", "TEACHER");

        mockMvc.perform(get("/api/admin/users")
                        .header(AUTHORIZATION, bearer(adminToken))
                        .param("keyword", prefix)
                        .param("role", "STUDENT")
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].role").value("STUDENT"))
                .andExpect(jsonPath("$.content[0].username", containsString(prefix)));
    }

    @Test
    void adminCanSearchCoursesAssignmentsAndAuditLogs() throws Exception {
        String adminToken = login("admin1", "123456");
        String teacherToken = login("teacher1", "123456");
        String courseCode = "ADM" + System.nanoTime();
        String assignmentTitle = "AdminSearch_" + System.nanoTime();

        long courseId = createCourse(teacherToken, courseCode, "Admin Search Course");
        createAssignment(teacherToken, courseId, assignmentTitle);

        mockMvc.perform(get("/api/admin/courses")
                        .header(AUTHORIZATION, bearer(adminToken))
                        .param("keyword", courseCode)
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].code").value(courseCode));

        mockMvc.perform(get("/api/admin/assignments")
                        .header(AUTHORIZATION, bearer(adminToken))
                        .param("keyword", assignmentTitle)
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value(assignmentTitle));

        mockMvc.perform(get("/api/admin/audit-logs")
                        .header(AUTHORIZATION, bearer(adminToken))
                        .param("keyword", assignmentTitle)
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].summary", containsString(assignmentTitle)));
    }

    private void register(String username, String password, String role) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", username,
                                "password", password,
                                "role", role
                        ))))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON));
    }

    private long createCourse(String token, String code, String name) throws Exception {
        JsonNode response = readJson(
                mockMvc.perform(post("/api/courses")
                                .header(AUTHORIZATION, bearer(token))
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of(
                                        "code", code,
                                        "name", name,
                                        "term", "2026 Spring",
                                        "className", "Software Engineering 1"
                                ))))
                        .andExpect(status().isOk())
                        .andReturn()
        );
        return response.get("id").asLong();
    }

    private void createAssignment(String token, long courseId, String title) throws Exception {
        mockMvc.perform(post("/api/assignments")
                        .header(AUTHORIZATION, bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", title,
                                "description", "Admin search assignment",
                                "deadline", LocalDateTime.now().plusDays(2).truncatedTo(ChronoUnit.SECONDS).toString(),
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

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
