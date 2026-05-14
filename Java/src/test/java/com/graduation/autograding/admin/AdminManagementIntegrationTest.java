package com.graduation.autograding.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import com.graduation.autograding.repository.AiSettingsRepository;
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
class AdminManagementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AiSettingsRepository aiSettingsRepository;

    @Test
    void adminCanSearchAssignmentsAndUsers() throws Exception {
        String adminToken = login("admin1", "123456");
        String teacherToken = login("teacher1", "123456");
        String assignmentTitle = "AdminMgmt_" + System.nanoTime();
        String userPrefix = "admin_mgmt_user_" + System.nanoTime();
        long courseId = createCourse(teacherToken, "ADM2" + System.nanoTime(), "Admin Management Course");

        createAssignment(teacherToken, courseId, assignmentTitle);
        register(userPrefix + "_student", "123456", "STUDENT");
        register(userPrefix + "_teacher", "123456", "TEACHER");

                mockMvc.perform(get("/api/admin/assignments")
                        .header(AUTHORIZATION, bearer(adminToken))
                        .param("keyword", assignmentTitle)
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value(assignmentTitle));

        mockMvc.perform(get("/api/admin/users")
                        .header(AUTHORIZATION, bearer(adminToken))
                        .param("keyword", userPrefix)
                        .param("role", "STUDENT")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].role").value("STUDENT"))
                .andExpect(jsonPath("$.content[0].username", containsString(userPrefix)));
    }

    @Test
    void nonAdminCannotAccessAdminManagementEndpoints() throws Exception {
        String teacherToken = login("teacher1", "123456");

        mockMvc.perform(get("/api/admin/assignments")
                        .header(AUTHORIZATION, bearer(teacherToken)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON));

        mockMvc.perform(get("/api/admin/users")
                        .header(AUTHORIZATION, bearer(teacherToken)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON));

        mockMvc.perform(get("/api/admin/ai-settings")
                        .header(AUTHORIZATION, bearer(teacherToken)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON));
    }

    @Test
    void adminCanReadAndUpdateAiSettings() throws Exception {
        String adminToken = login("admin1", "123456");

        mockMvc.perform(get("/api/admin/ai-settings")
                        .header(AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.enabled").exists())
                .andExpect(jsonPath("$.baseUrl").exists())
                .andExpect(jsonPath("$.model").exists())
                .andExpect(jsonPath("$.timeoutSeconds").exists())
                .andExpect(jsonPath("$.apiKeyConfigured").exists());

        mockMvc.perform(post("/api/admin/ai-settings")
                        .header(AUTHORIZATION, bearer(adminToken))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "enabled", true,
                                "baseUrl", "https://api.deepseek.com",
                                "model", "deepseek-chat",
                                "timeoutSeconds", 25,
                                "apiKey", "sk-demo-admin-config"
                        ))))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.baseUrl").value("https://api.deepseek.com"))
                .andExpect(jsonPath("$.model").value("deepseek-chat"))
                .andExpect(jsonPath("$.timeoutSeconds").value(25))
                .andExpect(jsonPath("$.apiKeyConfigured").value(true));

        var savedSettings = aiSettingsRepository.findById(1L).orElseThrow();
        assertTrue(savedSettings.isEnabled());
        assertEquals("https://api.deepseek.com", savedSettings.getBaseUrl());
        assertEquals("deepseek-chat", savedSettings.getModel());
        assertEquals(25, savedSettings.getTimeoutSeconds());
        assertEquals("sk-demo-admin-config", savedSettings.getApiKey());
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
                                "description", "Admin management assignment",
                                "deadline", LocalDateTime.now().plusDays(2).truncatedTo(ChronoUnit.SECONDS).toString(),
                                "courseId", courseId,
                                "status", "PUBLISHED",
                                "maxSubmissions", 5,
                                "lateSubmissionAllowed", false,
                                "gradingPolicy", "LATEST"
                        ))))
                .andExpect(status().isOk());
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
