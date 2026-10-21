
        package com.harish.student_management.service;

import com.harish.student_management.dto.HomeworkRequestDto;
import com.harish.student_management.dto.HomeworkResponseDto;
import com.harish.student_management.entity.ClassEntity;
import com.harish.student_management.entity.Homework;
import com.harish.student_management.entity.Section;
import com.harish.student_management.entity.Subject;
import com.harish.student_management.entity.Teacher;
import com.harish.student_management.exception.ResourceNotFoundException;
import com.harish.student_management.repository.ClassRepository;
import com.harish.student_management.repository.ClassSubjectRepository;
import com.harish.student_management.repository.HomeworkRepository;
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
class HomeworkServiceImplTest {

    @Mock
    private HomeworkRepository homeworkRepository;

    @Mock
    private ClassRepository classRepository;

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private TeacherAccessService teacherAccessService;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private ClassSubjectRepository classSubjectRepository;

    @InjectMocks
    private HomeworkServiceImpl homeworkService;

    private Homework homework;
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

        homework = Homework.builder()
                .id(1L)
                .classEntity(classEntity)
                .section(section)
                .assignedByTeacher(teacher)
                .subject(subject)
                .title("Algebra Exercises")
                .description("Complete exercises 1-10")
                .dueDate(LocalDate.now().plusDays(7))
                .status("ACTIVE")
                .priority("MEDIUM")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testCreateHomework_Success() {

        HomeworkRequestDto request = HomeworkRequestDto.builder()
                .classId(1L)
                .sectionId(1L)
                .teacherId(1L)
                .subjectId(1L)
                .title("Algebra Exercises")
                .description("Complete exercises 1-10")
                .dueDate(LocalDate.now().plusDays(7))
                .build();

        when(classRepository.findById(1L))
                .thenReturn(Optional.of(classEntity));

        when(teacherRepository.findById(1L))
                .thenReturn(Optional.of(teacher));

        when(sectionRepository.findById(1L))
                .thenReturn(Optional.of(section));

        when(subjectRepository.findById(1L))
                .thenReturn(Optional.of(subject));

        when(classSubjectRepository.existsByClassEntityIdAndSubjectId(1L, 1L))
                .thenReturn(true);

        when(homeworkRepository.save(any(Homework.class)))
                .thenReturn(homework);

        when(teacherAccessService.canTeacherAccessClassAndSubject(
                1L,
                1L,
                1L
        )).thenReturn(true);

        HomeworkResponseDto result =
                homeworkService.createHomework(request);

        assertNotNull(result);
        assertEquals("Algebra Exercises", result.getTitle());
        assertEquals(1L, result.getSubjectId());
        assertEquals("Mathematics", result.getSubjectName());
        assertEquals("MATH", result.getSubjectCode());
        assertEquals("Teacher Smith", result.getAssignedByTeacherName());

        verify(homeworkRepository).save(any(Homework.class));
    }

    @Test
    void testCreateHomework_PastDueDate() {

        HomeworkRequestDto request = HomeworkRequestDto.builder()
                .classId(1L)
                .teacherId(1L)
                .subjectId(1L)
                .title("Algebra Exercises")
                .dueDate(LocalDate.now().minusDays(1))
                .build();

        when(classRepository.findById(1L))
                .thenReturn(Optional.of(classEntity));

        when(teacherRepository.findById(1L))
                .thenReturn(Optional.of(teacher));

        assertThrows(
                IllegalArgumentException.class,
                () -> homeworkService.createHomework(request)
        );
    }

    @Test
    void testCreateHomework_ClassNotFound() {

        HomeworkRequestDto request = HomeworkRequestDto.builder()
                .classId(1L)
                .teacherId(1L)
                .subjectId(1L)
                .title("Algebra Exercises")
                .dueDate(LocalDate.now().plusDays(7))
                .build();

        when(classRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> homeworkService.createHomework(request)
        );
    }

