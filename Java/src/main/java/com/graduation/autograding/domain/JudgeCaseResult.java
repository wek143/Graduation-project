package com.graduation.autograding.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "judge_case_results")
public class JudgeCaseResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer caseOrder;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String inputData;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String expectedOutput;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String actualOutput;

    @Column(nullable = false)
    private boolean passed;

    @Column(columnDefinition = "LONGTEXT")
    private String errorMessage;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id")
    private Submission submission;

    public JudgeCaseResult() {
    }

    public JudgeCaseResult(Integer caseOrder, String inputData, String expectedOutput, String actualOutput,
                           boolean passed, String errorMessage) {
        this.caseOrder = caseOrder;
        this.inputData = inputData;
        this.expectedOutput = expectedOutput;
        this.actualOutput = actualOutput;
        this.passed = passed;
        this.errorMessage = errorMessage;
    }

    public Long getId() {
        return id;
    }

    public Integer getCaseOrder() {
        return caseOrder;
    }

    public String getInputData() {
        return inputData;
    }

    public String getExpectedOutput() {
        return expectedOutput;
    }

    public String getActualOutput() {
        return actualOutput;
    }

    public boolean isPassed() {
        return passed;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Submission getSubmission() {
        return submission;
    }

    public void setSubmission(Submission submission) {
        this.submission = submission;
    }
}
