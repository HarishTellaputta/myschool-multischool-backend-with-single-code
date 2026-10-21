
package com.harish.student_management.service;

import com.harish.student_management.dto.*;
import com.harish.student_management.entity.*;
import com.harish.student_management.exception.ResourceNotFoundException;
import com.harish.student_management.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportCardServiceImpl implements ReportCardService {

    private final StudentRepository studentRepository;
    private final ExaminationRepository examinationRepository;
    private final ExamScheduleRepository scheduleRepository;
    private final ExamResultRepository resultRepository;
    private final AttendanceRepository attendanceRepository;
    private final GradeRuleRepository gradeRuleRepository;
    private final CurrentSchoolService currentSchoolService;

    @Override
    public ReportCardDto generate(
            Long studentId,
            Long examId,
            boolean includeUnpublished) {

        Long schoolId = currentSchoolService.getCurrentSchoolId();

        if (schoolId == null) {
            throw new ResourceNotFoundException(
                    "Current school not found");
        }

        // Student must belong to the current school.
        Student student = studentRepository
                .findByIdAndSection_ClassEntity_School_Id(
                        studentId, schoolId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Student not found in current school: "
                                        + studentId));

        // If academic year is assigned, it must belong to this school.
        if (student.getAcademicYear() != null
                && (student.getAcademicYear().getSchool() == null
                || !schoolId.equals(
                student.getAcademicYear().getSchool().getId()))) {
            throw new ResourceNotFoundException(
                    "Student academic year does not belong to current school");
        }

        // Examination must belong to the current school.
        Examination exam = examinationRepository
                .findByIdAndAcademicYear_School_Id(examId, schoolId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Examination not found in current school: "
                                        + examId));

        // Fetch schedules using school scope.
        List<ExamSchedule> schedules =
                scheduleRepository.findSchedulesForSchool(
                        schoolId, examId, null, null, null);

        List<ReportCardSubjectDto> subjects = new ArrayList<>();

        for (ExamSchedule schedule : schedules) {

            // Defensive checks for related entities.
            if (schedule.getClassEntity() == null
                    || schedule.getClassEntity().getSchool() == null
                    || !schoolId.equals(
                    schedule.getClassEntity().getSchool().getId())) {
                continue;
            }

            if (schedule.getSubject() == null
                    || schedule.getSubject().getSchool() == null
                    || !schoolId.equals(
                    schedule.getSubject().getSchool().getId())) {
                continue;
            }

            if (student.getSection() == null) {
                continue;
            }

            if (!schedule.getClassEntity().getId().equals(
                    student.getSection().getClassEntity().getId())) {
                continue;
            }

            // A section-specific schedule must match the student's section.
            // A schedule without a section applies to the whole class.
            if (schedule.getSection() != null
                    && !schedule.getSection().getId().equals(
                    student.getSection().getId())) {
                continue;
            }

            // Fetch result only if schedule belongs to current school.
            ExamResult result = resultRepository
                    .findByScheduleStudentAndSchool(
                            schedule.getId(), studentId, schoolId)
                    .orElse(null);

            if (result == null) {
                continue;
            }

            // Ignore inconsistent result records pointing to another school.
            if (result.getStudent() == null
                    || !studentId.equals(result.getStudent().getId())) {
                continue;
            }

            if (!includeUnpublished
                    && !Boolean.TRUE.equals(result.getIsPublished())) {
                continue;
            }

            String subjectName = schedule.getSubject().getName();

            subjects.add(
                    ReportCardSubjectDto.builder()
                            .scheduleId(schedule.getId())
                            .subject(subjectName)
                            .examDate(schedule.getExamDate())
                            .marksObtained(result.getMarksObtained())
                            .maximumMarks(schedule.getMaxMarks())
                            .percentage(result.getPercentage())
                            .grade(calculateGrade(
                                    result.getPercentage(), schoolId))
                            .build()
            );
        }

        int total = subjects.stream()
                .mapToInt(s -> s.getMarksObtained() == null
                        ? 0 : s.getMarksObtained())
                .sum();

        int maximum = subjects.stream()
                .mapToInt(s -> s.getMaximumMarks() == null
                        ? 0 : s.getMaximumMarks())
                .sum();

        double percentage = maximum == 0
                ? 0
                : Math.round(total * 10000.0 / maximum) / 100.0;

        String grade = calculateGrade(percentage, schoolId);

        // Attendance is limited to this student's school and class.
        List<Attendance> attendance = new ArrayList<>();

        if (student.getSection() != null) {
            Long classId = student.getSection()
                    .getClassEntity().getId();

            if (student.getAcademicYear() != null
                    && student.getAcademicYear().getStartDate() != null
                    && student.getAcademicYear().getEndDate() != null) {

                attendance = attendanceRepository
                        .findByStudent_IdAndClassEntity_School_IdAndAttendanceDateBetween(
                                studentId,
                                schoolId,
                                student.getAcademicYear().getStartDate(),
                                student.getAcademicYear().getEndDate());
            } else {
                attendance = attendanceRepository
                        .findByStudent_IdAndClassEntity_School_Id(
                                studentId, schoolId);
            }

            // Ensure attendance is for the student's current class.
            attendance = attendance.stream()
                    .filter(a -> a.getClassEntity() != null
                            && a.getClassEntity().getId().equals(classId))
                    .toList();
        }

        long present = attendance.stream()
                .filter(a -> "PRESENT".equalsIgnoreCase(a.getStatus()))
                .count();

        Integer rank = calculateOverallRank(
                student, examId, schoolId, includeUnpublished, percentage);

        double attendancePercentage = attendance.isEmpty()
                ? 0
                : Math.round(present * 10000.0 / attendance.size()) / 100.0;

        String overallResult;

        if (subjects.isEmpty()) {
            overallResult = "PENDING";
        } else if (subjects.stream().anyMatch(s ->
                "F".equalsIgnoreCase(s.getGrade()))) {
            overallResult = "FAIL";
        } else {
            overallResult = "PASS";
        }

        return ReportCardDto.builder()
                .studentId(studentId)
                .studentName(student.getName())
                .rollNumber(student.getRollNumber())

                .classId(student.getSection() == null
                        ? null
                        : student.getSection().getClassEntity().getId())
                .className(student.getSection() == null
                        ? null
                        : student.getSection().getClassEntity().getName())

                .sectionId(student.getSection() == null
                        ? null : student.getSection().getId())
                .sectionName(student.getSection() == null
                        ? null : student.getSection().getName())

                .examinationId(examId)
                .examinationName(exam.getName())

                .subjects(subjects)
                .totalMarks(total)
                .totalMaximumMarks(maximum)
                .percentage(percentage)
                .grade(grade)
                .rank(rank)

                .attendancePresent(present)
                .attendanceTotal((long) attendance.size())
                .attendancePercentage(attendancePercentage)
                .overallResult(overallResult)
                .build();
    }

    private Integer calculateOverallRank(
            Student student,
            Long examId,
            Long schoolId,
            boolean includeUnpublished,
            double ownPercentage) {

        if (student.getSection() == null) {
            return null;
        }

        Long sectionId = student.getSection().getId();

        Map<Long, int[]> totals = new HashMap<>();

        List<ExamSchedule> schedules =
                scheduleRepository.findSchedulesForSchool(
                        schoolId, examId, null, null, null);

        for (ExamSchedule schedule : schedules) {

            if (schedule.getClassEntity() == null
                    || schedule.getClassEntity().getSchool() == null
                    || !schoolId.equals(
                    schedule.getClassEntity().getSchool().getId())) {
                continue;
            }

            if (schedule.getSubject() == null
                    || schedule.getSubject().getSchool() == null
                    || !schoolId.equals(
                    schedule.getSubject().getSchool().getId())) {
                continue;
            }

            if (!schedule.getClassEntity().getId().equals(
                    student.getSection().getClassEntity().getId())) {
                continue;
            }

            if (schedule.getSection() != null
                    && !sectionId.equals(schedule.getSection().getId())) {
                continue;
            }

            List<ExamResult> results =
                    resultRepository.findByScheduleAndSchool(
                            schedule.getId(), schoolId);

            for (ExamResult result : results) {

                if (!includeUnpublished
                        && !Boolean.TRUE.equals(result.getIsPublished())) {
                    continue;
                }

                if (result.getStudent() == null
                        || result.getStudent().getSection() == null
                        || result.getMarksObtained() == null) {
                    continue;
                }

                Student resultStudent = result.getStudent();

                if (!sectionId.equals(
                        resultStudent.getSection().getId())) {
                    continue;
                }

                if (resultStudent.getSection().getClassEntity() == null
                        || resultStudent.getSection().getClassEntity()
                        .getSchool() == null
                        || !schoolId.equals(
                        resultStudent.getSection().getClassEntity()
                                .getSchool().getId())) {
                    continue;
                }

                int maximumMarks = schedule.getMaxMarks() == null
                        ? 0 : schedule.getMaxMarks();

                int[] values = totals.computeIfAbsent(
                        resultStudent.getId(), key -> new int[]{0, 0});

                values[0] += result.getMarksObtained();
                values[1] += maximumMarks;
            }
        }

        List<Double> percentages = totals.values().stream()
                .filter(values -> values[1] > 0)
                .map(values -> values[0] * 100.0 / values[1])
                .sorted(Comparator.reverseOrder())
                .toList();

        for (int i = 0; i < percentages.size(); i++) {
            if (Math.abs(percentages.get(i) - ownPercentage) < 0.005) {
                return i + 1;
            }
        }

        return null;
    }

    private String calculateGrade(Double percentage, Long schoolId) {

        if (percentage == null) {
            return null;
        }

        List<GradeRule> rules =
                gradeRuleRepository
                        .findAllBySchool_IdOrderByMinimumPercentageDesc(
                                schoolId);

        for (GradeRule rule : rules) {

            Double minimum = rule.getMinimumPercentage();
            Double maximum = rule.getMaximumPercentage();

            if (minimum != null
                    && percentage >= minimum
                    && (maximum == null || percentage <= maximum)) {
                return rule.getGrade();
            }
        }

        return null;
    }

    @Override
    public byte[] download(
            Long studentId,
            Long examId,
            boolean includeUnpublished) {

        ReportCardDto report =
                generate(studentId, examId, includeUnpublished);

        StringBuilder builder = new StringBuilder("REPORT CARD\n")
                .append("Student: ")
                .append(report.getStudentName())
                .append(" (")
                .append(report.getRollNumber())
                .append(")\n")
                .append("Class: ")
                .append(report.getClassName())
                .append(" / ")
                .append(report.getSectionName())
                .append("\n")
                .append("Examination: ")
                .append(report.getExaminationName())
                .append("\n\n")
                .append("Subject,Marks,Maximum,Percentage,Grade\n");

        report.getSubjects().forEach(subject ->
                builder.append(subject.getSubject()).append(',')
                        .append(subject.getMarksObtained()).append(',')
                        .append(subject.getMaximumMarks()).append(',')
                        .append(subject.getPercentage()).append(',')
                        .append(subject.getGrade()).append('\n'));

        builder.append("\nTotal: ")
                .append(report.getTotalMarks())
                .append('/')
                .append(report.getTotalMaximumMarks())
                .append("\nPercentage: ")
                .append(report.getPercentage())
                .append("\nGrade: ")
                .append(report.getGrade())
                .append("\nAttendance: ")
                .append(report.getAttendancePresent())
                .append('/')
                .append(report.getAttendanceTotal())
                .append(" (")
                .append(report.getAttendancePercentage())
                .append("%)\nResult: ")
                .append(report.getOverallResult());

        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }
}
