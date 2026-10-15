
        package com.harish.student_management.service;

import com.harish.student_management.dto.AdminDashboardDto;
import com.harish.student_management.dto.ExamScheduleResponseDto;
import com.harish.student_management.entity.ExamSchedule;
import com.harish.student_management.repository.AttendanceRepository;
import com.harish.student_management.repository.CalendarEventRepository;
import com.harish.student_management.repository.ExamScheduleRepository;
import com.harish.student_management.repository.HomeworkRepository;
import com.harish.student_management.repository.NotificationRepository;
import com.harish.student_management.repository.StudentFeeRepository;
import com.harish.student_management.repository.StudentRepository;
import com.harish.student_management.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final StudentRepository students;
    private final TeacherRepository teachers;
    private final AttendanceRepository attendance;
    private final StudentFeeRepository fees;
    private final ExamScheduleRepository schedules;
    private final HomeworkRepository homework;
    private final CalendarEventRepository events;
    private final NotificationRepository notifications;

    @Override
    public AdminDashboardDto get() {

        LocalDate today = LocalDate.now();

        long totalStudents = students.count();

        long totalTeachers = teachers.count();

        long presentStudents =
                attendance.countByAttendanceDateAndStatus(
                        today,
                        "PRESENT"
                );

        long absentStudents =
                attendance.countByAttendanceDateAndStatus(
                        today,
                        "ABSENT"
                );

        long admissions =
                students.countByAdmissionDate(today);

        long homeworkCount =
                homework.countByDueDateGreaterThanEqualAndStatus(
                        today,
                        "ACTIVE"
                );

        long eventCount =
                events.countByStartDateLessThanEqualAndEndDateGreaterThanEqualAndStatus(
                        today,
                        today,
                        "PUBLISHED"
                );

        long notificationCount = notifications.count();

        return AdminDashboardDto.builder()
                .totalStudents(totalStudents)
                .totalTeachers(totalTeachers)
                .presentStudents(presentStudents)
                .absentStudents(absentStudents)
                .admissions(admissions)
                .pendingFees(fees.pendingAmount())
                .upcomingExams(
                        schedules
                                .findTop10ByExamDateGreaterThanEqualOrderByExamDateAsc(today)
                                .stream()
                                .map(this::toExamResponse)
                                .toList()
                )
                .homework(homeworkCount)
                .events(eventCount)
                .notifications(notificationCount)
                .build();
    }

    private ExamScheduleResponseDto toExamResponse(ExamSchedule exam) {

        return ExamScheduleResponseDto.builder()

                .id(exam.getId())

                // Examination
                .examinationId(
                        exam.getExamination() != null
                                ? exam.getExamination().getId()
                                : null
                )

                .examinationName(
                        exam.getExamination() != null
                                ? exam.getExamination().getName()
                                : null
                )

                // Class
                .classId(
                        exam.getClassEntity() != null
                                ? exam.getClassEntity().getId()
                                : null
                )

                .className(
                        exam.getClassEntity() != null
                                ? exam.getClassEntity().getName()
                                : null
                )

                // Section
                .sectionId(
                        exam.getSection() != null
                                ? exam.getSection().getId()
                                : null
                )

                .sectionName(
                        exam.getSection() != null
                                ? exam.getSection().getName()
                                : null
                )

                // Subject
                .subjectId(
                        exam.getSubject() != null
                                ? exam.getSubject().getId()
                                : null
                )

                .subjectName(
                        exam.getSubject() != null
                                ? exam.getSubject().getName()
                                : null
                )

                .subjectCode(
                        exam.getSubject() != null
                                ? exam.getSubject().getCode()
                                : null
                )

                // Subject Teacher
                .subjectTeacherId(
                        exam.getSubjectTeacher() != null
                                ? exam.getSubjectTeacher().getId()
                                : null
                )

                .subjectTeacherName(
                        exam.getSubjectTeacher() != null
                                ? exam.getSubjectTeacher().getName()
                                : null
                )

                // Exam details
                .examDate(exam.getExamDate())
                .startTime(exam.getStartTime())
                .duration(exam.getDuration())
                .maxMarks(exam.getMaxMarks())
                .examType(exam.getExamType())
                .roomNumber(exam.getRoomNumber())
                .status(exam.getStatus())

                // Audit fields
                .createdAt(exam.getCreatedAt())
                .updatedAt(exam.getUpdatedAt())

                .build();
    }
}