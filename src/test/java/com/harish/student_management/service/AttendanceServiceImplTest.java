package com.harish.student_management.service;

import com.harish.student_management.dto.AttendanceReportDto;
import com.harish.student_management.dto.AttendanceRequestDto;
import com.harish.student_management.dto.AttendanceResponseDto;
import com.harish.student_management.dto.AttendanceUpdateDto;
import com.harish.student_management.dto.BulkAttendanceRequestDto;
import com.harish.student_management.entity.Attendance;
import com.harish.student_management.entity.ClassEntity;
import com.harish.student_management.entity.Student;
import com.harish.student_management.entity.Teacher;
import com.harish.student_management.exception.ResourceNotFoundException;
import com.harish.student_management.repository.AttendanceRepository;
import com.harish.student_management.repository.ClassRepository;
import com.harish.student_management.repository.StudentRepository;
import com.harish.student_management.repository.TeacherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AttendanceServiceImplTest {

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private ClassRepository classRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private TeacherAccessService teacherAccessService;

    @InjectMocks
    private AttendanceServiceImpl attendanceService;

    private Student student;
    private ClassEntity classEntity;
    private Teacher teacher;
    private Attendance attendance;

    @BeforeEach
    void setUp() {
        student = Student.builder()
                .id(1L)
                .name("John Doe")
                .rollNumber("R001")
                .build();

        classEntity = ClassEntity.builder()
                .id(1L)
                .name("Class 10")
                .code("C10")
                .build();

        teacher = Teacher.builder()
                .id(1L)
                .name("Teacher Smith")
                .employeeId("T001")
                .build();

        attendance = Attendance.builder()
                .id(1L)
                .student(student)
                .classEntity(classEntity)
                .markedByTeacher(teacher)
                .attendanceDate(LocalDate.now())
                .status("PRESENT")
                .remarks("On time")
                .markedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testMarkAttendance_Success() {
        AttendanceRequestDto request = AttendanceRequestDto.builder()
                .studentId(1L)
                .classId(1L)
                .teacherId(1L)
                .attendanceDate(LocalDate.now())
                .status("PRESENT")
                .remarks("On time")
                .build();

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(classRepository.findById(1L)).thenReturn(Optional.of(classEntity));
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(teacher));
        when(attendanceRepository.existsByStudentIdAndClassIdAndAttendanceDate(any(), any(), any())).thenReturn(false);
        when(attendanceRepository.save(any(Attendance.class))).thenReturn(attendance);

        AttendanceResponseDto result = attendanceService.markAttendance(request);

        assertNotNull(result);
        assertEquals("PRESENT", result.getStatus());
        assertEquals("John Doe", result.getStudentName());
        verify(attendanceRepository).save(any(Attendance.class));
    }

    @Test
    void testMarkAttendance_DuplicateAttendance() {
        AttendanceRequestDto request = AttendanceRequestDto.builder()
                .studentId(1L)
                .classId(1L)
                .teacherId(1L)
                .attendanceDate(LocalDate.now())
                .status("PRESENT")
                .build();

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(classRepository.findById(1L)).thenReturn(Optional.of(classEntity));
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(teacher));
        when(attendanceRepository.existsByStudentIdAndClassIdAndAttendanceDate(any(), any(), any())).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> attendanceService.markAttendance(request));
    }

    @Test
    void testMarkAttendance_InvalidStatus() {
        AttendanceRequestDto request = AttendanceRequestDto.builder()
                .studentId(1L)
                .classId(1L)
                .teacherId(1L)
                .attendanceDate(LocalDate.now())
                .status("INVALID")
                .build();

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(classRepository.findById(1L)).thenReturn(Optional.of(classEntity));
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(teacher));
        assertThrows(IllegalArgumentException.class, () -> attendanceService.markAttendance(request));
    }

    @Test
    void testMarkAttendance_StudentNotFound() {
        AttendanceRequestDto request = AttendanceRequestDto.builder()
                .studentId(1L)
                .classId(1L)
                .teacherId(1L)
                .attendanceDate(LocalDate.now())
                .status("PRESENT")
                .build();

        when(studentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> attendanceService.markAttendance(request));
    }

    @Test
    void testUpdateAttendance_Success() {
        AttendanceUpdateDto updateDto = AttendanceUpdateDto.builder()
                .teacherId(1L)
                .status("ABSENT")
                .remarks("Sick leave")
                .build();

        when(attendanceRepository.findById(1L)).thenReturn(Optional.of(attendance));
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(teacher));
        when(attendanceRepository.save(any(Attendance.class))).thenReturn(attendance);

        AttendanceResponseDto result = attendanceService.updateAttendance(1L, updateDto);

        assertNotNull(result);
        verify(attendanceRepository).save(any(Attendance.class));
    }

    @Test
    void testUpdateAttendance_InvalidStatus() {
        AttendanceUpdateDto updateDto = AttendanceUpdateDto.builder()
                .teacherId(1L)
                .status("INVALID")
                .build();

        when(attendanceRepository.findById(1L)).thenReturn(Optional.of(attendance));
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(teacher));

        assertThrows(IllegalArgumentException.class, () -> attendanceService.updateAttendance(1L, updateDto));
    }

    @Test
    void testGetAttendanceById_Success() {
        when(attendanceRepository.findById(1L)).thenReturn(Optional.of(attendance));

        AttendanceResponseDto result = attendanceService.getAttendanceById(1L);

        assertNotNull(result);
        assertEquals("PRESENT", result.getStatus());
        assertEquals("John Doe", result.getStudentName());
    }

    @Test
    void testGetAttendanceById_NotFound() {
        when(attendanceRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> attendanceService.getAttendanceById(1L));
    }

    @Test
    void testDeleteAttendance_Success() {
        when(attendanceRepository.findById(1L)).thenReturn(Optional.of(attendance));

        attendanceService.deleteAttendance(1L);

        verify(attendanceRepository).delete(attendance);
    }

    @Test
    void testGetStudentAttendance_Success() {
        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(attendanceRepository.findByStudentIdAndAttendanceDateBetween(1L, startDate, endDate))
                .thenReturn(Arrays.asList(attendance));

        List<AttendanceResponseDto> result = attendanceService.getStudentAttendance(1L, startDate, endDate);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("PRESENT", result.get(0).getStatus());
    }

    @Test
    void testGetStudentAttendance_InvalidDateRange() {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().minusDays(7);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));

        assertThrows(IllegalArgumentException.class, () -> 
            attendanceService.getStudentAttendance(1L, startDate, endDate));
    }

    @Test
    void testGetClassAttendance_Success() {
        LocalDate date = LocalDate.now();

        when(classRepository.findById(1L)).thenReturn(Optional.of(classEntity));
        when(attendanceRepository.findByClassIdAndAttendanceDate(1L, date))
                .thenReturn(Arrays.asList(attendance));

        List<AttendanceResponseDto> result = attendanceService.getClassAttendance(1L, date);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetStudentAttendanceReport_Success() {
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now();

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(classRepository.findById(1L)).thenReturn(Optional.of(classEntity));
        when(attendanceRepository.countByStudentIdAndClassIdAndDateRange(1L, 1L, startDate, endDate)).thenReturn(20L);
        when(attendanceRepository.countByStudentIdAndClassIdAndDateRangeAndStatus(1L, 1L, startDate, endDate, "PRESENT")).thenReturn(18L);
        when(attendanceRepository.countByStudentIdAndClassIdAndDateRangeAndStatus(1L, 1L, startDate, endDate, "ABSENT")).thenReturn(1L);
        when(attendanceRepository.countByStudentIdAndClassIdAndDateRangeAndStatus(1L, 1L, startDate, endDate, "LEAVE")).thenReturn(1L);

        AttendanceReportDto result = attendanceService.getStudentAttendanceReport(1L, 1L, startDate, endDate);

        assertNotNull(result);
        assertEquals(20, result.getTotalDays());
        assertEquals(18, result.getPresentDays());
        assertEquals(1, result.getAbsentDays());
        assertEquals(1, result.getLeaveDays());
        assertEquals(90.0, result.getAttendancePercentage());
    }

    @Test
    void testCalculateAttendancePercentage_Success() {
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now();

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(classRepository.findById(1L)).thenReturn(Optional.of(classEntity));
        when(attendanceRepository.countByStudentIdAndClassIdAndDateRange(1L, 1L, startDate, endDate)).thenReturn(20L);
        when(attendanceRepository.countByStudentIdAndClassIdAndDateRangeAndStatus(1L, 1L, startDate, endDate, "PRESENT")).thenReturn(18L);

        double result = attendanceService.calculateAttendancePercentage(1L, 1L, startDate, endDate);

        assertEquals(90.0, result);
    }

    @Test
    void testCalculateAttendancePercentage_ZeroTotalDays() {
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now();

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(classRepository.findById(1L)).thenReturn(Optional.of(classEntity));
        when(attendanceRepository.countByStudentIdAndClassIdAndDateRange(1L, 1L, startDate, endDate)).thenReturn(0L);

        double result = attendanceService.calculateAttendancePercentage(1L, 1L, startDate, endDate);

        assertEquals(0.0, result);
    }

    @Test
    void testMarkBulkAttendance_Success() {
        BulkAttendanceRequestDto.StudentAttendanceDto studentRecord = 
            BulkAttendanceRequestDto.StudentAttendanceDto.builder()
                .studentId(1L)
                .status("PRESENT")
                .remarks("On time")
                .build();

        BulkAttendanceRequestDto request = BulkAttendanceRequestDto.builder()
                .classId(1L)
                .teacherId(1L)
                .attendanceDate(LocalDate.now())
                .attendanceRecords(Arrays.asList(studentRecord))
                .build();

        when(classRepository.findById(1L)).thenReturn(Optional.of(classEntity));
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(teacher));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(attendanceRepository.existsByStudentIdAndClassIdAndAttendanceDate(any(), any(), any())).thenReturn(false);
        when(attendanceRepository.save(any(Attendance.class))).thenReturn(attendance);

        List<AttendanceResponseDto> result = attendanceService.markBulkAttendance(request);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(attendanceRepository).save(any(Attendance.class));
    }
}
