package com.harish.student_management.repository;

import com.harish.student_management.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {


    List<Student> findByParentId(Long parentId);


    long countByAdmissionDate(LocalDate date);

    @Query("""
            SELECT s
            FROM Student s
            WHERE s.section.classEntity.id = :classId
            """)
    List<Student> findBySection_ClassEntity_Id(
            @Param("classId") Long classId
    );


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

    Optional<Student> findByAdmissionNoAndSection_ClassEntity_School_Id(
            String admissionNo,
            Long schoolId
    );

    boolean existsByAdmissionNoAndSection_ClassEntity_School_Id(
            String admissionNo,
            Long schoolId
    );

    Optional<Student> findByIdAndSection_ClassEntity_School_Id(
            Long id,
            Long schoolId
    );

    List<Student> findByParent_IdAndSection_ClassEntity_School_Id(
            Long parentId,
            Long schoolId
    );


    // School-wise pagination
    Page<Student> findAllBySection_ClassEntity_School_Id(
            Long schoolId,
            Pageable pageable
    );

    // School-wise student count
    long countBySection_ClassEntity_School_Id(Long schoolId);

    // School-wise gender summary
    long countByGenderIgnoreCaseAndSection_ClassEntity_School_Id(
            String gender,
            Long schoolId
    );

    // Class-wise students, restricted to the current school
    List<Student> findBySection_ClassEntity_IdAndSection_ClassEntity_School_Id(
            Long classId,
            Long schoolId
    );

    // Section-wise students, restricted to the current school
    List<Student> findBySection_IdAndSection_ClassEntity_School_Id(
            Long sectionId,
            Long schoolId
    );

    // School-wise birthday list
    @Query("""
            SELECT s
            FROM Student s
            WHERE s.section.classEntity.school.id = :schoolId
              AND s.dateOfBirth IS NOT NULL
              AND MONTH(s.dateOfBirth) = :month
              AND DAY(s.dateOfBirth) = :day
            """)
    List<Student> findSchoolStudentsHavingBirthdayTodayBySchool(
            @Param("schoolId") Long schoolId,
            @Param("month") int month,
            @Param("day") int day
    );


    @Query("""
    SELECT COUNT(s)
    FROM Student s
    WHERE s.section.id = :sectionId
      AND s.section.classEntity.school.id = :schoolId
      AND s.academicYear.school.id = :schoolId
    """)
    long countPerformanceStudentsBySectionAndSchool(
            @Param("sectionId") Long sectionId,
            @Param("schoolId") Long schoolId
    );

    @Query("""
    SELECT COUNT(s)
    FROM Student s
    WHERE s.section.classEntity.id = :classId
      AND s.section.classEntity.school.id = :schoolId
      AND s.academicYear.school.id = :schoolId
    """)
    long countPerformanceStudentsByClassAndSchool(
            @Param("classId") Long classId,
            @Param("schoolId") Long schoolId
    );


    Optional<Student>
    findByAdmissionNoIgnoreCaseAndSection_ClassEntity_School_Id(
            String admissionNo,
            Long schoolId
    );



}