    @Test
    void testGetHomeworkById_Success() {

        when(homeworkRepository.findById(1L))
                .thenReturn(Optional.of(homework));

        HomeworkResponseDto result =
                homeworkService.getHomeworkById(1L);

        assertNotNull(result);
        assertEquals("Algebra Exercises", result.getTitle());
        assertEquals(1L, result.getSubjectId());
        assertEquals("Mathematics", result.getSubjectName());
    }

    @Test
    void testGetHomeworkById_NotFound() {

        when(homeworkRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> homeworkService.getHomeworkById(1L)
        );
    }

    @Test
    void testGetAllHomework_Success() {

        when(homeworkRepository.findAll())
                .thenReturn(Arrays.asList(homework));

        List<HomeworkResponseDto> result =
                homeworkService.getAllHomework();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(
                "Algebra Exercises",
                result.get(0).getTitle()
        );
    }

    @Test
    void testGetHomeworkByClassId_Success() {

        when(classRepository.findById(1L))
                .thenReturn(Optional.of(classEntity));

        when(homeworkRepository.findByClassEntityId(1L))
                .thenReturn(Arrays.asList(homework));

        List<HomeworkResponseDto> result =
                homeworkService.getHomeworkByClassId(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(
                "Algebra Exercises",
                result.get(0).getTitle()
        );
    }

    @Test
    void testGetHomeworkByClassId_ClassNotFound() {

        when(classRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> homeworkService.getHomeworkByClassId(1L)
        );
    }

    @Test
    void testGetHomeworkBySectionId_Success() {

        when(sectionRepository.findById(1L))
                .thenReturn(Optional.of(section));

        when(homeworkRepository.findBySectionId(1L))
                .thenReturn(Arrays.asList(homework));

        List<HomeworkResponseDto> result =
                homeworkService.getHomeworkBySectionId(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetHomeworkByTeacherId_Success() {

        when(teacherRepository.findById(1L))
                .thenReturn(Optional.of(teacher));

        when(homeworkRepository.findByAssignedByTeacherId(1L))
                .thenReturn(Arrays.asList(homework));

        List<HomeworkResponseDto> result =
                homeworkService.getHomeworkByTeacherId(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetHomeworkBySubject_Success() {

        when(subjectRepository.findById(1L))
                .thenReturn(Optional.of(subject));

        when(homeworkRepository.findBySubjectId(1L))
                .thenReturn(Arrays.asList(homework));

        List<HomeworkResponseDto> result =
                homeworkService.getHomeworkBySubject(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getSubjectId());
        assertEquals(
                "Mathematics",
                result.get(0).getSubjectName()
        );
    }

    @Test
    void testGetHomeworkByClassIdAndDueDateRange_Success() {

        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().plusDays(30);

        when(classRepository.findById(1L))
                .thenReturn(Optional.of(classEntity));

        when(homeworkRepository.findByClassIdAndDueDateRange(
                1L,
                startDate,
                endDate
        )).thenReturn(Arrays.asList(homework));

        List<HomeworkResponseDto> result =
                homeworkService.getHomeworkByClassIdAndDueDateRange(
                        1L,
                        startDate,
                        endDate
                );

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetHomeworkByClassIdAndDueDateRange_InvalidDateRange() {

        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().minusDays(7);

        when(classRepository.findById(1L))
                .thenReturn(Optional.of(classEntity));

        assertThrows(
                IllegalArgumentException.class,
                () -> homeworkService
                        .getHomeworkByClassIdAndDueDateRange(
                                1L,
                                startDate,
                                endDate
                        )
        );
    }

    @Test
    void testGetActiveHomeworkByClassId_Success() {

        when(classRepository.findById(1L))
                .thenReturn(Optional.of(classEntity));

        when(homeworkRepository.findByClassEntityIdAndDueDateAfter(
                eq(1L),
                any(LocalDate.class)
        )).thenReturn(Arrays.asList(homework));

        List<HomeworkResponseDto> result =
                homeworkService.getActiveHomeworkByClassId(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetHomeworkByClassIdAndSubject_Success() {

        when(classRepository.findById(1L))
                .thenReturn(Optional.of(classEntity));

        when(homeworkRepository
                .findByClassEntityIdAndSubjectIdOrderByDueDate(
                        1L,
                        1L
                ))
                .thenReturn(Arrays.asList(homework));

        List<HomeworkResponseDto> result =
                homeworkService.getHomeworkByClassIdAndSubject(
                        1L,
                        1L
                );

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getSubjectId());
        assertEquals(
                "Mathematics",
                result.get(0).getSubjectName()
        );
    }

    @Test
    void testUpdateHomework_Success() {

        HomeworkRequestDto request = HomeworkRequestDto.builder()
                .classId(1L)
                .sectionId(1L)
                .teacherId(1L)
                .subjectId(1L)
                .title("Newton's Laws")
                .dueDate(LocalDate.now().plusDays(10))
                .build();

        when(homeworkRepository.findById(1L))
                .thenReturn(Optional.of(homework));

        when(teacherRepository.findById(1L))
                .thenReturn(Optional.of(teacher));

        when(classRepository.findById(1L))
                .thenReturn(Optional.of(classEntity));

        when(sectionRepository.findById(1L))
                .thenReturn(Optional.of(section));

        when(subjectRepository.findById(1L))
                .thenReturn(Optional.of(subject));

        when(classSubjectRepository.existsByClassEntityIdAndSubjectId(
                1L,
                1L
        )).thenReturn(true);

        when(teacherAccessService.canTeacherAccessClassAndSubject(
                1L,
                1L,
                1L
        )).thenReturn(true);

        when(homeworkRepository.save(any(Homework.class)))
                .thenReturn(homework);

        HomeworkResponseDto result =
                homeworkService.updateHomework(1L, request);

        assertNotNull(result);

        verify(homeworkRepository)
                .save(any(Homework.class));
    }

    @Test
    void testUpdateHomework_PastDueDate() {

        HomeworkRequestDto request = HomeworkRequestDto.builder()
                .classId(1L)
                .teacherId(1L)
                .subjectId(1L)
                .dueDate(LocalDate.now().minusDays(1))
                .build();

        when(homeworkRepository.findById(1L))
                .thenReturn(Optional.of(homework));

        when(teacherRepository.findById(1L))
                .thenReturn(Optional.of(teacher));

        assertThrows(
                IllegalArgumentException.class,
                () -> homeworkService.updateHomework(1L, request)
        );
    }

    @Test
    void testDeleteHomework_Success() {

        when(homeworkRepository.findById(1L))
                .thenReturn(Optional.of(homework));

        homeworkService.deleteHomework(1L);

        verify(homeworkRepository)
                .delete(homework);
    }

    @Test
    void testDeleteHomework_NotFound() {

        when(homeworkRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> homeworkService.deleteHomework(1L)
        );
    }

    @Test
    void testCreateHomework_WithoutSection_Success() {

        HomeworkRequestDto request = HomeworkRequestDto.builder()
                .classId(1L)
                .teacherId(1L)
                .subjectId(1L)
                .title("Algebra Exercises")
                .dueDate(LocalDate.now().plusDays(7))
                .build();

        when(classRepository.findById(1L))
                .thenReturn(Optional.of(classEntity));

        when(teacherRepository.findById(1L))
                .thenReturn(Optional.of(teacher));

        when(subjectRepository.findById(1L))
                .thenReturn(Optional.of(subject));

        when(classSubjectRepository.existsByClassEntityIdAndSubjectId(
                1L,
                1L
        )).thenReturn(true);

        when(teacherAccessService.canTeacherAccessClassAndSubject(
                1L,
                1L,
                1L
        )).thenReturn(true);

        when(homeworkRepository.save(any(Homework.class)))
                .thenReturn(homework);

        HomeworkResponseDto result =
                homeworkService.createHomework(request);

        assertNotNull(result);
        assertEquals(
                "Algebra Exercises",
                result.getTitle()
        );

        assertEquals(
                1L,
                result.getSubjectId()
        );

        verify(homeworkRepository)
                .save(any(Homework.class));
    }
}

