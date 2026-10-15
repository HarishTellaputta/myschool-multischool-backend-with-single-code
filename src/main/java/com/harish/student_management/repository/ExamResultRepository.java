package com.harish.student_management.repository;

import com.harish.student_management.entity.ExamResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.*;

public interface ExamResultRepository extends JpaRepository<ExamResult, Long> {
    Optional<ExamResult> findByExamScheduleIdAndStudentId(Long examScheduleId, Long studentId);
    List<ExamResult> findByExamScheduleId(Long examScheduleId);
    List<ExamResult> findByStudentId(Long studentId);
    Page<ExamResult> findByStudentId(Long studentId, Pageable pageable);
    Page<ExamResult> findByStudentIdAndIsPublishedTrue(Long studentId, Pageable pageable);
    List<ExamResult> findByExamScheduleIdOrderByPercentageDesc(
            Long examScheduleId
    );

}
