package com.graduation.autograding.repository;

import com.graduation.autograding.domain.Assignment;
import com.graduation.autograding.domain.AssignmentStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    @Override
    @EntityGraph(attributePaths = {"teacher", "testCases", "course"})
    java.util.List<Assignment> findAll();

    @Override
    @EntityGraph(attributePaths = {"teacher", "testCases", "course"})
    Optional<Assignment> findById(Long id);

    @EntityGraph(attributePaths = {"teacher", "testCases", "course"})
    java.util.List<Assignment> findByStatusOrderByDeadlineAsc(AssignmentStatus status);

    @EntityGraph(attributePaths = {"teacher", "testCases", "course"})
    java.util.List<Assignment> findByTeacherIdOrderByDeadlineAsc(Long teacherId);

    @Query("""
            select distinct a from Assignment a
            join CourseEnrollment e on e.course.id = a.course.id
            where e.student.id = :studentId and a.status = :status
            order by a.deadline asc
            """)
    @EntityGraph(attributePaths = {"teacher", "testCases", "course"})
    java.util.List<Assignment> findPublishedAssignmentsForStudent(
            @Param("studentId") Long studentId,
            @Param("status") AssignmentStatus status
    );

    long countByCourseId(Long courseId);

    long countByStatus(AssignmentStatus status);

    @EntityGraph(attributePaths = {"teacher", "course"})
    @Query(value = """
            select a from Assignment a
            join a.teacher t
            left join a.course c
            where :keyword is null
               or lower(a.title) like lower(concat('%', :keyword, '%'))
               or lower(a.description) like lower(concat('%', :keyword, '%'))
               or lower(t.username) like lower(concat('%', :keyword, '%'))
               or lower(coalesce(t.fullName, '')) like lower(concat('%', :keyword, '%'))
               or lower(coalesce(c.code, '')) like lower(concat('%', :keyword, '%'))
               or lower(coalesce(c.name, '')) like lower(concat('%', :keyword, '%'))
            """,
            countQuery = """
                    select count(a) from Assignment a
                    join a.teacher t
                    left join a.course c
                    where :keyword is null
                       or lower(a.title) like lower(concat('%', :keyword, '%'))
                       or lower(a.description) like lower(concat('%', :keyword, '%'))
                       or lower(t.username) like lower(concat('%', :keyword, '%'))
                       or lower(coalesce(t.fullName, '')) like lower(concat('%', :keyword, '%'))
                       or lower(coalesce(c.code, '')) like lower(concat('%', :keyword, '%'))
                       or lower(coalesce(c.name, '')) like lower(concat('%', :keyword, '%'))
                    """)
    Page<Assignment> searchForAdmin(@Param("keyword") String keyword, Pageable pageable);
}
