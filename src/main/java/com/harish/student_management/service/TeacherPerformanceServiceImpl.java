
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
    private final CurrentSchoolService currentSchoolService;

    // =========================================================
    // ADMIN: SELECTED TEACHER PERFORMANCE
    // =========================================================

    @Override
    public List<TeacherPerformanceResponseDto> getTeacherPerformance(
            Long teacherId) {

        Long schoolId = currentSchoolService.getCurrentSchoolId();

        Teacher teacher = teacherRepository
                .findByIdAndSchoolId(teacherId, schoolId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Teacher not found in current school: "
                                        + teacherId
                        )
                );

        return buildTeacherPerformance(teacher.getId(), schoolId);
    }

    // =========================================================
    // TEACHER: LOGGED-IN TEACHER PERFORMANCE
    // =========================================================

    @Override
    public List<TeacherPerformanceResponseDto> getMyPerformance(
            String username) {

        Long schoolId = currentSchoolService.getCurrentSchoolId();

        Teacher teacher = teacherRepository
                .findByUsernameAndSchoolId(username, schoolId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Teacher not found in current school"
                        )
                );

        return buildTeacherPerformance(teacher.getId(), schoolId);
    }

    // =========================================================
    // COMMON TEACHER PERFORMANCE LOGIC
    // =========================================================

    private List<TeacherPerformanceResponseDto> buildTeacherPerformance(
            Long teacherId,
            Long schoolId) {

        // Assignment service already validates the current school.
        List<TeacherClassAssignmentResponseDto> assignments =
                assignmentService.getAssignmentsByTeacherId(teacherId);

        List<TeacherPerformanceResponseDto> response =
                new ArrayList<>();

        for (TeacherClassAssignmentResponseDto assignment : assignments) {

            Long classId = assignment.getClassId();
            Long sectionId = assignment.getSectionId();
            Long subjectId = assignment.getSubjectId();

            // Only schedules belonging to this school and the exact
            // class + section + subject are returned.
            List<ExamSchedule> schedules =
                    examScheduleRepository
                            .findPerformanceSchedulesByClassSectionSubjectAndSchool(
                                    classId,
                                    sectionId,
                                    subjectId,
                                    schoolId
                            );

            if (schedules.isEmpty()) {
                continue;
            }

            ExamSchedule latestSchedule = schedules.get(0);

            // School-scoped student count.
            long studentCount = getStudentCount(
                    classId,
                    sectionId,
                    schoolId
            );

            // Results are scoped to the schedule's school.
            List<ExamResult> results =
                    examResultRepository
                            .findPerformanceResultsByScheduleAndSchool(
                                    latestSchedule.getId(),
                                    schoolId
                            );

            List<ExamResult> assessedResults = results.stream()
                    .filter(result -> result.getMarksObtained() != null)
                    .toList();

            int assessedCount = assessedResults.size();

            double averageMarks = calculateAverageMarks(assessedResults);

            Integer maxMarks = latestSchedule.getMaxMarks();

            double performancePercentage =
                    calculatePerformancePercentage(
                            averageMarks,
                            maxMarks,
                            assessedCount
                    );

            response.add(
                    TeacherPerformanceResponseDto.builder()
                            .classId(classId)
                            .className(assignment.getClassName())
                            .sectionId(sectionId)
                            .sectionName(assignment.getSectionName())
                            .subjectId(subjectId)
                            .subjectName(assignment.getSubjectName())
                            .subjectCode(assignment.getSubjectCode())
                            .latestExamScheduleId(latestSchedule.getId())
                            .latestExamName(
                                    latestSchedule.getExamination().getName()
                            )
                            .examDate(latestSchedule.getExamDate())
                            .studentCount(toIntCount(studentCount))
                            .assessedCount(assessedCount)
                            .averageMarks(round(averageMarks))
                            .maxMarks(maxMarks == null ? 0 : maxMarks)
                            .performancePercentage(
                                    round(performancePercentage)
                            )
                            .build()
            );
        }

        return response;
    }

    // =========================================================
    // ADMIN: SUBJECT PERFORMANCE HISTORY
    // =========================================================

    @Override
    public List<SubjectPerformanceResponseDto> getSubjectPerformance(
            Long subjectId) {

        Long schoolId = currentSchoolService.getCurrentSchoolId();

        List<ExamSchedule> schedules =
                examScheduleRepository
                        .findPerformanceSchedulesBySubjectAndSchool(
                                subjectId,
                                schoolId
                        );

        return buildSubjectPerformance(schedules, schoolId);
    }

    // =========================================================
    // TEACHER: OWN SUBJECT PERFORMANCE HISTORY
    // =========================================================

    @Override
    public List<SubjectPerformanceResponseDto> getMySubjectPerformance(
            String username) {

        Long schoolId = currentSchoolService.getCurrentSchoolId();

        Teacher teacher = teacherRepository
                .findByUsernameAndSchoolId(username, schoolId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Teacher not found in current school"
                        )
                );

        List<TeacherClassAssignmentResponseDto> assignments =
                assignmentService.getAssignmentsByTeacherId(
                        teacher.getId()
                );

        List<SubjectPerformanceResponseDto> response =
                new ArrayList<>();

        for (TeacherClassAssignmentResponseDto assignment : assignments) {

            List<ExamSchedule> schedules =
                    examScheduleRepository
                            .findPerformanceSchedulesByClassSectionSubjectAndSchool(
                                    assignment.getClassId(),
                                    assignment.getSectionId(),
                                    assignment.getSubjectId(),
                                    schoolId
                            );

            response.addAll(
                    buildSubjectPerformance(schedules, schoolId)
            );
        }

        return response;
    }

    // =========================================================
    // BUILD SUBJECT PERFORMANCE HISTORY
    // =========================================================

    private List<SubjectPerformanceResponseDto> buildSubjectPerformance(
            List<ExamSchedule> schedules,
            Long schoolId) {

        List<SubjectPerformanceResponseDto> response =
                new ArrayList<>();

        for (ExamSchedule schedule : schedules) {

            Long classId = schedule.getClassEntity().getId();
            String className = schedule.getClassEntity().getName();

            Long sectionId = null;
            String sectionName = "All Sections";

            if (schedule.getSection() != null) {
                sectionId = schedule.getSection().getId();
                sectionName = schedule.getSection().getName();
            }

            long studentCount = getStudentCount(
                    classId,
                    sectionId,
                    schoolId
            );

            List<ExamResult> results =
                    examResultRepository
                            .findPerformanceResultsByScheduleAndSchool(
                                    schedule.getId(),
                                    schoolId
                            );

            List<ExamResult> assessedResults = results.stream()
                    .filter(result -> result.getMarksObtained() != null)
                    .toList();

            int assessedCount = assessedResults.size();

            double averageMarks = calculateAverageMarks(assessedResults);

            Integer maxMarks = schedule.getMaxMarks();

            double performancePercentage =
                    calculatePerformancePercentage(
                            averageMarks,
                            maxMarks,
                            assessedCount
                    );

            response.add(
                    SubjectPerformanceResponseDto.builder()
                            .classId(classId)
                            .className(className)
                            .sectionId(sectionId)
                            .sectionName(sectionName)
                            .subjectId(schedule.getSubject().getId())
                            .subjectName(schedule.getSubject().getName())
                            .subjectCode(schedule.getSubject().getCode())
                            .examScheduleId(schedule.getId())
                            .examName(schedule.getExamination().getName())
                            .examDate(schedule.getExamDate())
                            .studentCount(toIntCount(studentCount))
                            .assessedCount(assessedCount)
                            .averageMarks(round(averageMarks))
                            .maxMarks(maxMarks)
                            .performancePercentage(
                                    round(performancePercentage)
                            )
                            .build()
            );
        }

        return response;
    }

    // =========================================================
    // SCHOOL-SCOPED STUDENT COUNT
    // =========================================================

    private long getStudentCount(
            Long classId,
            Long sectionId,
            Long schoolId) {

        if (sectionId != null) {
            return studentRepository
                    .countPerformanceStudentsBySectionAndSchool(
                            sectionId,
                            schoolId
                    );
        }

        return studentRepository
                .countPerformanceStudentsByClassAndSchool(
                        classId,
                        schoolId
                );
    }

    // =========================================================
    // PERFORMANCE CALCULATIONS
    // =========================================================

    private double calculateAverageMarks(
            List<ExamResult> assessedResults) {

        if (assessedResults.isEmpty()) {
            return 0.0;
        }

        double totalMarks = assessedResults.stream()
                .mapToDouble(result -> result.getMarksObtained())
                .sum();

        return totalMarks / assessedResults.size();
    }

    private double calculatePerformancePercentage(
            double averageMarks,
            Integer maxMarks,
            int assessedCount) {

        if (assessedCount == 0 || maxMarks == null || maxMarks <= 0) {
            return 0.0;
        }

        return averageMarks * 100.0 / maxMarks;
    }

    private int toIntCount(long count) {
        return count > Integer.MAX_VALUE
                ? Integer.MAX_VALUE
                : (int) count;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
