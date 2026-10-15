
        package com.harish.student_management.repository;

import com.harish.student_management.entity.ExamSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

        public interface ExamScheduleRepository extends JpaRepository<ExamSchedule, Long> {

    List<ExamSchedule> findByExaminationId(Long examinationId);

    List<ExamSchedule> findByClassEntityId(Long classId);

    List<ExamSchedule> findBySectionId(Long sectionId);

    List<ExamSchedule> findTop10ByExamDateGreaterThanEqualOrderByExamDateAsc(
            LocalDate date
    );

    Page<ExamSchedule> findByExamDateBetween(
            LocalDate from,
            LocalDate to,
            Pageable pageable
    );

    Page<ExamSchedule> findByClassEntityId(
            Long classId,
            Pageable pageable
    );

    List<ExamSchedule> findByClassEntityIdAndSubjectIdOrderByExamDateDescIdDesc(
            Long classId,
            Long subjectId
    );

    // =========================================================
    // GET ALL EXAM SCHEDULES FOR A SUBJECT
    // =========================================================

    List<ExamSchedule> findBySubjectIdOrderByExamDateDescIdDesc(
            Long subjectId
    );

            @Query("""
    SELECT s
    FROM ExamSchedule s
    WHERE s.examination.id = :examinationId
      AND s.classEntity.id = :classId
      AND s.subject.id = :subjectId
      AND s.examDate = :examDate
      AND (
          (:sectionId IS NULL AND s.section IS NULL)
          OR
          (:sectionId IS NOT NULL AND s.section.id = :sectionId)
      )
""")
            Optional<ExamSchedule> findExistingSchedule(
                    @Param("examinationId") Long examinationId,
                    @Param("classId") Long classId,
                    @Param("sectionId") Long sectionId,
                    @Param("subjectId") Long subjectId,
                    @Param("examDate") LocalDate examDate
            );


            @Query("""
    SELECT s
    FROM ExamSchedule s
    WHERE s.classEntity.id = :classId
      AND s.subject.id = :subjectId
      AND (
          (:sectionId IS NULL AND s.section IS NULL)
          OR
          (:sectionId IS NOT NULL AND s.section.id = :sectionId)
      )
    ORDER BY s.examDate DESC, s.id DESC
""")
            List<ExamSchedule> findByClassSectionSubjectOrderByLatest(
                    @Param("classId") Long classId,
                    @Param("sectionId") Long sectionId,
                    @Param("subjectId") Long subjectId
            );

}

