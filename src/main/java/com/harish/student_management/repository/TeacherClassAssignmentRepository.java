package com.harish.student_management.repository;

import com.harish.student_management.entity.TeacherClassAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeacherClassAssignmentRepository
        extends JpaRepository<TeacherClassAssignment, Long> {

    // =========================================================
    // EXISTING: GET ASSIGNMENT BY TEACHER + CLASS + SUBJECT
    // =========================================================

    Optional<TeacherClassAssignment>
    findByTeacherIdAndClassEntityIdAndSubjectId(
            Long teacherId,
            Long classId,
            Long subjectId
    );


    // =========================================================
    // EXISTING: GET ALL ASSIGNMENTS OF A TEACHER
    // =========================================================

    List<TeacherClassAssignment>
    findByTeacherId(Long teacherId);


    // =========================================================
    // EXISTING: GET ALL ASSIGNMENTS OF A CLASS
    // =========================================================

    List<TeacherClassAssignment>
    findByClassEntityId(Long classId);


    // =========================================================
    // EXISTING: GET ASSIGNMENTS OF TEACHER + SUBJECT
    // =========================================================

    List<TeacherClassAssignment>
    findByTeacherIdAndSubjectId(
            Long teacherId,
            Long subjectId
    );


    // =========================================================
    // EXISTING: CHECK TEACHER + CLASS + SUBJECT
    // =========================================================

    boolean existsByTeacherIdAndClassEntityIdAndSubjectId(
            Long teacherId,
            Long classId,
            Long subjectId
    );


    // =========================================================
    // EXISTING: CHECK TEACHER SUBJECT ACCESS
    // =========================================================

    boolean existsByTeacherIdAndSubjectId(
            Long teacherId,
            Long subjectId
    );


    // =========================================================
    // NEW: CHECK SECTION-WISE ASSIGNMENT
    // =========================================================
    // One section + one subject should have one teacher.

    boolean existsByClassEntityIdAndSectionIdAndSubjectId(
            Long classId,
            Long sectionId,
            Long subjectId
    );


    // =========================================================
    // NEW: GET ASSIGNMENT BY TEACHER + CLASS + SECTION + SUBJECT
    // =========================================================

    Optional<TeacherClassAssignment>
    findByTeacherIdAndClassEntityIdAndSectionIdAndSubjectId(
            Long teacherId,
            Long classId,
            Long sectionId,
            Long subjectId
    );


    // =========================================================
    // NEW: GET ALL ASSIGNMENTS FOR CLASS + SUBJECT
    // =========================================================
    // Required for Exam Schedule Excel:
    //
    // 1st Class + English
    //        ↓
    // A → Suresh
    // B → Ravi
    // C → Anil

    List<TeacherClassAssignment>
    findByClassEntityIdAndSubjectId(
            Long classId,
            Long subjectId
    );


    // =========================================================
    // NEW: GET ASSIGNMENTS BY CLASS + SECTION
    // =========================================================

    List<TeacherClassAssignment>
    findByClassEntityIdAndSectionId(
            Long classId,
            Long sectionId
    );
}