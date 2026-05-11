package com.graduation.autograding.flow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
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
import com.graduation.autograding.domain.Assignment;
import com.graduation.autograding.repository.AssignmentRepository;
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
class AssignmentSubmissionFlowIntegrationTest {

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

    private static final String COMPILE_ERROR_SOURCE = """
            public class Main {
                public static void main(String[] args) {
                    System.out.println("broken")
                }
            }
            """;

    private static final String PARTIAL_60_SOURCE = """
            public class Main {
                public static void main(String[] args) {
                    int value = new java.util.Scanner(System.in).nextInt();
                    System.out.println(Math.min(value, 3));
                }
            }
            """;

    private static final String PARTIAL_80_SOURCE = """
            public class Main {
                public static void main(String[] args) {
                    int value = new java.util.Scanner(System.in).nextInt();
                    System.out.println(Math.min(value, 4));
                }
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Test
    void teacherCreatesAssignmentAndStudentCompletesAcceptedSubmissionFlow() throws Exception {
        SessionInfo teacher = login("teacher1", "123456");
        SessionInfo student = login("student1", "123456");
        long courseId = findFirstCourseId(teacher.token());

        JsonNode assignment = createAssignment(teacher.token(), courseId, "Accepted Flow " + System.nanoTime());
        long assignmentId = assignment.get("id").asLong();

        JsonNode publishedAssignments = readJson(
                mockMvc.perform(get("/api/assignments/published").header(AUTHORIZATION, bearer(student.token())))
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                        .andReturn()
        );
        assertTrue(containsId(publishedAssignments, assignmentId));
        assertEquals(0, findById(publishedAssignments, assignmentId).get("testCases").size());

        JsonNode studentAssignmentDetail = readJson(
                mockMvc.perform(get("/api/assignments/{assignmentId}", assignmentId)
                                .header(AUTHORIZATION, bearer(student.token())))
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                        .andReturn()
        );
        assertEquals(0, studentAssignmentDetail.get("testCases").size());

        JsonNode createdSubmission = submitCode(student.token(), assignmentId, ACCEPTED_SOURCE);
        long submissionId = createdSubmission.get("id").asLong();

        JsonNode finalSubmission = awaitSubmission(student.token(), submissionId);
        assertEquals("ACCEPTED", finalSubmission.get("status").asText());
        assertEquals(100, finalSubmission.get("score").asInt());
        assertTrue(!finalSubmission.get("compileMessage").asText().isBlank());
        assertTrue(!finalSubmission.get("runtimeMessage").asText().isBlank());
        assertEquals(2, finalSubmission.get("caseResults").size());

        JsonNode teacherSubmissions = readJson(
                mockMvc.perform(get("/api/submissions/assignment/{assignmentId}", assignmentId)
                                .header(AUTHORIZATION, bearer(teacher.token())))
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                        .andReturn()
        );
        assertTrue(containsId(teacherSubmissions, submissionId));

        JsonNode latestSubmission = readJson(
                mockMvc.perform(get("/api/submissions/assignment/{assignmentId}/student/{studentId}/latest",
                                assignmentId, student.id())
                                .header(AUTHORIZATION, bearer(teacher.token())))
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                        .andReturn()
        );
        assertEquals(submissionId, latestSubmission.get("id").asLong());
        assertEquals("ACCEPTED", latestSubmission.get("status").asText());

        JsonNode latestSummaries = readJson(
                mockMvc.perform(get("/api/submissions/student/{studentId}/latest", student.id())
                                .header(AUTHORIZATION, bearer(student.token())))
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                        .andReturn()
        );
        JsonNode summary = findByAssignmentId(latestSummaries, assignmentId);
        assertEquals(submissionId, summary.get("submissionId").asLong());
        assertEquals("ACCEPTED", summary.get("status").asText());
        assertEquals(100, summary.get("score").asInt());
    }

    @Test
    void compileErrorSubmissionReturnsCompileError() throws Exception {
        SessionInfo teacher = login("teacher1", "123456");
        SessionInfo student = login("student1", "123456");
        long courseId = findFirstCourseId(teacher.token());

        JsonNode assignment = createAssignment(teacher.token(), courseId, "Compile Error Flow " + System.nanoTime());
        long assignmentId = assignment.get("id").asLong();

        JsonNode createdSubmission = submitCode(student.token(), assignmentId, COMPILE_ERROR_SOURCE);
        JsonNode finalSubmission = awaitSubmission(student.token(), createdSubmission.get("id").asLong());

        assertEquals("COMPILE_ERROR", finalSubmission.get("status").asText());
        assertEquals(0, finalSubmission.get("score").asInt());
        assertTrue(!finalSubmission.get("compileMessage").asText().isBlank());
        assertTrue(!finalSubmission.get("runtimeMessage").asText().isBlank());
    }

    @Test
    void teacherCanRejudgeSubmissionAfterTestCaseChanges() throws Exception {
        SessionInfo teacher = login("teacher1", "123456");
        SessionInfo student = login("student1", "123456");
        long courseId = findFirstCourseId(teacher.token());

        JsonNode assignment = createAssignment(teacher.token(), courseId, "Rejudge Flow " + System.nanoTime());
        long assignmentId = assignment.get("id").asLong();
        long firstTestCaseId = assignment.get("testCases").get(0).get("id").asLong();

        JsonNode createdSubmission = submitCode(student.token(), assignmentId, ACCEPTED_SOURCE);
        long submissionId = createdSubmission.get("id").asLong();

        JsonNode acceptedSubmission = awaitSubmission(teacher.token(), submissionId);
        assertEquals("ACCEPTED", acceptedSubmission.get("status").asText());
        assertEquals(100, acceptedSubmission.get("score").asInt());

        updateTestCase(teacher.token(), assignmentId, firstTestCaseId, "1 2", "100");

        JsonNode rejudgingSubmission = readJson(
                mockMvc.perform(post("/api/submissions/{submissionId}/rejudge", submissionId)
                                .header(AUTHORIZATION, bearer(teacher.token())))
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                        .andExpect(jsonPath("$.id").value(submissionId))
                        .andExpect(jsonPath("$.status").value("PENDING"))
                        .andReturn()
        );
        assertEquals("PENDING", rejudgingSubmission.get("status").asText());

        JsonNode finalSubmission = awaitSubmission(teacher.token(), submissionId);
        assertEquals("PARTIAL_ACCEPTED", finalSubmission.get("status").asText());
        assertEquals(50, finalSubmission.get("score").asInt());
        assertEquals(2, finalSubmission.get("caseResults").size());
        assertEquals("100", finalSubmission.get("caseResults").get(0).get("expectedOutput").asText());
    }

    @Test
    void studentCannotRejudgeSubmission() throws Exception {
        SessionInfo teacher = login("teacher1", "123456");
        SessionInfo student = login("student1", "123456");
        long courseId = findFirstCourseId(teacher.token());

        JsonNode assignment = createAssignment(teacher.token(), courseId, "Forbidden Rejudge " + System.nanoTime());
        JsonNode createdSubmission = submitCode(student.token(), assignment.get("id").asLong(), ACCEPTED_SOURCE);
        long submissionId = awaitSubmission(student.token(), createdSubmission.get("id").asLong()).get("id").asLong();

        mockMvc.perform(post("/api/submissions/{submissionId}/rejudge", submissionId)
                        .header(AUTHORIZATION, bearer(student.token())))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("只有教师或管理员可以重新判题。"));
    }

    @Test
    void teacherOnlySeesStudentSubmissionsForOwnedAssignments() throws Exception {
        SessionInfo teacher1 = login("teacher1", "123456");
        SessionInfo teacher2 = register("teacher_scope_" + System.nanoTime(), "123456", "TEACHER");
        SessionInfo student2 = register("student_scope_" + System.nanoTime(), "123456", "STUDENT");
        long courseId = createCourse(teacher2.token(), "SCP" + System.nanoTime(), "Scoped Course");

        enrollStudent(teacher2.token(), courseId, student2.id());
        JsonNode assignment = createAssignment(teacher2.token(), courseId, "Scoped Assignment " + System.nanoTime());
        long assignmentId = assignment.get("id").asLong();

        JsonNode createdSubmission = submitCode(student2.token(), assignmentId, ACCEPTED_SOURCE);
        long submissionId = createdSubmission.get("id").asLong();
        awaitSubmission(teacher2.token(), submissionId);

        JsonNode teacher2StudentSubmissions = readJson(
                mockMvc.perform(get("/api/submissions/student/{studentId}", student2.id())
                                .header(AUTHORIZATION, bearer(teacher2.token())))
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                        .andReturn()
        );
        assertEquals(1, teacher2StudentSubmissions.size());
        assertEquals(submissionId, teacher2StudentSubmissions.get(0).get("id").asLong());

        JsonNode teacher1StudentSubmissions = readJson(
                mockMvc.perform(get("/api/submissions/student/{studentId}", student2.id())
                                .header(AUTHORIZATION, bearer(teacher1.token())))
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                        .andReturn()
        );
        assertEquals(0, teacher1StudentSubmissions.size());

        JsonNode teacher1LatestSummaries = readJson(
                mockMvc.perform(get("/api/submissions/student/{studentId}/latest", student2.id())
                                .header(AUTHORIZATION, bearer(teacher1.token())))
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                        .andReturn()
        );
        assertEquals(0, teacher1LatestSummaries.size());
    }

    @Test
    void highestGradingPolicyUsesBestSubmissionForStatisticsAndExport() throws Exception {
        SessionInfo teacher = login("teacher1", "123456");
        SessionInfo student = login("student1", "123456");
        long courseId = findFirstCourseId(teacher.token());

        JsonNode assignment = createAssignment(
                teacher.token(),
                courseId,
                "Highest Policy " + System.nanoTime(),
                5,
                false,
                "HIGHEST",
                List.of(
                        Map.of("inputData", "1", "expectedOutput", "1"),
                        Map.of("inputData", "2", "expectedOutput", "2"),
                        Map.of("inputData", "3", "expectedOutput", "3"),
                        Map.of("inputData", "4", "expectedOutput", "4"),
                        Map.of("inputData", "5", "expectedOutput", "5")
                )
        );
        long assignmentId = assignment.get("id").asLong();

        long firstSubmissionId = awaitSubmission(
                student.token(),
                submitCode(student.token(), assignmentId, PARTIAL_60_SOURCE).get("id").asLong()
        ).get("id").asLong();

        JsonNode secondSubmission = awaitSubmission(
                student.token(),
                submitCode(student.token(), assignmentId, PARTIAL_80_SOURCE).get("id").asLong()
        );
        long secondSubmissionId = secondSubmission.get("id").asLong();
        assertEquals(80, secondSubmission.get("score").asInt());

        JsonNode statistics = readJson(
                mockMvc.perform(get("/api/assignments/statistics/overview")
                                .header(AUTHORIZATION, bearer(teacher.token())))
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                        .andReturn()
        );
        JsonNode assignmentStats = findByAssignmentId(statistics, assignmentId);
        assertEquals(80.0, assignmentStats.get("averageScore").asDouble(), 0.001);

        String csvContent = mockMvc.perform(get("/api/assignments/{assignmentId}/grades/export", assignmentId)
                        .header(AUTHORIZATION, bearer(teacher.token())))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv;charset=UTF-8"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertTrue(csvContent.contains("\"" + secondSubmissionId + "\""));
        assertTrue(!csvContent.contains("\"" + firstSubmissionId + "\""));
    }

    @Test
    void expiredAssignmentsRespectLateSubmissionSetting() throws Exception {
        SessionInfo teacher = login("teacher1", "123456");
        SessionInfo student = login("student1", "123456");
        long courseId = findFirstCourseId(teacher.token());

        JsonNode blockedAssignment = createAssignment(
                teacher.token(),
                courseId,
                "Expired Blocked " + System.nanoTime(),
                5,
                false,
                "LATEST",
                defaultTestCases()
        );
        setAssignmentDeadlineAndLatePolicy(blockedAssignment.get("id").asLong(), LocalDateTime.now().minusMinutes(5), false);

        JsonNode blockedResponse = readJson(
                mockMvc.perform(post("/api/submissions")
                                .header(AUTHORIZATION, bearer(student.token()))
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of(
                                        "assignmentId", blockedAssignment.get("id").asLong(),
                                        "sourceCode", ACCEPTED_SOURCE
                                ))))
                        .andExpect(status().isBadRequest())
                        .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                        .andReturn()
        );
        assertNotNull(blockedResponse.get("message"));
        assertTrue(!blockedResponse.get("message").asText().isBlank());

        JsonNode allowedAssignment = createAssignment(
                teacher.token(),
                courseId,
                "Expired Allowed " + System.nanoTime(),
                5,
                false,
                "LATEST",
                defaultTestCases()
        );
        setAssignmentDeadlineAndLatePolicy(allowedAssignment.get("id").asLong(), LocalDateTime.now().minusMinutes(5), true);

        JsonNode createdSubmission = submitCode(student.token(), allowedAssignment.get("id").asLong(), ACCEPTED_SOURCE);
        JsonNode finalSubmission = awaitSubmission(student.token(), createdSubmission.get("id").asLong());
        assertEquals("ACCEPTED", finalSubmission.get("status").asText());
    }

    @Test
    void maxSubmissionsLimitRejectsSecondAttemptWhenLimitIsOne() throws Exception {
        SessionInfo teacher = login("teacher1", "123456");
        SessionInfo student = login("student1", "123456");
        long courseId = findFirstCourseId(teacher.token());

        JsonNode assignment = createAssignment(
                teacher.token(),
                courseId,
                "Max Submissions " + System.nanoTime(),
                1,
                false,
                "LATEST",
                defaultTestCases()
        );
        long assignmentId = assignment.get("id").asLong();

        awaitSubmission(student.token(), submitCode(student.token(), assignmentId, ACCEPTED_SOURCE).get("id").asLong());
        JsonNode rejectedResponse = readJson(
                mockMvc.perform(post("/api/submissions")
                                .header(AUTHORIZATION, bearer(student.token()))
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of(
                                        "assignmentId", assignmentId,
                                        "sourceCode", ACCEPTED_SOURCE
                                ))))
                        .andExpect(status().isBadRequest())
                        .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                        .andReturn()
        );
        assertNotNull(rejectedResponse.get("message"));
        assertTrue(!rejectedResponse.get("message").asText().isBlank());
    }

    @Test
    void teacherCannotSubmitStudentCode() throws Exception {
        SessionInfo teacher = login("teacher1", "123456");
        long courseId = findFirstCourseId(teacher.token());
        JsonNode assignment = createAssignment(teacher.token(), courseId, "Forbidden Submit " + System.nanoTime());

        mockMvc.perform(post("/api/submissions")
                        .header(AUTHORIZATION, bearer(teacher.token()))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "assignmentId", assignment.get("id").asLong(),
                                "sourceCode", ACCEPTED_SOURCE
                        ))))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("只有学生可以提交作业。"));
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
        return createAssignment(teacherToken, courseId, title, 5, false, "LATEST", defaultTestCases());
    }

    private JsonNode createAssignment(String teacherToken, long courseId, String title, int maxSubmissions,
                                      boolean lateSubmissionAllowed, String gradingPolicy,
                                      List<Map<String, String>> testCases) throws Exception {
        Map<String, Object> request = Map.of(
                "title", title,
                "description", "Add two integers",
                "deadline", LocalDateTime.now().plusDays(1).truncatedTo(ChronoUnit.SECONDS).toString(),
                "courseId", courseId,
                "status", "PUBLISHED",
                "maxSubmissions", maxSubmissions,
                "lateSubmissionAllowed", lateSubmissionAllowed,
                "gradingPolicy", gradingPolicy,
                "testCases", testCases
        );
        return readJson(
                mockMvc.perform(post("/api/assignments")
                                .header(AUTHORIZATION, bearer(teacherToken))
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                        .andReturn()
        );
    }

    private long createCourse(String teacherToken, String code, String name) throws Exception {
        JsonNode course = readJson(
                mockMvc.perform(post("/api/courses")
                                .header(AUTHORIZATION, bearer(teacherToken))
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of(
                                        "code", code,
                                        "name", name,
                                        "term", "2026 Spring",
                                        "className", "Software Engineering 1"
                                ))))
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                        .andReturn()
        );
        return course.get("id").asLong();
    }

    private void enrollStudent(String teacherToken, long courseId, long studentId) throws Exception {
        mockMvc.perform(post("/api/courses/{courseId}/enrollments/{studentId}", courseId, studentId)
                        .header(AUTHORIZATION, bearer(teacherToken)))
                .andExpect(status().isOk());
    }

    private JsonNode submitCode(String studentToken, long assignmentId, String sourceCode) throws Exception {
        return readJson(
                mockMvc.perform(post("/api/submissions")
                                .header(AUTHORIZATION, bearer(studentToken))
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of(
                                        "assignmentId", assignmentId,
                                        "sourceCode", sourceCode
                                ))))
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                        .andReturn()
        );
    }

    private void updateTestCase(String teacherToken, long assignmentId, long testCaseId,
                                String inputData, String expectedOutput) throws Exception {
        mockMvc.perform(put("/api/assignments/{assignmentId}/test-cases/{testCaseId}", assignmentId, testCaseId)
                        .header(AUTHORIZATION, bearer(teacherToken))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "inputData", inputData,
                                "expectedOutput", expectedOutput
                        ))))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON));
    }

    private void setAssignmentDeadlineAndLatePolicy(long assignmentId, LocalDateTime deadline,
                                                    boolean lateSubmissionAllowed) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new AssertionError("Assignment was not found."));
        assignment.setDeadline(deadline);
        assignment.setLateSubmissionAllowed(lateSubmissionAllowed);
        assignmentRepository.save(assignment);
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
        fail("Submission did not finish within the expected time.");
        return null;
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private boolean containsId(JsonNode items, long expectedId) {
        return StreamSupport.stream(items.spliterator(), false)
                .anyMatch(item -> item.get("id").asLong() == expectedId);
    }

    private JsonNode findById(JsonNode items, long expectedId) {
        return StreamSupport.stream(items.spliterator(), false)
                .filter(item -> item.get("id").asLong() == expectedId)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Assignment was not found."));
    }

    private JsonNode findByAssignmentId(JsonNode items, long assignmentId) {
        return StreamSupport.stream(items.spliterator(), false)
                .filter(item -> item.get("assignmentId").asLong() == assignmentId)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Assignment summary was not found."));
    }

    private List<Map<String, String>> defaultTestCases() {
        return List.of(
                Map.of("inputData", "1 2", "expectedOutput", "3"),
                Map.of("inputData", "4 5", "expectedOutput", "9")
        );
    }

    private String bearer(String token) {
        return "Bearer " + token;
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

    private record SessionInfo(long id, String token) {
    }
}
