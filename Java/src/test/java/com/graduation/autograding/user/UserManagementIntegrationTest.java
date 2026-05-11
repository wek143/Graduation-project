package com.graduation.autograding.user;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class UserManagementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void adminCanDisableAndEnableUserAndOldTokenBecomesInvalid() throws Exception {
        String adminToken = login("admin1", "123456");
        RegisteredUser teacher = register("teacher_disable_" + System.nanoTime(), "123456", "TEACHER");

        mockMvc.perform(put("/api/users/{userId}/status", teacher.id())
                        .header(AUTHORIZATION, bearer(adminToken))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("active", false))))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(teacher.id()))
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/api/auth/me")
                        .header(AUTHORIZATION, bearer(teacher.token())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("登录已过期，请重新登录。"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", teacher.username(),
                                "password", "123456"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("账号已被禁用，请联系管理员。"));

        mockMvc.perform(put("/api/users/{userId}/status", teacher.id())
                        .header(AUTHORIZATION, bearer(adminToken))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("active", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", teacher.username(),
                                "password", "123456"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(teacher.username()));
    }

    @Test
    void adminCanResetPasswordAndOldPasswordStopsWorking() throws Exception {
        String adminToken = login("admin1", "123456");
        RegisteredUser student = register("student_reset_" + System.nanoTime(), "123456", "STUDENT");

        mockMvc.perform(post("/api/users/{userId}/reset-password", student.id())
                        .header(AUTHORIZATION, bearer(adminToken))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("newPassword", "654321"))))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(student.id()))
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(get("/api/auth/me")
                        .header(AUTHORIZATION, bearer(student.token())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("登录已过期，请重新登录。"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", student.username(),
                                "password", "123456"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("密码错误。"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", student.username(),
                                "password", "654321"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(student.username()));
    }

    @Test
    void nonAdminCannotManageUserAccounts() throws Exception {
        String teacherToken = login("teacher1", "123456");
        RegisteredUser student = register("student_forbidden_" + System.nanoTime(), "123456", "STUDENT");

        mockMvc.perform(put("/api/users/{userId}/status", student.id())
                        .header(AUTHORIZATION, bearer(teacherToken))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("active", false))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("只有管理员可以访问该资源。"));

        mockMvc.perform(post("/api/users/{userId}/reset-password", student.id())
                        .header(AUTHORIZATION, bearer(teacherToken))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("newPassword", "654321"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("只有管理员可以访问该资源。"));
    }

    private RegisteredUser register(String username, String password, String role) throws Exception {
        JsonNode response = readJson(
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
        return new RegisteredUser(response.get("id").asLong(), username, response.get("token").asText());
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

    private record RegisteredUser(long id, String username, String token) {
    }
}
