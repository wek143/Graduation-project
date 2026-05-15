package com.graduation.autograding.ai;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class AiDiagnosisIntegrationTest {

    private static final String ACCEPTED_SOURCE = """
            public class Main {
                public static void main(String[] args) {
                    java.util.Scanner scanner = new java.util.Scanner(System.in);
                    int a = scanner.nextInt();
                    int b = scanner.nextInt();
                    System.out.println(a + b);
                }
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void aiDiagnosisReturnsBadRequestWhenApiKeyMissing() throws Exception {
        SessionInfo student = login("student1", "123456");
        SessionInfo admin = login("admin1", "123456");
        long assignmentId = 1L;

        mockMvc.perform(post("/api/admin/ai-settings")
                        .header(AUTHORIZATION, bearer(admin.token()))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "enabled", true,
                                "baseUrl", "https://api.deepseek.com",
                                "model", "deepseek-chat",
                                "timeoutSeconds", 20,
                                "apiKey", ""
                        ))))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON));

        JsonNode submission = readJson(
                mockMvc.perform(post("/api/submissions")
                                .header(AUTHORIZATION, bearer(student.token()))
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of(
                                        "assignmentId", assignmentId,
                                        "sourceCode", ACCEPTED_SOURCE
                                ))))
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                        .andReturn()
        );

        long submissionId = submission.get("id").asLong();

        awaitSubmission(student.token(), submissionId);

        mockMvc.perform(post("/api/submissions/{submissionId}/ai-diagnosis", submissionId)
                        .header(AUTHORIZATION, bearer(student.token())))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("AI 服务未正确配置 API Key。"));
    }

    @Test
    void studentCannotDiagnoseAnotherStudentsSubmission() throws Exception {
        SessionInfo teacher = login("teacher1", "123456");
        SessionInfo student1 = login("student1", "123456");
        SessionInfo student2 = register("student_ai_" + System.nanoTime(), "123456", "STUDENT");

        long courseId = 1L;
        enrollStudent(teacher.token(), courseId, student2.id());
        long assignmentId = 1L;

        JsonNode submission = readJson(
                mockMvc.perform(post("/api/submissions")
                                .header(AUTHORIZATION, bearer(student2.token()))
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of(
                                        "assignmentId", assignmentId,
                                        "sourceCode", ACCEPTED_SOURCE
                                ))))
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                        .andReturn()
        );

        mockMvc.perform(post("/api/submissions/{submissionId}/ai-diagnosis", submission.get("id").asLong())
                        .header(AUTHORIZATION, bearer(student1.token())))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("学生只能分析自己的提交记录。"));
    }

    @Test
    void teacherCannotDiagnoseSubmission() throws Exception {
        SessionInfo teacher = login("teacher1", "123456");
        SessionInfo student = login("student1", "123456");
        long assignmentId = 1L;

        JsonNode submission = readJson(
                mockMvc.perform(post("/api/submissions")
                                .header(AUTHORIZATION, bearer(student.token()))
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of(
                                        "assignmentId", assignmentId,
                                        "sourceCode", ACCEPTED_SOURCE
                                ))))
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                        .andReturn()
        );

        mockMvc.perform(post("/api/submissions/{submissionId}/ai-diagnosis", submission.get("id").asLong())
                        .header(AUTHORIZATION, bearer(teacher.token())))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("只有学生可以发起 AI 辅助分析。"));
    }

    private SessionInfo login(String username, String password) throws Exception {
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
        return new SessionInfo(response.get("id").asLong(), response.get("token").asText());
    }

    private SessionInfo register(String username, String password, String role) throws Exception {
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
        return new SessionInfo(response.get("id").asLong(), response.get("token").asText());
    }

    private void enrollStudent(String teacherToken, long courseId, long studentId) throws Exception {
        mockMvc.perform(post("/api/courses/{courseId}/enrollments/{studentId}", courseId, studentId)
                        .header(AUTHORIZATION, bearer(teacherToken)))
                .andExpect(status().isOk());
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode awaitSubmission(String token, long submissionId) throws Exception {
        for (int attempt = 0; attempt < 40; attempt++) {
            JsonNode submission = readJson(
                    mockMvc.perform(get("/api/submissions/{submissionId}", submissionId)
                                    .header(AUTHORIZATION, bearer(token)))
                            .andExpect(status().isOk())
                            .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                            .andReturn()
            );
            if (!"PENDING".equals(submission.get("status").asText())) {
                return submission;
            }
            Thread.sleep(150);
        }
        throw new AssertionError("Submission did not finish within the expected time.");
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private record SessionInfo(long id, String token) {
    }
}
