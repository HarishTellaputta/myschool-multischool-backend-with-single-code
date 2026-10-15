package com.harish.student_management.repository;

import com.harish.student_management.entity.Parent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ParentRepository extends JpaRepository<Parent, Long> {

    Optional<Parent> findByContactPhone(String contactPhone);

    Optional<Parent> findByEmail(String email);
    Optional<Parent> findByUserId(Long userId);

    boolean existsByContactPhone(String contactPhone);

    boolean existsByEmail(String email);

    Optional<Parent> findByUserUsername(String username);


}