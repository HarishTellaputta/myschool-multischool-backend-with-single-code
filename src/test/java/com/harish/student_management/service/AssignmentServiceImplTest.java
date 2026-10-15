
        package com.harish.student_management.service;

import com.harish.student_management.dto.AssignmentRequestDto;
import com.harish.student_management.dto.AssignmentResponseDto;
import com.harish.student_management.entity.Assignment;
import com.harish.student_management.entity.ClassEntity;
import com.harish.student_management.entity.Section;
import com.harish.student_management.entity.Subject;
import com.harish.student_management.entity.Teacher;
import com.harish.student_management.exception.ResourceNotFoundException;
import com.harish.student_management.repository.AssignmentRepository;
import com.harish.student_management.repository.ClassRepository;
import com.harish.student_management.repository.ClassSubjectRepository;
import com.harish.student_management.repository.SectionRepository;
import com.harish.student_management.repository.SubjectRepository;
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
class AssignmentServiceImplTest {

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private ClassRepository classRepository;

    @Mock
    private ClassSubjectRepository classSubjectRepository;

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private TeacherAccessService teacherAccessService;

    @InjectMocks
    private AssignmentServiceImpl assignmentService;

    private Assignment assignment;
    private ClassEntity classEntity;
    private Section section;
    private Teacher teacher;
    private Subject subject;

    @BeforeEach
    void setUp() {

        classEntity = ClassEntity.builder()
                .id(1L)
                .name("Class 10")
                .code("C10")
                .build();

        section = Section.builder()
                .id(1L)
                .name("Section A")
                .classEntity(classEntity)
                .build();

        teacher = Teacher.builder()
                .id(1L)
                .name("Teacher Smith")
                .employeeId("T001")
                .build();

        subject = Subject.builder()
                .id(1L)
                .name("Mathematics")
                .code("MATH")
                .status("ACTIVE")
                .build();

        assignment = Assignment.builder()
                .id(1L)
                .classEntity(classEntity)
                .section(section)
                .assignedByTeacher(teacher)
                .subject(subject)
                .title("Calculus Project")
                .description("Complete the calculus project chapter 5")
                .assignedDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(14))
                .status("ASSIGNED")
                .priority("HIGH")
                .submissionType("ONLINE")
                .maxMarks(100)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ============================================================
    // CREATE
    // ============================================================

    @Test
    void testCreateAssignment_Success() {

        AssignmentRequestDto request = AssignmentRequestDto.builder()
                .classId(1L)
                .sectionId(1L)
                .teacherId(1L)
                .subjectId(1L)
                .title("Calculus Project")
                .description("Complete the calculus project chapter 5")
                .assignedDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(14))
                .priority("HIGH")
                .submissionType("ONLINE")
                .maxMarks(100)
                .build();

        when(classRepository.findById(1L))
                .thenReturn(Optional.of(classEntity));

        when(teacherRepository.findById(1L))
                .thenReturn(Optional.of(teacher));

        when(subjectRepository.findById(1L))
                .thenReturn(Optional.of(subject));

        when(classSubjectRepository
                .existsByClassEntityIdAndSubjectId(1L, 1L))
                .thenReturn(true);

        when(sectionRepository.findById(1L))
                .thenReturn(Optional.of(section));

        when(assignmentRepository.save(any(Assignment.class)))
                .thenReturn(assignment);

        AssignmentResponseDto result =
                assignmentService.createAssignment(request);

        assertNotNull(result);
        assertEquals("Calculus Project", result.getTitle());

        assertEquals(1L, result.getSubjectId());
        assertEquals("Mathematics", result.getSubjectName());
        assertEquals("MATH", result.getSubjectCode());

        assertEquals(
                "Teacher Smith",
                result.getAssignedByTeacherName()
        );

        assertEquals(100, result.getMaxMarks());

        verify(assignmentRepository)
                .save(any(Assignment.class));

        verify(subjectRepository)
                .findById(1L);

