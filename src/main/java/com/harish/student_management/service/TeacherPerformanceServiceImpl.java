package com.harish.student_management.service;

import com.harish.student_management.dto.SubjectPerformanceResponseDto;
import com.harish.student_management.dto.TeacherClassAssignmentResponseDto;
import com.harish.student_management.dto.TeacherPerformanceResponseDto;
import com.harish.student_management.entity.ExamResult;
import com.harish.student_management.entity.ExamSchedule;
import com.harish.student_management.entity.Teacher;
import com.harish.student_management.exception.ResourceNotFoundException;
import com.harish.student_management.repository.ExamResultRepository;
import com.harish.student_management.repository.ExamScheduleRepository;
import com.harish.student_management.repository.StudentRepository;
import com.harish.student_management.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeacherPerformanceServiceImpl
        implements TeacherPerformanceService {

    private final TeacherRepository teacherRepository;
    private final TeacherClassAssignmentService assignmentService;
    private final ExamScheduleRepository examScheduleRepository;
    private final ExamResultRepository examResultRepository;
    private final StudentRepository studentRepository;

    // =========================================================
    // ADMIN
    // SELECTED TEACHER PERFORMANCE
    // =========================================================

    @Override
    public List<TeacherPerformanceResponseDto> getTeacherPerformance(
            Long teacherId) {

        teacherRepository.findById(teacherId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Teacher not found with id: " + teacherId
                        )
                );

        return buildTeacherPerformance(teacherId);
    }

    // =========================================================
    // TEACHER
    // LOGGED-IN TEACHER PERFORMANCE
    // =========================================================

    @Override
    public List<TeacherPerformanceResponseDto> getMyPerformance(
            String username) {

        Teacher teacher =
                teacherRepository.findByUser_Username(username)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Teacher not found for username: "
                                                + username
                                )
                        );

        return buildTeacherPerformance(teacher.getId());
    }

    // =========================================================
    // COMMON TEACHER PERFORMANCE LOGIC
    // =========================================================

    private List<TeacherPerformanceResponseDto> buildTeacherPerformance(
            Long teacherId) {

        List<TeacherClassAssignmentResponseDto> assignments =
                assignmentService.getAssignmentsByTeacherId(
                        teacherId
                );

        List<TeacherPerformanceResponseDto> response =
                new ArrayList<>();

        for (TeacherClassAssignmentResponseDto assignment
                : assignments) {

            Long classId = assignment.getClassId();
            Long sectionId = assignment.getSectionId();
            Long subjectId = assignment.getSubjectId();

            // =====================================================
            // FIND LATEST EXAM FOR EXACT
            // CLASS + SECTION + SUBJECT
            // =====================================================

            List<ExamSchedule> schedules =
                    examScheduleRepository
                            .findByClassSectionSubjectOrderByLatest(
                                    classId,
                                    sectionId,
                                    subjectId
                            );

            if (schedules.isEmpty()) {
                continue;
            }

            ExamSchedule latestSchedule =
                    schedules.get(0);

            // =====================================================
            // STUDENT COUNT
            // =====================================================

            int studentCount;

            if (sectionId != null) {

                studentCount =
                        studentRepository
                                .findBySectionId(sectionId)
                                .size();

            } else {

                studentCount =
                        studentRepository
                                .findBySection_ClassEntity_Id(
                                        classId
                                )
                                .size();
            }

            // =====================================================
            // EXAM RESULTS
            // =====================================================

            List<ExamResult> results =
                    examResultRepository
                            .findByExamScheduleId(
                                    latestSchedule.getId()
                            );

            List<ExamResult> assessedResults =
                    results.stream()
                            .filter(result ->
                                    result.getMarksObtained() != null
                            )
                            .toList();

            int assessedCount =
                    assessedResults.size();

            // =====================================================
            // PERFORMANCE CALCULATION
            // =====================================================

            double averageMarks = 0.0;
            double performancePercentage = 0.0;

            if (!assessedResults.isEmpty()) {

                double totalMarks =
                        assessedResults.stream()
                                .mapToDouble(result ->
                                        result.getMarksObtained()
                                )
                                .sum();

                averageMarks =
                        totalMarks / assessedResults.size();

                if (latestSchedule.getMaxMarks() != null
                        && latestSchedule.getMaxMarks() > 0) {

                    performancePercentage =
                            averageMarks
                                    * 100.0
                                    / latestSchedule.getMaxMarks();
                }
            }

            // =====================================================
            // RESPONSE
            // =====================================================

            response.add(
                    TeacherPerformanceResponseDto.builder()

                            .classId(classId)

                            .className(
                                    assignment.getClassName()
                            )

                            .sectionId(sectionId)

                            .sectionName(
                                    assignment.getSectionName()
                            )

                            .subjectId(subjectId)

                            .subjectName(
                                    assignment.getSubjectName()
                            )

                            .subjectCode(
                                    assignment.getSubjectCode()
                            )

                            .latestExamScheduleId(
                                    latestSchedule.getId()
                            )

                            .latestExamName(
                                    latestSchedule
                                            .getExamination()
                                            .getName()
                            )

                            .examDate(
                                    latestSchedule.getExamDate()
                            )

                            .studentCount(
                                    studentCount
                            )

                            .assessedCount(
                                    assessedCount
                            )

                            .averageMarks(
                                    round(averageMarks)
                            )

                            .maxMarks(
                                    latestSchedule.getMaxMarks()
                            )

                            .performancePercentage(
                                    round(
                                            performancePercentage
                                    )
                            )

                            .build()
            );
        }

        return response;
    }

    // =========================================================
    // ADMIN
    // SUBJECT PERFORMANCE HISTORY
    // =========================================================

    @Override
    public List<SubjectPerformanceResponseDto> getSubjectPerformance(
            Long subjectId) {

        List<ExamSchedule> schedules =
                examScheduleRepository
                        .findBySubjectIdOrderByExamDateDescIdDesc(
                                subjectId
                        );

        return buildSubjectPerformance(schedules, subjectId);
    }

    // =========================================================
    // TEACHER
    // OWN SUBJECT PERFORMANCE HISTORY
    // =========================================================

    @Override
    public List<SubjectPerformanceResponseDto>
    getMySubjectPerformance(String username) {

        Teacher teacher =
                teacherRepository.findByUser_Username(username)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Teacher not found for username: "
                                                + username
                                )
                        );

        List<TeacherClassAssignmentResponseDto> assignments =
                assignmentService.getAssignmentsByTeacherId(
                        teacher.getId()
                );

        List<SubjectPerformanceResponseDto> response =
                new ArrayList<>();

        // =========================================================
        // ONLY EXAMS BELONGING TO TEACHER ASSIGNMENTS
        // =========================================================

        for (TeacherClassAssignmentResponseDto assignment
                : assignments) {

            List<ExamSchedule> schedules =
                    examScheduleRepository
                            .findByClassSectionSubjectOrderByLatest(
                                    assignment.getClassId(),
                                    assignment.getSectionId(),
                                    assignment.getSubjectId()
                            );

            response.addAll(
                    buildSubjectPerformance(
                            schedules,
                            assignment.getSubjectId()
                    )
            );
        }

        return response;
    }

    // =========================================================
    // BUILD SUBJECT PERFORMANCE
    // =========================================================

    private List<SubjectPerformanceResponseDto>
    buildSubjectPerformance(
            List<ExamSchedule> schedules,
            Long subjectId) {

        List<SubjectPerformanceResponseDto> response =
                new ArrayList<>();

        for (ExamSchedule schedule : schedules) {

            Long classId =
                    schedule.getClassEntity().getId();

            String className =
                    schedule.getClassEntity().getName();

            Long sectionId = null;
            String sectionName = "All Sections";

            if (schedule.getSection() != null) {

                sectionId =
                        schedule.getSection().getId();

                sectionName =
                        schedule.getSection().getName();
            }

            // =====================================================
            // STUDENT COUNT
            // =====================================================

            int studentCount;

            if (sectionId != null) {

                studentCount =
                        studentRepository
                                .findBySectionId(sectionId)
                                .size();

            } else {

                studentCount =
                        studentRepository
                                .findBySection_ClassEntity_Id(
                                        classId
                                )
                                .size();
            }

            // =====================================================
            // RESULTS
            // =====================================================

            List<ExamResult> results =
                    examResultRepository
                            .findByExamScheduleId(
                                    schedule.getId()
                            );

            List<ExamResult> assessedResults =
                    results.stream()
                            .filter(result ->
                                    result.getMarksObtained() != null
                            )
                            .toList();

            int assessedCount =
                    assessedResults.size();

            // =====================================================
            // CALCULATE PERFORMANCE
            // =====================================================

            double averageMarks = 0.0;
            double performancePercentage = 0.0;

            if (!assessedResults.isEmpty()) {

                double totalMarks =
                        assessedResults.stream()
                                .mapToDouble(result ->
                                        result.getMarksObtained()
                                )
                                .sum();

                averageMarks =
                        totalMarks / assessedResults.size();

                if (schedule.getMaxMarks() != null
                        && schedule.getMaxMarks() > 0) {

                    performancePercentage =
                            averageMarks
                                    * 100.0
                                    / schedule.getMaxMarks();
                }
            }

            // =====================================================
            // RESPONSE
            // =====================================================

            response.add(
                    SubjectPerformanceResponseDto.builder()

                            .classId(classId)

                            .className(className)

                            .sectionId(sectionId)

                            .sectionName(sectionName)

                            .subjectId(subjectId)

                            .subjectName(
                                    schedule
                                            .getSubject()
                                            .getName()
                            )

                            .subjectCode(
                                    schedule
                                            .getSubject()
                                            .getCode()
                            )

                            .examScheduleId(
                                    schedule.getId()
                            )

                            .examName(
                                    schedule
                                            .getExamination()
                                            .getName()
                            )

                            .examDate(
                                    schedule.getExamDate()
                            )

                            .studentCount(
                                    studentCount
                            )

                            .assessedCount(
                                    assessedCount
                            )

                            .averageMarks(
                                    round(averageMarks)
                            )

                            .maxMarks(
                                    schedule.getMaxMarks()
                            )

                            .performancePercentage(
                                    round(
                                            performancePercentage
                                    )
                            )

                            .build()
            );
        }

        return response;
    }

    // =========================================================
    // ROUND
    // =========================================================

    private double round(double value) {

        return Math.round(value * 100.0) / 100.0;
    }
}