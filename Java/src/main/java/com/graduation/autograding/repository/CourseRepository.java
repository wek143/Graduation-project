package com.graduation.autograding.repository;

import com.graduation.autograding.domain.Course;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseRepository extends JpaRepository<Course, Long> {

    @Override
    @EntityGraph(attributePaths = {"teacher"})
    List<Course> findAll();

    @Override
    @EntityGraph(attributePaths = {"teacher"})
    Optional<Course> findById(Long id);

    @EntityGraph(attributePaths = {"teacher"})
    List<Course> findByTeacherIdOrderByNameAsc(Long teacherId);

    Optional<Course> findFirstByTeacherIdOrderByIdAsc(Long teacherId);

    Optional<Course> findByCode(String code);

    @EntityGraph(attributePaths = {"teacher"})
    List<Course> findAllByTermOrderByNameAsc(String term);

    @EntityGraph(attributePaths = {"teacher"})
    @Query("select c from Course c order by c.term desc, c.name asc")
    List<Course> findAllOrderByTermDescNameAsc();

    @EntityGraph(attributePaths = {"teacher"})
    @Query(value = """
            select c from Course c
            left join c.teacher t
            where :keyword is null
               or lower(c.code) like lower(concat('%', :keyword, '%'))
               or lower(c.name) like lower(concat('%', :keyword, '%'))
               or lower(c.term) like lower(concat('%', :keyword, '%'))
               or lower(c.className) like lower(concat('%', :keyword, '%'))
               or lower(coalesce(t.username, '')) like lower(concat('%', :keyword, '%'))
               or lower(coalesce(t.fullName, '')) like lower(concat('%', :keyword, '%'))
            """,
            countQuery = """
                    select count(c) from Course c
                    left join c.teacher t
                    where :keyword is null
                       or lower(c.code) like lower(concat('%', :keyword, '%'))
                       or lower(c.name) like lower(concat('%', :keyword, '%'))
                       or lower(c.term) like lower(concat('%', :keyword, '%'))
                       or lower(c.className) like lower(concat('%', :keyword, '%'))
                       or lower(coalesce(t.username, '')) like lower(concat('%', :keyword, '%'))
                       or lower(coalesce(t.fullName, '')) like lower(concat('%', :keyword, '%'))
                    """)
    Page<Course> searchForAdmin(@Param("keyword") String keyword, Pageable pageable);
}
