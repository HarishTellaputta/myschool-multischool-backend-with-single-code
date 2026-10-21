
package com.harish.student_management.repository;

import com.harish.student_management.entity.Examination;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExaminationRepository
        extends JpaRepository<Examination, Long> {

    List<Examination> findByAcademicYear_School_Id(
            Long schoolId
    );

    List<Examination> findByAcademicYear_IdAndAcademicYear_School_Id(
            Long academicYearId,
            Long schoolId
    );

    Optional<Examination> findByIdAndAcademicYear_School_Id(
            Long id,
            Long schoolId
    );

    Optional<Examination> findByAcademicYear_School_IdAndNameIgnoreCase(
            Long schoolId,
            String name
    );

    Optional<Examination> findByAcademicYear_IdAndNameIgnoreCase(
            Long academicYearId,
            String name
    );

    // Multischool-safe lookup:
    // Academic year + school + examination name
    Optional<Examination>
    findByAcademicYear_IdAndAcademicYear_School_IdAndNameIgnoreCase(
            Long academicYearId,
            Long schoolId,
            String name
    );


}
