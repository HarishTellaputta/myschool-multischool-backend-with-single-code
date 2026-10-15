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

    @Override
    public ReportCardDto generate(Long studentId, Long examId, boolean includeUnpublished) {

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Student not found: " + studentId));

        Examination exam = examinationRepository.findById(examId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Examination not found: " + examId));

        List<ExamSchedule> schedules =
                scheduleRepository.findByExaminationId(examId);

        List<ReportCardSubjectDto> subjects = new ArrayList<>();

        for (ExamSchedule s : schedules) {

            // Student must have a section
            if (student.getSection() == null) {
                continue;
            }

            // Schedule class must match student's class
            if (!s.getClassEntity().getId()
                    .equals(student.getSection().getClassEntity().getId())) {
                continue;
            }

            // If schedule has section, it must match student's section
            if (s.getSection() != null
                    && !s.getSection().getId()
                    .equals(student.getSection().getId())) {
                continue;
            }

            var result = resultRepository
                    .findByExamScheduleIdAndStudentId(s.getId(), studentId)
                    .orElse(null);

            // No result
            if (result == null) {
                continue;
            }

            // Hide unpublished result unless explicitly requested
            if (!includeUnpublished
                    && !Boolean.TRUE.equals(result.getIsPublished())) {
                continue;
            }

            // Subject comes directly from ExamSchedule -> Subject relation
            String subjectName = s.getSubject() != null
                    ? s.getSubject().getName()
                    : "Unknown Subject";

            subjects.add(
                    ReportCardSubjectDto.builder()
                            .scheduleId(s.getId())
                            .subject(subjectName)
                            .examDate(s.getExamDate())
                            .marksObtained(result.getMarksObtained())
                            .maximumMarks(s.getMaxMarks())
                            .percentage(result.getPercentage())
                            .grade(calculateGrade(result.getPercentage()))
                            .build()
            );
        }

        int total = subjects.stream()
                .mapToInt(x ->
                        x.getMarksObtained() == null
                                ? 0
                                : x.getMarksObtained())
                .sum();

        int max = subjects.stream()
                .mapToInt(x ->
                        x.getMaximumMarks() == null
                                ? 0
                                : x.getMaximumMarks())
                .sum();

        double pct = max == 0
                ? 0
                : Math.round(total * 10000.0 / max) / 100.0;

//        String grade = gradeRuleRepository
//                .findAllByOrderByMinimumPercentageDesc()
//                .stream()
//                .filter(g ->
//                        pct >= g.getMinimumPercentage()
//                                && pct <= g.getMaximumPercentage())
//                .map(GradeRule::getGrade)
//                .findFirst()
//                .orElse("UNDEFINED");
        String grade = calculateGrade(pct);

        // Attendance
        List<Attendance> attendance;

        if (student.getAcademicYear() == null
                && student.getSection() != null) {

            attendance = attendanceRepository
                    .findByStudentIdAndClassId(
                            studentId,
                            student.getSection()
                                    .getClassEntity()
                                    .getId()
                    );

        } else if (student.getAcademicYear() != null) {

            attendance = attendanceRepository
                    .findByStudentIdAndAttendanceDateBetween(
                            studentId,
                            student.getAcademicYear().getStartDate(),
                            student.getAcademicYear().getEndDate()
                    );

        } else {

            attendance = attendanceRepository
                    .findByStudentId(studentId);
        }

        long present = attendance.stream()
                .filter(a ->
                        "PRESENT".equalsIgnoreCase(a.getStatus()))
                .count();

        Integer rank = calculateOverallRank(
                student,
                examId,
                includeUnpublished,
                pct
        );

        return ReportCardDto.builder()
                .studentId(studentId)
                .studentName(student.getName())
                .rollNumber(student.getRollNumber())

                .classId(
                        student.getSection() == null
                                ? null
                                : student.getSection()
                                .getClassEntity()
                                .getId()
                )

                .className(
                        student.getSection() == null
                                ? null
                                : student.getSection()
                                .getClassEntity()
                                .getName()
                )

                .sectionId(
                        student.getSection() == null
                                ? null
                                : student.getSection().getId()
                )

                .sectionName(
                        student.getSection() == null
                                ? null
                                : student.getSection().getName()
                )

                .examinationId(examId)
                .examinationName(exam.getName())

                .subjects(subjects)

                .totalMarks(total)
                .totalMaximumMarks(max)

                .percentage(pct)
                .grade(grade)
                .rank(rank)

                .attendancePresent(present)
                .attendanceTotal((long) attendance.size())

                .attendancePercentage(
                        attendance.isEmpty()
                                ? 0
                                : Math.round(
                                present * 10000.0
                                        / attendance.size()
                        ) / 100.0
                )

                .overallResult(
                        subjects.isEmpty()
                                ? "PENDING"
                                : subjects.stream()
                                .anyMatch(x ->
                                        "F".equalsIgnoreCase(
                                                x.getGrade()
                                        ))
                                ? "FAIL"
                                : "PASS"
                )

                .build();
    }
    private Integer calculateOverallRank(Student student, Long examId, boolean includeUnpublished, double ownPercentage) {
        if (student.getSection() == null) return null;
        Map<Long, int[]> totals = new HashMap<>();
        for (ExamSchedule s : scheduleRepository.findByExaminationId(examId)) {
            if (s.getSection() != null && !s.getSection().getId().equals(student.getSection().getId())) continue;
            for (ExamResult x : resultRepository.findByExamScheduleId(s.getId())) {
                if (!includeUnpublished && !Boolean.TRUE.equals(x.getIsPublished())) continue;
                if (x.getStudent().getSection() == null || !x.getStudent().getSection().getId().equals(student.getSection().getId()) || x.getMarksObtained() == null)
                    continue;
                int[] v = totals.computeIfAbsent(x.getStudent().getId(), k -> new int[]{0, 0});
                v[0] += x.getMarksObtained();
                v[1] += s.getMaxMarks();
            }
        }
        List<Double> percentages = totals.values().stream().filter(v -> v[1] > 0).map(v -> v[0] * 100.0 / v[1]).sorted(Comparator.reverseOrder()).toList();
        for (int i = 0; i < percentages.size(); i++)
            if (Math.abs(percentages.get(i) - ownPercentage) < 0.005) return i + 1;
        return null;
    }
    private String calculateGrade(Double percentage) {
        if (percentage == null) {
            return null;
        }

        List<GradeRule> rules =
                gradeRuleRepository
                        .findAllByOrderByMinimumPercentageDesc();

        for (GradeRule rule : rules) {

            if (rule.getMinimumPercentage() != null
                    && percentage >= rule.getMinimumPercentage()) {

                return rule.getGrade();
            }
        }

        return null;
    }
    @Override
    public byte[] download(Long studentId, Long examId, boolean includeUnpublished) {
        ReportCardDto r = generate(studentId, examId, includeUnpublished);
        StringBuilder b = new StringBuilder("REPORT CARD\nStudent: ").append(r.getStudentName()).append(" (" + r.getRollNumber() + ")\nClass: ").append(r.getClassName()).append(" / ").append(r.getSectionName()).append("\nExamination: ").append(r.getExaminationName()).append("\n\nSubject,Marks,Maximum,Percentage,Grade\n");
        r.getSubjects().forEach(s -> b.append(s.getSubject()).append(',').append(s.getMarksObtained()).append(',').append(s.getMaximumMarks()).append(',').append(s.getPercentage()).append(',').append(s.getGrade()).append('\n'));
        b.append("\nTotal: ").append(r.getTotalMarks()).append('/').append(r.getTotalMaximumMarks()).append("\nPercentage: ").append(r.getPercentage()).append("\nGrade: ").append(r.getGrade()).append("\nAttendance: ").append(r.getAttendancePresent()).append('/').append(r.getAttendanceTotal()).append(" ( ").append(r.getAttendancePercentage()).append("%)\nResult: ").append(r.getOverallResult());
        return b.toString().getBytes(StandardCharsets.UTF_8);
    }
}
