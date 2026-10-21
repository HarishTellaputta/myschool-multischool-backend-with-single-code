
package com.harish.student_management.repository;

import com.harish.student_management.entity.AcademicYear;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AcademicYearRepository
        extends JpaRepository<AcademicYear, Long> {

    List<AcademicYear> findAllBySchool_Id(Long schoolId);

    Optional<AcademicYear> findByIdAndSchool_Id(
            Long id, Long schoolId);

    Optional<AcademicYear> findBySchool_IdAndNameIgnoreCase(
            Long schoolId, String name);

    boolean existsBySchool_IdAndNameIgnoreCase(
            Long schoolId, String name);

    Optional<AcademicYear> findBySchool_IdAndCurrentTrue(
            Long schoolId);
}
