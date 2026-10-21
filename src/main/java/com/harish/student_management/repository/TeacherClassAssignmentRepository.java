package com.harish.student_management.repository;

import com.harish.student_management.entity.TeacherClassAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeacherClassAssignmentRepository
        extends JpaRepository<TeacherClassAssignment, Long> {

    // ---------------- EXISTING METHODS ----------------

    Optional<TeacherClassAssignment>
    findByTeacherIdAndClassEntityIdAndSubjectId(
            Long teacherId,
            Long classId,
            Long subjectId
    );

    List<TeacherClassAssignment> findByTeacherId(Long teacherId);

    List<TeacherClassAssignment> findByClassEntityId(Long classId);

    List<TeacherClassAssignment> findByTeacherIdAndSubjectId(
            Long teacherId,
            Long subjectId
    );

    boolean existsByTeacherIdAndClassEntityIdAndSubjectId(
            Long teacherId,
            Long classId,
            Long subjectId
    );

    boolean existsByTeacherIdAndSubjectId(
            Long teacherId,
            Long subjectId
    );

    boolean existsByClassEntityIdAndSectionIdAndSubjectId(
            Long classId,
            Long sectionId,
            Long subjectId
    );

    Optional<TeacherClassAssignment>
    findByTeacherIdAndClassEntityIdAndSectionIdAndSubjectId(
            Long teacherId,
            Long classId,
            Long sectionId,
            Long subjectId
    );

    List<TeacherClassAssignment> findByClassEntityIdAndSubjectId(
            Long classId,
            Long subjectId
    );

    List<TeacherClassAssignment> findByClassEntityIdAndSectionId(
            Long classId,
            Long sectionId
    );

    // ---------------- SCHOOL-SCOPED METHODS ----------------

    @Query("""
        SELECT a
        FROM TeacherClassAssignment a
        WHERE a.id = :assignmentId
          AND a.teacher.school.id = :schoolId
          AND a.classEntity.school.id = :schoolId
          AND a.subject.school.id = :schoolId
        """)
    Optional<TeacherClassAssignment> findByIdAndSchoolId(
            @Param("assignmentId") Long assignmentId,
            @Param("schoolId") Long schoolId
    );

    @Query("""
        SELECT a
        FROM TeacherClassAssignment a
        WHERE a.teacher.id = :teacherId
          AND a.teacher.school.id = :schoolId
          AND a.classEntity.school.id = :schoolId
          AND a.subject.school.id = :schoolId
        """)
    List<TeacherClassAssignment> findByTeacherIdAndSchoolId(
            @Param("teacherId") Long teacherId,
            @Param("schoolId") Long schoolId
    );

    @Query("""
        SELECT a
        FROM TeacherClassAssignment a
        WHERE a.classEntity.id = :classId
          AND a.teacher.school.id = :schoolId
          AND a.classEntity.school.id = :schoolId
          AND a.subject.school.id = :schoolId
        """)
    List<TeacherClassAssignment> findByClassIdAndSchoolId(
            @Param("classId") Long classId,
            @Param("schoolId") Long schoolId
    );

    @Query("""
        SELECT a
        FROM TeacherClassAssignment a
        WHERE a.teacher.id = :teacherId
          AND a.subject.id = :subjectId
          AND a.teacher.school.id = :schoolId
          AND a.classEntity.school.id = :schoolId
          AND a.subject.school.id = :schoolId
        """)
    List<TeacherClassAssignment> findByTeacherIdAndSubjectIdAndSchoolId(
            @Param("teacherId") Long teacherId,
            @Param("subjectId") Long subjectId,
            @Param("schoolId") Long schoolId
    );
}