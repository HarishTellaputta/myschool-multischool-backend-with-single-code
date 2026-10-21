package com.harish.student_management.repository;

import com.harish.student_management.entity.StudentSectionAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentSectionAssignmentRepository
        extends JpaRepository<StudentSectionAssignment, Long> {

    // Existing methods — retained for compatibility
    Optional<StudentSectionAssignment>
    findByStudentIdAndSectionIdAndAcademicYearId(
            Long studentId, Long sectionId, Long academicYearId);

    List<StudentSectionAssignment> findByStudentId(Long studentId);

    List<StudentSectionAssignment> findBySectionId(Long sectionId);

    List<StudentSectionAssignment> findByAcademicYearId(Long academicYearId);

    List<StudentSectionAssignment>
    findBySectionIdAndAcademicYearId(Long sectionId, Long academicYearId);

    boolean existsByStudentIdAndSectionIdAndAcademicYearId(
            Long studentId, Long sectionId, Long academicYearId);

    Optional<StudentSectionAssignment>
    findByStudentIdAndAcademicYearId(Long studentId, Long academicYearId);

    // School-scoped queries
    @Query("""
        SELECT a FROM StudentSectionAssignment a
        JOIN a.section s
        JOIN s.classEntity c
        WHERE a.id = :id AND c.school.id = :schoolId
        """)
    Optional<StudentSectionAssignment> findByIdAndSchoolId(
            @Param("id") Long id,
            @Param("schoolId") Long schoolId);

    @Query("""
        SELECT a FROM StudentSectionAssignment a
        JOIN a.section s
        JOIN s.classEntity c
        WHERE c.school.id = :schoolId
        ORDER BY a.id DESC
        """)
    List<StudentSectionAssignment> findAllBySchoolId(
            @Param("schoolId") Long schoolId);

    @Query("""
        SELECT a FROM StudentSectionAssignment a
        JOIN a.section s
        JOIN s.classEntity c
        WHERE a.student.id = :studentId
          AND c.school.id = :schoolId
        ORDER BY a.id DESC
        """)
    List<StudentSectionAssignment> findByStudentIdAndSchoolId(
            @Param("studentId") Long studentId,
            @Param("schoolId") Long schoolId);

    @Query("""
        SELECT a FROM StudentSectionAssignment a
        JOIN a.section s
        JOIN s.classEntity c
        WHERE s.id = :sectionId
          AND c.school.id = :schoolId
        ORDER BY a.id DESC
        """)
    List<StudentSectionAssignment> findBySectionIdAndSchoolId(
            @Param("sectionId") Long sectionId,
            @Param("schoolId") Long schoolId);

    @Query("""
        SELECT a FROM StudentSectionAssignment a
        JOIN a.section s
        JOIN s.classEntity c
        WHERE a.academicYear.id = :academicYearId
          AND c.school.id = :schoolId
        ORDER BY a.id DESC
        """)
    List<StudentSectionAssignment> findByAcademicYearIdAndSchoolId(
            @Param("academicYearId") Long academicYearId,
            @Param("schoolId") Long schoolId);

    @Query("""
        SELECT a FROM StudentSectionAssignment a
        JOIN a.section s
        JOIN s.classEntity c
        WHERE s.id = :sectionId
          AND a.academicYear.id = :academicYearId
          AND c.school.id = :schoolId
        ORDER BY a.id DESC
        """)
    List<StudentSectionAssignment>
    findBySectionIdAndAcademicYearIdAndSchoolId(
            @Param("sectionId") Long sectionId,
            @Param("academicYearId") Long academicYearId,
            @Param("schoolId") Long schoolId);

    @Query("""
        SELECT a FROM StudentSectionAssignment a
        JOIN a.section s
        JOIN s.classEntity c
        WHERE a.student.id = :studentId
          AND a.academicYear.id = :academicYearId
          AND c.school.id = :schoolId
        """)
    Optional<StudentSectionAssignment>
    findByStudentIdAndAcademicYearIdAndSchoolId(
            @Param("studentId") Long studentId,
            @Param("academicYearId") Long academicYearId,
            @Param("schoolId") Long schoolId);
}