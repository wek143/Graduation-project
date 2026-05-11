package com.graduation.autograding.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "submissions",
    indexes = {
        @Index(name = "idx_submissions_assignment_id", columnList = "assignment_id"),
        @Index(name = "idx_submissions_student_id", columnList = "student_id"),
        @Index(name = "idx_submissions_assignment_student", columnList = "assignment_id, student_id")
    }
)
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignment_id")
    private Assignment assignment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id")
    private User student;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String sourceCode;

    @Column(nullable = false, length = 100)
    private String className;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SubmissionStatus status = SubmissionStatus.PENDING;

    @Column(nullable = false)
    private Integer score = 0;

    @Column(columnDefinition = "LONGTEXT")
    private String compileMessage;

    @Column(columnDefinition = "LONGTEXT")
    private String runtimeMessage;

    @Column(nullable = false)
    private LocalDateTime submittedAt = LocalDateTime.now();

    @OrderBy("id asc")
    @OneToMany(mappedBy = "submission", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JudgeCaseResult> caseResults = new ArrayList<>();

    public Submission() {
    }

    public Submission(Assignment assignment, User student, String sourceCode, String className) {
        this.assignment = assignment;
        this.student = student;
        this.sourceCode = sourceCode;
        this.className = className;
    }

    public void addCaseResult(JudgeCaseResult caseResult) {
        caseResults.add(caseResult);
        caseResult.setSubmission(this);
    }

    public void clearCaseResults() {
        caseResults.clear();
    }

    public Long getId() {
        return id;
    }

    public Assignment getAssignment() {
        return assignment;
    }

    public User getStudent() {
        return student;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public SubmissionStatus getStatus() {
        return status;
    }

    public void setStatus(SubmissionStatus status) {
        this.status = status;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public String getCompileMessage() {
        return compileMessage;
    }

    public void setCompileMessage(String compileMessage) {
        this.compileMessage = compileMessage;
    }

    public String getRuntimeMessage() {
        return runtimeMessage;
    }

    public void setRuntimeMessage(String runtimeMessage) {
        this.runtimeMessage = runtimeMessage;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public List<JudgeCaseResult> getCaseResults() {
        return caseResults;
    }
}
