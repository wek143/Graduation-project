package com.graduation.autograding.statistics;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import java.util.List;
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
class StatisticsExportIntegrationTest {

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
    void teacherCanViewCourseStatisticsAndExportAssignmentGrades() throws Exception {
        String teacherToken = login("teacher1", "123456");
        String studentToken = login("student1", "123456");
        long courseId = findFirstCourseId(teacherToken);
        String title = "Export Stats " + System.nanoTime();

        JsonNode assignment = createAssignment(teacherToken, courseId, title);
        long assignmentId = assignment.get("id").asLong();
        long submissionId = submitCode(studentToken, assignmentId);
        JsonNode finalSubmission = awaitSubmission(teacherToken, submissionId);
        assertTrue(finalSubmission.get("score").asInt() >= 0);

        mockMvc.perform(get("/api/courses/statistics/overview")
                        .header(AUTHORIZATION, bearer(teacherToken)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$[0].courseId").exists())
                .andExpect(jsonPath("$[0].assignmentCount").isNumber())
                .andExpect(jsonPath("$[0].totalSubmissions").isNumber())
                .andExpect(jsonPath("$[0].averageScore").isNumber());

        mockMvc.perform(get("/api/assignments/{assignmentId}/grades/export", assignmentId)
                        .header(AUTHORIZATION, bearer(teacherToken)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv;charset=UTF-8"))
                .andExpect(content().string(containsString("用户名,姓名,班级,状态,成绩,提交时间,提交次数,有效提交ID")))
                .andExpect(content().string(containsString("student1")))
                .andExpect(content().string(containsString("100")));
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

    private long findFirstCourseId(String token) throws Exception {
        JsonNode courses = readJson(
                mockMvc.perform(get("/api/courses").header(AUTHORIZATION, bearer(token)))
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                        .andReturn()
        );
        return StreamSupport.stream(courses.spliterator(), false)
                .filter(course -> "CS101".equals(course.get("code").asText()))
                .findFirst()
                .orElse(courses.get(0))
                .get("id")
                .asLong();
    }

    private JsonNode createAssignment(String teacherToken, long courseId, String title) throws Exception {
        return readJson(
                mockMvc.perform(post("/api/assignments")
                                .header(AUTHORIZATION, bearer(teacherToken))
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of(
                                        "title", title,
                                        "description", "Statistics export assignment",
                                        "deadline", LocalDateTime.now().plusDays(1).truncatedTo(ChronoUnit.SECONDS).toString(),
                                        "courseId", courseId,
                                        "status", "PUBLISHED",
                                        "maxSubmissions", 5,
                                        "lateSubmissionAllowed", false,
                                        "gradingPolicy", "LATEST",
                                        "testCases", List.of(
                                                Map.of("inputData", "1 2", "expectedOutput", "3"),
                                                Map.of("inputData", "4 5", "expectedOutput", "9")
                                        )
                                ))))
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                        .andReturn()
        );
    }

    private long submitCode(String studentToken, long assignmentId) throws Exception {
        JsonNode response = readJson(
                mockMvc.perform(post("/api/submissions")
                                .header(AUTHORIZATION, bearer(studentToken))
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of(
                                        "assignmentId", assignmentId,
                                        "sourceCode", ACCEPTED_SOURCE
                                ))))
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                        .andReturn()
        );
        return response.get("id").asLong();
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

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
