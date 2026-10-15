package com.harish.student_management.repository;

import com.harish.student_management.entity.School;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SchoolRepository extends JpaRepository<School, Long> {

    Optional<School> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByName(String name);
}
