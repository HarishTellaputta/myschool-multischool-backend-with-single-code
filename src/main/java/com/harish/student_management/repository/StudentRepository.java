package com.harish.student_management.repository;

import com.harish.student_management.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByAdmissionNo(String admissionNo);

    boolean existsByAdmissionNo(String admissionNo);

    List<Student> findBySectionId(Long sectionId);

    List<Student> findByParentId(Long parentId);

    List<Student> findByAcademicYearId(Long academicYearId);

    List<Student> findByStatus(String status);

    long countByAdmissionDate(LocalDate date);

    @Query("""
            SELECT s
            FROM Student s
            WHERE s.section.classEntity.id = :classId
            """)
    List<Student> findBySection_ClassEntity_Id(
            @Param("classId") Long classId
    );

    long countByGenderIgnoreCase(String gender);

    @Query("""
            SELECT s
            FROM Student s
            WHERE s.parent.id = :parentId
              AND s.dateOfBirth IS NOT NULL
              AND MONTH(s.dateOfBirth) = :month
              AND DAY(s.dateOfBirth) = :day
            """)
    List<Student> findParentStudentsHavingBirthdayToday(
            @Param("parentId") Long parentId,
            @Param("month") int month,
            @Param("day") int day
    );

    @Query("""
            SELECT s
            FROM Student s
            WHERE MONTH(s.dateOfBirth) = :month
              AND DAY(s.dateOfBirth) = :day
              AND s.dateOfBirth IS NOT NULL
            """)
    List<Student> findSchoolStudentsHavingBirthdayToday(
            @Param("month") int month,
            @Param("day") int day
    );

    Optional<Student> findByAdmissionNoIgnoreCase(
            String admissionNo
    );

    /*
     * Fetch admission numbers belonging only to the selected school.
     *
     * ORDER BY s.id DESC means newest student first.
     *
     * The service will scan this list and use the first valid
     * admission number having a trailing numeric part.
     *
     * Examples:
     * ADM001  -> ADM002
     * 1001    -> 1002
     * STU-009 -> STU-010
     */
    @Query("""
            SELECT s.admissionNo
            FROM Student s
            WHERE s.section.classEntity.school.id = :schoolId
              AND s.admissionNo IS NOT NULL
              AND TRIM(s.admissionNo) <> ''
            ORDER BY s.id DESC
            """)
    List<String> findAdmissionNumbersBySchoolOrderByLatest(
            @Param("schoolId") Long schoolId
    );

    @Query("""
        SELECT s.rollNumber
        FROM Student s
        WHERE s.section.classEntity.id = :classId
          AND s.rollNumber IS NOT NULL
          AND TRIM(s.rollNumber) <> ''
        ORDER BY s.id DESC
        """)
    List<String> findRollNumbersByClassOrderByLatest(
            @Param("classId") Long classId
    );
}