        verify(classSubjectRepository)
                .existsByClassEntityIdAndSubjectId(1L, 1L);

        verify(teacherAccessService)
                .validateTeacherAccessToClassAndSubject(
                        1L,
                        1L,
                        1L
                );
    }

    // ============================================================
    // CREATE - PAST DUE DATE
    // ============================================================

    @Test
    void testCreateAssignment_PastDueDate() {

        AssignmentRequestDto request = AssignmentRequestDto.builder()
                .classId(1L)
                .teacherId(1L)
                .subjectId(1L)
                .title("Calculus Project")
                .dueDate(LocalDate.now().minusDays(1))
                .build();

        when(classRepository.findById(1L))
                .thenReturn(Optional.of(classEntity));

        when(teacherRepository.findById(1L))
                .thenReturn(Optional.of(teacher));

        when(subjectRepository.findById(1L))
                .thenReturn(Optional.of(subject));

        when(classSubjectRepository
                .existsByClassEntityIdAndSubjectId(1L, 1L))
                .thenReturn(true);

        assertThrows(
                IllegalArgumentException.class,
                () -> assignmentService.createAssignment(request)
        );
    }

    // ============================================================
    // CREATE - ASSIGNED DATE AFTER DUE DATE
    // ============================================================

    @Test
    void testCreateAssignment_AssignedDateAfterDueDate() {

        AssignmentRequestDto request = AssignmentRequestDto.builder()
                .classId(1L)
                .teacherId(1L)
                .subjectId(1L)
                .title("Calculus Project")
                .assignedDate(LocalDate.now().plusDays(15))
                .dueDate(LocalDate.now().plusDays(10))
                .build();

        when(classRepository.findById(1L))
                .thenReturn(Optional.of(classEntity));

        when(teacherRepository.findById(1L))
                .thenReturn(Optional.of(teacher));

        when(subjectRepository.findById(1L))
                .thenReturn(Optional.of(subject));

        when(classSubjectRepository
                .existsByClassEntityIdAndSubjectId(1L, 1L))
                .thenReturn(true);

        assertThrows(
                IllegalArgumentException.class,
                () -> assignmentService.createAssignment(request)
        );
    }

    // ============================================================
    // CREATE - CLASS NOT FOUND
    // ============================================================

    @Test
    void testCreateAssignment_ClassNotFound() {

        AssignmentRequestDto request = AssignmentRequestDto.builder()
                .classId(1L)
                .teacherId(1L)
                .subjectId(1L)
                .title("Calculus Project")
                .dueDate(LocalDate.now().plusDays(14))
                .build();

        when(classRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> assignmentService.createAssignment(request)
        );

        verify(assignmentRepository, never())
                .save(any(Assignment.class));
    }

    // ============================================================
    // GET BY ID
    // ============================================================

    @Test
    void testGetAssignmentById_Success() {

        when(assignmentRepository.findById(1L))
                .thenReturn(Optional.of(assignment));

        AssignmentResponseDto result =
                assignmentService.getAssignmentById(1L);

        assertNotNull(result);
        assertEquals("Calculus Project", result.getTitle());

        assertEquals(1L, result.getSubjectId());
        assertEquals("Mathematics", result.getSubjectName());
        assertEquals("MATH", result.getSubjectCode());

        assertEquals("ASSIGNED", result.getStatus());
    }

    // ============================================================
    // GET BY ID - NOT FOUND
    // ============================================================

    @Test
    void testGetAssignmentById_NotFound() {

        when(assignmentRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> assignmentService.getAssignmentById(1L)
        );
    }

    // ============================================================
    // GET ALL
    // ============================================================

    @Test
    void testGetAllAssignments_Success() {

        when(assignmentRepository.findAll())
                .thenReturn(Arrays.asList(assignment));

        List<AssignmentResponseDto> result =
                assignmentService.getAllAssignments();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(
                "Calculus Project",
                result.get(0).getTitle()
        );

        assertEquals(
                1L,
                result.get(0).getSubjectId()
        );
    }

    // ============================================================
    // GET BY CLASS
    // ============================================================

    @Test
    void testGetAssignmentsByClassId_Success() {

        when(classRepository.findById(1L))
                .thenReturn(Optional.of(classEntity));

        when(assignmentRepository.findByClassEntityId(1L))
                .thenReturn(Arrays.asList(assignment));

        List<AssignmentResponseDto> result =
                assignmentService.getAssignmentsByClassId(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(
                "Calculus Project",
                result.get(0).getTitle()
        );
    }

    // ============================================================
    // GET BY CLASS - NOT FOUND
    // ============================================================

    @Test
    void testGetAssignmentsByClassId_ClassNotFound() {

        when(classRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> assignmentService.getAssignmentsByClassId(1L)
        );
    }

    // ============================================================
    // GET BY SECTION
    // ============================================================

    @Test
    void testGetAssignmentsBySectionId_Success() {

        when(sectionRepository.findById(1L))
                .thenReturn(Optional.of(section));

        when(assignmentRepository.findBySectionId(1L))
                .thenReturn(Arrays.asList(assignment));

        List<AssignmentResponseDto> result =
                assignmentService.getAssignmentsBySectionId(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // ============================================================
    // GET BY TEACHER
    // ============================================================

    @Test
    void testGetAssignmentsByTeacherId_Success() {

        when(teacherRepository.findById(1L))
                .thenReturn(Optional.of(teacher));

        when(assignmentRepository.findByAssignedByTeacherId(1L))
                .thenReturn(Arrays.asList(assignment));

        List<AssignmentResponseDto> result =
                assignmentService.getAssignmentsByTeacherId(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // ============================================================
    // GET BY SUBJECT ID
    // ============================================================

    @Test
    void testGetAssignmentsBySubject_Success() {

        when(subjectRepository.findById(1L))
                .thenReturn(Optional.of(subject));

        when(assignmentRepository.findBySubjectId(1L))
                .thenReturn(Arrays.asList(assignment));

        List<AssignmentResponseDto> result =
                assignmentService.getAssignmentsBySubject(1L);

        assertNotNull(result);
        assertEquals(1, result.size());

        assertEquals(
                1L,
                result.get(0).getSubjectId()
        );

        assertEquals(
                "Mathematics",
                result.get(0).getSubjectName()
        );

        assertEquals(
                "MATH",
                result.get(0).getSubjectCode()
        );
    }

    // ============================================================
    // GET BY SUBJECT ID - NOT FOUND
    // ============================================================

    @Test
    void testGetAssignmentsBySubject_SubjectNotFound() {

        when(subjectRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> assignmentService.getAssignmentsBySubject(1L)
        );
    }

    // ============================================================
    // GET BY CLASS + DATE RANGE
    // ============================================================

    @Test
    void testGetAssignmentsByClassIdAndDueDateRange_Success() {

        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().plusDays(30);

        when(classRepository.findById(1L))
                .thenReturn(Optional.of(classEntity));

        when(assignmentRepository.findByClassIdAndDueDateRange(
                1L,
                startDate,
                endDate
        )).thenReturn(Arrays.asList(assignment));

        List<AssignmentResponseDto> result =
                assignmentService.getAssignmentsByClassIdAndDueDateRange(
                        1L,
                        startDate,
                        endDate
                );

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // ============================================================
    // GET BY CLASS + DATE RANGE - INVALID
    // ============================================================

    @Test
    void testGetAssignmentsByClassIdAndDueDateRange_InvalidDateRange() {

        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().minusDays(7);

        when(classRepository.findById(1L))
                .thenReturn(Optional.of(classEntity));

        assertThrows(
                IllegalArgumentException.class,
                () -> assignmentService
                        .getAssignmentsByClassIdAndDueDateRange(
                                1L,
                                startDate,
                                endDate
                        )
        );
    }

    // ============================================================
    // GET ACTIVE
    // ============================================================

    @Test
    void testGetActiveAssignmentsByClassId_Success() {

        when(classRepository.findById(1L))
                .thenReturn(Optional.of(classEntity));

        when(assignmentRepository
                .findByClassEntityIdAndDueDateAfter(
                        1L,
                        LocalDate.now()
                ))
                .thenReturn(Arrays.asList(assignment));

        List<AssignmentResponseDto> result =
                assignmentService.getActiveAssignmentsByClassId(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // ============================================================
    // GET BY CLASS + SUBJECT ID
    // ============================================================

    @Test
    void testGetAssignmentsByClassIdAndSubject_Success() {

        when(classRepository.findById(1L))
                .thenReturn(Optional.of(classEntity));

        when(subjectRepository.findById(1L))
                .thenReturn(Optional.of(subject));

        when(classSubjectRepository
                .existsByClassEntityIdAndSubjectId(1L, 1L))
                .thenReturn(true);

        when(assignmentRepository
                .findByClassEntityIdAndSubjectIdOrderByDueDate(
                        1L,
                        1L
                ))
                .thenReturn(Arrays.asList(assignment));

        List<AssignmentResponseDto> result =
                assignmentService.getAssignmentsByClassIdAndSubject(
                        1L,
                        1L
                );

        assertNotNull(result);
        assertEquals(1, result.size());

        assertEquals(
                1L,
                result.get(0).getSubjectId()
        );

        assertEquals(
                "Mathematics",
                result.get(0).getSubjectName()
        );

        assertEquals(
                "MATH",
                result.get(0).getSubjectCode()
        );
    }

    // ============================================================
    // GET BY STATUS
    // ============================================================

    @Test
    void testGetAssignmentsByStatus_Success() {

        when(assignmentRepository.findByStatus("ASSIGNED"))
                .thenReturn(Arrays.asList(assignment));

        List<AssignmentResponseDto> result =
                assignmentService.getAssignmentsByStatus("ASSIGNED");

        assertNotNull(result);
        assertEquals(1, result.size());

        assertEquals(
                "ASSIGNED",
                result.get(0).getStatus()
        );
    }

    // ============================================================
    // UPDATE
    // ============================================================

    @Test
    void testUpdateAssignment_Success() {

        AssignmentRequestDto request =
                AssignmentRequestDto.builder()
                        .classId(1L)
                        .sectionId(1L)
                        .teacherId(1L)
                        .subjectId(1L)
                        .title("Physics Lab Report")
                        .dueDate(LocalDate.now().plusDays(21))
                        .build();

        when(assignmentRepository.findById(1L))
                .thenReturn(Optional.of(assignment));

        when(teacherRepository.findById(1L))
                .thenReturn(Optional.of(teacher));

        when(classRepository.findById(1L))
                .thenReturn(Optional.of(classEntity));

        when(subjectRepository.findById(1L))
                .thenReturn(Optional.of(subject));

        when(classSubjectRepository
                .existsByClassEntityIdAndSubjectId(1L, 1L))
                .thenReturn(true);

        when(sectionRepository.findById(1L))
                .thenReturn(Optional.of(section));

        when(assignmentRepository.save(any(Assignment.class)))
                .thenReturn(assignment);

        AssignmentResponseDto result =
                assignmentService.updateAssignment(
                        1L,
                        request
                );

        assertNotNull(result);

        verify(assignmentRepository)
                .save(any(Assignment.class));

        verify(teacherAccessService)
                .validateTeacherAccessToClassAndSubject(
                        1L,
                        1L,
                        1L
                );
    }

    // ============================================================
    // UPDATE - PAST DUE DATE
    // ============================================================

    @Test
    void testUpdateAssignment_PastDueDate() {

        AssignmentRequestDto request =
                AssignmentRequestDto.builder()
                        .classId(1L)
                        .teacherId(1L)
                        .subjectId(1L)
                        .dueDate(LocalDate.now().minusDays(1))
                        .build();

        when(assignmentRepository.findById(1L))
                .thenReturn(Optional.of(assignment));

        when(teacherRepository.findById(1L))
                .thenReturn(Optional.of(teacher));

        when(classRepository.findById(1L))
                .thenReturn(Optional.of(classEntity));

        when(subjectRepository.findById(1L))
                .thenReturn(Optional.of(subject));

        when(classSubjectRepository
                .existsByClassEntityIdAndSubjectId(1L, 1L))
                .thenReturn(true);

        assertThrows(
                IllegalArgumentException.class,
                () -> assignmentService.updateAssignment(
                        1L,
                        request
                )
        );
    }

    // ============================================================
    // DELETE
    // ============================================================

    @Test
    void testDeleteAssignment_Success() {

        when(assignmentRepository.findById(1L))
                .thenReturn(Optional.of(assignment));

        assignmentService.deleteAssignment(1L);

        verify(assignmentRepository)
                .delete(assignment);
    }

    // ============================================================
    // DELETE - NOT FOUND
    // ============================================================

    @Test
    void testDeleteAssignment_NotFound() {

        when(assignmentRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> assignmentService.deleteAssignment(1L)
        );
    }

    // ============================================================
    // CREATE WITHOUT SECTION
    // ============================================================

    @Test
    void testCreateAssignment_WithoutSection_Success() {

        AssignmentRequestDto request =
                AssignmentRequestDto.builder()
                        .classId(1L)
                        .teacherId(1L)
                        .subjectId(1L)
                        .title("Calculus Project")
                        .dueDate(LocalDate.now().plusDays(14))
                        .build();

        when(classRepository.findById(1L))
                .thenReturn(Optional.of(classEntity));

        when(teacherRepository.findById(1L))
                .thenReturn(Optional.of(teacher));

        when(subjectRepository.findById(1L))
                .thenReturn(Optional.of(subject));

        when(classSubjectRepository
                .existsByClassEntityIdAndSubjectId(1L, 1L))
                .thenReturn(true);

        when(assignmentRepository.save(any(Assignment.class)))
                .thenReturn(assignment);

        AssignmentResponseDto result =
                assignmentService.createAssignment(request);

        assertNotNull(result);

        assertEquals(
                "Calculus Project",
                result.getTitle()
        );

        assertEquals(
                1L,
                result.getSubjectId()
        );

        verify(assignmentRepository)
                .save(any(Assignment.class));

        verify(teacherAccessService)
                .validateTeacherAccessToClassAndSubject(
                        1L,
                        1L,
                        1L
                );
    }

    // ============================================================
    // CREATE WITHOUT ASSIGNED DATE
    // ============================================================

    @Test
    void testCreateAssignment_WithoutAssignedDate_Success() {

        AssignmentRequestDto request =
                AssignmentRequestDto.builder()
                        .classId(1L)
                        .teacherId(1L)
                        .subjectId(1L)
                        .title("Calculus Project")
                        .dueDate(LocalDate.now().plusDays(14))
                        .build();

        when(classRepository.findById(1L))
                .thenReturn(Optional.of(classEntity));

        when(teacherRepository.findById(1L))
                .thenReturn(Optional.of(teacher));

        when(subjectRepository.findById(1L))
                .thenReturn(Optional.of(subject));

        when(classSubjectRepository
                .existsByClassEntityIdAndSubjectId(1L, 1L))
                .thenReturn(true);

        when(assignmentRepository.save(any(Assignment.class)))
                .thenReturn(assignment);

        AssignmentResponseDto result =
                assignmentService.createAssignment(request);

        assertNotNull(result);

        assertEquals(
                "Calculus Project",
                result.getTitle()
        );

        assertNotNull(result.getAssignedDate());

        verify(assignmentRepository)
                .save(any(Assignment.class));
    }
}