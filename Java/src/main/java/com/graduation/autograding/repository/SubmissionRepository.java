package com.graduation.autograding.repository;

import com.graduation.autograding.domain.Submission;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    @Override
    @EntityGraph(attributePaths = {"assignment", "student", "caseResults"})
    Optional<Submission> findById(Long id);

    @EntityGraph(attributePaths = {"assignment", "student", "caseResults"})
    List<Submission> findByStudentIdOrderBySubmittedAtDesc(Long studentId);

    @EntityGraph(attributePaths = {"assignment", "student", "caseResults"})
    List<Submission> findByAssignmentIdOrderBySubmittedAtDesc(Long assignmentId);

    @EntityGraph(attributePaths = {"assignment", "student", "caseResults"})
    Optional<Submission> findFirstByAssignmentIdAndStudentIdOrderBySubmittedAtDesc(Long assignmentId, Long studentId);

    long countByAssignmentId(Long assignmentId);

    long countByAssignmentIdAndStudentId(Long assignmentId, Long studentId);

    long countByStudentId(Long studentId);

    @Query("select count(distinct s.student.id) from Submission s where s.assignment.id = :assignmentId")
    long countDistinctStudentsByAssignmentId(@Param("assignmentId") Long assignmentId);

    /**
     * 批量查询多个作业的提交总数与提交学生数，避免 N+1。
     * 返回 Object[]{assignmentId, totalCount, distinctStudentCount}
     */
    @Query("""
            select s.assignment.id, count(s.id), count(distinct s.student.id)
            from Submission s
            where s.assignment.id in :assignmentIds
            group by s.assignment.id
            """)
    List<Object[]> findSubmissionCountsByAssignmentIds(@Param("assignmentIds") List<Long> assignmentIds);

    /**
     * 批量查询多个作业下所有提交的轻量投影（不含 sourceCode / caseResults），
     * 用于统计平均分，避免加载大字段。
     * 返回 Object[]{assignmentId, studentId, score, submittedAt}
     */
    @Query("""
            select s.assignment.id, s.student.id, s.score, s.submittedAt
            from Submission s
            where s.assignment.id in :assignmentIds
            order by s.submittedAt desc
            """)
    List<Object[]> findScoreProjectionsByAssignmentIds(@Param("assignmentIds") List<Long> assignmentIds);
}
