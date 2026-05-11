package com.graduation.autograding.repository;

import com.graduation.autograding.domain.User;
import com.graduation.autograding.domain.UserRole;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    List<User> findByRoleOrderByIdAsc(UserRole role);
    long countByRole(UserRole role);

    @Query("""
            select u from User u
            where (:role is null or u.role = :role)
              and (
                    :keyword is null
                    or lower(u.username) like lower(concat('%', :keyword, '%'))
                    or lower(coalesce(u.fullName, '')) like lower(concat('%', :keyword, '%'))
                    or lower(coalesce(u.className, '')) like lower(concat('%', :keyword, '%'))
              )
            """)
    Page<User> searchForAdmin(
            @Param("keyword") String keyword,
            @Param("role") UserRole role,
            Pageable pageable
    );
}
