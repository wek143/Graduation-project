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
    name = "assignments",
    indexes = {
        @Index(name = "idx_assignments_teacher_id", columnList = "teacher_id"),
        @Index(name = "idx_assignments_course_id", columnList = "course_id")
    }
)
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(nullable = false)
    private LocalDateTime deadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AssignmentStatus status = AssignmentStatus.PUBLISHED;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AssignmentGradingPolicy gradingPolicy = AssignmentGradingPolicy.LATEST;

    @Column
    private Integer maxSubmissions = 5;

    @Column
    private Boolean lateSubmissionAllowed = false;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_id")
    private User teacher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    @OrderBy("id asc")
    @OneToMany(mappedBy = "assignment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TestCase> testCases = new ArrayList<>();

    @OrderBy("submittedAt desc")
    @OneToMany(mappedBy = "assignment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Submission> submissions = new ArrayList<>();

    public Assignment() {
    }

    public Assignment(String title, String description, LocalDateTime deadline, User teacher) {
        this.title = title;
        this.description = description;
        this.deadline = deadline;
        this.teacher = teacher;
    }

    public void addTestCase(TestCase testCase) {
        testCases.add(testCase);
        testCase.setAssignment(this);
    }

    public void removeTestCase(TestCase testCase) {
        testCases.remove(testCase);
        testCase.setAssignment(null);
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDateTime deadline) {
        this.deadline = deadline;
    }

    public AssignmentStatus getStatus() {
        return status;
    }

    public void setStatus(AssignmentStatus status) {
        this.status = status;
    }

    public AssignmentGradingPolicy getGradingPolicy() {
        return gradingPolicy == null ? AssignmentGradingPolicy.LATEST : gradingPolicy;
    }

    public void setGradingPolicy(AssignmentGradingPolicy gradingPolicy) {
        this.gradingPolicy = gradingPolicy;
    }

    public Integer getMaxSubmissions() {
        return maxSubmissions == null ? 5 : maxSubmissions;
    }

    public void setMaxSubmissions(Integer maxSubmissions) {
        this.maxSubmissions = maxSubmissions;
    }

    public boolean isLateSubmissionAllowed() {
        return Boolean.TRUE.equals(lateSubmissionAllowed);
    }

    public void setLateSubmissionAllowed(boolean lateSubmissionAllowed) {
        this.lateSubmissionAllowed = lateSubmissionAllowed;
    }

    public User getTeacher() {
        return teacher;
    }

    public void setTeacher(User teacher) {
        this.teacher = teacher;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public List<TestCase> getTestCases() {
        return testCases;
    }

    public List<Submission> getSubmissions() {
        return submissions;
    }
}
