package com.harish.student_management.repository;

import com.harish.student_management.entity.StudentSectionAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentSectionAssignmentRepository extends JpaRepository<StudentSectionAssignment, Long> {

    Optional<StudentSectionAssignment> findByStudentIdAndSectionIdAndAcademicYearId(Long studentId, Long sectionId, Long academicYearId);

    List<StudentSectionAssignment> findByStudentId(Long studentId);

    List<StudentSectionAssignment> findBySectionId(Long sectionId);

    List<StudentSectionAssignment> findByAcademicYearId(Long academicYearId);

    List<StudentSectionAssignment> findBySectionIdAndAcademicYearId(Long sectionId, Long academicYearId);

    boolean existsByStudentIdAndSectionIdAndAcademicYearId(Long studentId, Long sectionId, Long academicYearId);
    Optional<StudentSectionAssignment>
    findByStudentIdAndAcademicYearId(
            Long studentId,
            Long academicYearId
    );
}
