package com.harish.student_management.service;

import com.harish.student_management.dto.StudentRequestDto;
import com.harish.student_management.dto.StudentResponseDto;
import com.harish.student_management.entity.Student;
import com.harish.student_management.mapper.StudentMapper;
import com.harish.student_management.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class StudentServiceImplTest {

    @Mock
    private StudentRepository repository;

    @Mock
    private StudentMapper mapper;

    @InjectMocks
    private StudentServiceImpl service;



    @Test
    void testGetStudentById() {

        // Arrange
        Student student = new Student(1L, "Harish", "harish@gmail.com",
                "Java", "9876543210");

        StudentResponseDto responseDto = new StudentResponseDto(
                1L,
                "Harish",
                "harish@gmail.com",
                "Java",
                "9876543210"
        );

        when(repository.findById(1L))
                .thenReturn(Optional.of(student));

        when(mapper.toResponseDto(student))
                .thenReturn(responseDto);

        // Act
        StudentResponseDto result = service.getStudentById(1L);

        // Assert
        assertEquals("Harish", result.getName());
        assertEquals("harish@gmail.com", result.getEmail());
    }
    @Test
    void testSaveStudent() {

        StudentRequestDto requestDto = new StudentRequestDto(
                "Harish",
                "harish@gmail.com",
                "Java",
                "9876543210"
        );

        Student student = new Student(1L, "Harish", "harish@gmail.com",
                "Java", "9876543210");

        StudentResponseDto responseDto = new StudentResponseDto(
                1L,
                "Harish",
                "harish@gmail.com",
                "Java",
                "9876543210"
        );
        when(mapper.toEntity(requestDto)).thenReturn(student);
        when(repository.save(student)).thenReturn(student);
        when(mapper.toResponseDto(student)).thenReturn(responseDto);

        StudentResponseDto result = service.saveStudent(requestDto);

        assertEquals("Harish", result.getName());
        assertEquals("harish@gmail.com", result.getEmail());
    }

    @Test
    void testUpdateStudent() {
        StudentRequestDto requestDto = new StudentRequestDto(
                "Harish",
                "harish@gmail.com",
                "Java",
                "9876543210"
        );

        Student student = new Student(1L, "Harish", "harish@gmail.com",
                "Java", "9876543210");

        StudentResponseDto responseDto = new StudentResponseDto(
                3L,
                "Harish",
                "harish@gmail.com",
                "Java",
                "9876543210"
        );
        when(repository.findById(3L)).thenReturn(Optional.of(student));
        when(repository.save(student)).thenReturn(student);
        when(mapper.toResponseDto(student)).thenReturn(responseDto);

        StudentResponseDto result = service.updateStudent(3L,requestDto);

        assertEquals("Harish", result.getName());
        assertEquals("harish@gmail.com", result.getEmail());
    }
    @Test
    void testDeleteStudent() {
        Student student = new Student(1L, "Harish", "harish@gmail.com",
                "Java", "9876543210");

        StudentResponseDto responseDto = new StudentResponseDto(
                3L,
                "Harish",
                "harish@gmail.com",
                "Java",
                "9876543210"
        );
        when(repository.findById(3L)).thenReturn(Optional.of(student));

        service.deleteStudent(3L);
        verify(repository).delete(student);

    }
    @Test
    void testGetAllStudents() {

        // Arrange
        Student student = new Student(
                1L,
                "Harish",
                "harish@gmail.com",
                "Java",
                "9876543210"
        );

        StudentResponseDto responseDto = new StudentResponseDto(
                1L,
                "Harish",
                "harish@gmail.com",
                "Java",
                "9876543210"
        );

        Pageable pageable = PageRequest.of(0, 5, Sort.by("name").ascending());

        Page<Student> studentPage =
                new PageImpl<>(List.of(student));

        when(repository.findAll(pageable))
                .thenReturn(studentPage);

        when(mapper.toResponseDto(student))
                .thenReturn(responseDto);

        // Act
        Page<StudentResponseDto> result =
                service.getAllStudents(0, 5, "name", "asc");

        // Assert
        assertEquals(1, result.getTotalElements());
        assertEquals("Harish", result.getContent().get(0).getName());
        assertEquals("harish@gmail.com", result.getContent().get(0).getEmail());
    }
}
