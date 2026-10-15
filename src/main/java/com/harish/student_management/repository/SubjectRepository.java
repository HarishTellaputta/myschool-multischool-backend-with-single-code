package com.harish.student_management.repository;

import com.harish.student_management.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
public interface SubjectRepository extends JpaRepository<Subject, Long> {

    List<Subject> findBySchoolId(Long schoolId);

    List<Subject> findBySchoolIdAndStatus(Long schoolId, String status);

    Optional<Subject> findBySchoolIdAndCode(Long schoolId, String code);

    boolean existsBySchoolIdAndCode(Long schoolId, String code);

    Optional<Subject> findByIdAndSchoolId(Long id, Long schoolId);

    Optional<Subject> findByNameIgnoreCase(String name);
}