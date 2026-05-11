package com.graduation.autograding.repository;

import com.graduation.autograding.domain.TestCase;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestCaseRepository extends JpaRepository<TestCase, Long> {
    List<TestCase> findByAssignmentIdOrderByIdAsc(Long assignmentId);
}
