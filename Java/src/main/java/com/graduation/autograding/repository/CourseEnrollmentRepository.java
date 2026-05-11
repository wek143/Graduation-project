package com.graduation.autograding.repository;

import com.graduation.autograding.domain.CourseEnrollment;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseEnrollmentRepository extends JpaRepository<CourseEnrollment, Long> {

    boolean existsByCourseIdAndStudentId(Long courseId, Long studentId);

    @EntityGraph(attributePaths = {"course", "student"})
    List<CourseEnrollment> findByCourseIdOrderByEnrolledAtDesc(Long courseId);

    @EntityGraph(attributePaths = {"course", "course.teacher", "student"})
    List<CourseEnrollment> findByStudentId(Long studentId);

    @EntityGraph(attributePaths = {"course", "student"})
    java.util.Optional<CourseEnrollment> findByCourseIdAndStudentId(Long courseId, Long studentId);

    void deleteByCourseIdAndStudentId(Long courseId, Long studentId);

    void deleteByCourseId(Long courseId);

    long countByCourseId(Long courseId);

    /**
     * 批量查询多个课程的选课人数，避免 N+1。
     * 返回 Object[]{courseId, enrollmentCount}
     */
    @Query("""
            select e.course.id, count(e.id)
            from CourseEnrollment e
            where e.course.id in :courseIds
            group by e.course.id
            """)
    List<Object[]> findEnrollmentCountsByCourseIds(@Param("courseIds") List<Long> courseIds);
}
