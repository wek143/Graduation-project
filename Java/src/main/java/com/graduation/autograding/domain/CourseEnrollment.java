package com.graduation.autograding.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "course_enrollments",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_course_enrollments_course_student",
        columnNames = {"course_id", "student_id"}
    ),
    indexes = {
        @Index(name = "idx_course_enrollments_course_id", columnList = "course_id"),
        @Index(name = "idx_course_enrollments_student_id", columnList = "student_id")
    }
)
public class CourseEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id")
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id")
    private User student;

    @Column(nullable = false)
    private LocalDateTime enrolledAt = LocalDateTime.now();

    public CourseEnrollment() {
    }

    public CourseEnrollment(Course course, User student) {
        this.course = course;
        this.student = student;
    }

    public Long getId() {
        return id;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public User getStudent() {
        return student;
    }

    public LocalDateTime getEnrolledAt() {
        return enrolledAt;
    }
}
