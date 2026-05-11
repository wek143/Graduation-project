package com.graduation.autograding.repository;

import com.graduation.autograding.domain.AuditLog;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findTop100ByOrderByCreatedAtDesc();

    @Query("""
            select l from AuditLog l
            where :keyword is null
               or lower(l.action) like lower(concat('%', :keyword, '%'))
               or lower(l.actorUsername) like lower(concat('%', :keyword, '%'))
               or lower(coalesce(l.targetType, '')) like lower(concat('%', :keyword, '%'))
               or lower(coalesce(l.targetId, '')) like lower(concat('%', :keyword, '%'))
               or lower(l.summary) like lower(concat('%', :keyword, '%'))
            """)
    Page<AuditLog> searchForAdmin(@Param("keyword") String keyword, Pageable pageable);
}
