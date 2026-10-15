package com.harish.student_management.repository;

import com.harish.student_management.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {


    Optional<User> findByUsername(String username);

    @Query("""
            SELECT u
            FROM User u
            LEFT JOIN FETCH u.role
            WHERE u.username = :username
            """)
    Optional<User> findByUsernameWithRole(@Param("username") String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    Optional<User> findByPasswordResetTokenHash(String passwordResetTokenHash);

    @Query("""
            SELECT u
            FROM User u
            WHERE u.role.name = :roleName
              AND NOT EXISTS (
                  SELECT t.id
                  FROM Teacher t
                  WHERE t.user.id = u.id
              )
            ORDER BY u.username
            """)
    List<User> findAvailableUsersByRole(@Param("roleName") String roleName);
    boolean existsByRole_Name(String roleName);
}
