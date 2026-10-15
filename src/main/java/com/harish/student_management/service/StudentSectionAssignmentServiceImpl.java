package com.harish.student_management.service;

import com.harish.student_management.dto.StudentSectionAssignmentRequestDto;
import com.harish.student_management.dto.StudentSectionAssignmentResponseDto;
import com.harish.student_management.entity.AcademicYear;
import com.harish.student_management.entity.Section;
import com.harish.student_management.entity.Student;
import com.harish.student_management.entity.StudentSectionAssignment;
import com.harish.student_management.exception.ResourceNotFoundException;
import com.harish.student_management.repository.AcademicYearRepository;
import com.harish.student_management.repository.SectionRepository;
import com.harish.student_management.repository.StudentRepository;
import com.harish.student_management.repository.StudentSectionAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StudentSectionAssignmentServiceImpl implements StudentSectionAssignmentService {

    private static final Logger logger = LoggerFactory.getLogger(StudentSectionAssignmentServiceImpl.class);

    private final StudentSectionAssignmentRepository assignmentRepository;
    private final StudentRepository studentRepository;
    private final SectionRepository sectionRepository;
    private final AcademicYearRepository academicYearRepository;

    @Override
    public StudentSectionAssignmentResponseDto assignStudentToSection(
            StudentSectionAssignmentRequestDto request) {

        logger.info(
                "Assigning student {} to section {} for academic year: {}",
                request.getStudentId(),
                request.getSectionId(),
                request.getAcademicYearId()
        );

        // =========================================================
        // GET STUDENT
        // =========================================================

        Student student = studentRepository.findById(
                        request.getStudentId()
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Student not found with id: "
                                        + request.getStudentId()
                        )
                );


        // =========================================================
        // GET SECTION
        // =========================================================

        Section section = sectionRepository.findById(
                        request.getSectionId()
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Section not found with id: "
                                        + request.getSectionId()
                        )
                );


        // =========================================================
        // GET ACADEMIC YEAR
        // =========================================================

        AcademicYear academicYear = academicYearRepository.findById(
                        request.getAcademicYearId()
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Academic year not found with id: "
                                        + request.getAcademicYearId()
                        )
                );


        // =========================================================
        // CHECK: STUDENT ALREADY ASSIGNED TO A SECTION
        // FOR THIS ACADEMIC YEAR
        // =========================================================

        Optional<StudentSectionAssignment> existingAssignment =
                assignmentRepository.findByStudentIdAndAcademicYearId(
                        request.getStudentId(),
                        request.getAcademicYearId()
                );

        if (existingAssignment.isPresent()) {

            throw new IllegalArgumentException(
                    "Student is already assigned to a section for this academic year"
            );
        }


        // =========================================================
        // CREATE ASSIGNMENT
        // =========================================================

        StudentSectionAssignment assignment =
                StudentSectionAssignment.builder()
                        .student(student)
                        .section(section)
                        .academicYear(academicYear)
                        .build();


        StudentSectionAssignment savedAssignment =
                assignmentRepository.save(assignment);


        logger.info(
                "Student {} successfully assigned to section {} for academic year {}. Assignment ID: {}",
                request.getStudentId(),
                request.getSectionId(),
                request.getAcademicYearId(),
                savedAssignment.getId()
        );


        return mapToResponseDto(savedAssignment);
    }
    @Override
    public StudentSectionAssignmentResponseDto getAssignmentById(Long id) {
        logger.info("Fetching assignment with ID: {}", id);
        StudentSectionAssignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found with id: " + id));
        return mapToResponseDto(assignment);
    }

    @Override
    public List<StudentSectionAssignmentResponseDto> getAssignmentsByStudentId(Long studentId) {
        logger.info("Fetching assignments for student ID: {}", studentId);
        return assignmentRepository.findByStudentId(studentId).stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    public List<StudentSectionAssignmentResponseDto> getAssignmentsBySectionId(Long sectionId) {
        logger.info("Fetching assignments for section ID: {}", sectionId);
        return assignmentRepository.findBySectionId(sectionId).stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    public List<StudentSectionAssignmentResponseDto> getAssignmentsByAcademicYearId(Long academicYearId) {
        logger.info("Fetching assignments for academic year ID: {}", academicYearId);
        return assignmentRepository.findByAcademicYearId(academicYearId).stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    public List<StudentSectionAssignmentResponseDto> getAssignmentsBySectionAndYear(Long sectionId, Long academicYearId) {
        logger.info("Fetching assignments for section ID: {} and academic year ID: {}", sectionId, academicYearId);
        return assignmentRepository.findBySectionIdAndAcademicYearId(sectionId, academicYearId).stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    public void removeAssignment(Long id) {
        logger.info("Removing assignment with ID: {}", id);
        StudentSectionAssignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found with id: " + id));
        assignmentRepository.delete(assignment);
        logger.info("Assignment removed successfully");
    }

    private StudentSectionAssignmentResponseDto mapToResponseDto(StudentSectionAssignment assignment) {
        return StudentSectionAssignmentResponseDto.builder()
                .id(assignment.getId())
                .studentId(assignment.getStudent() != null ? assignment.getStudent().getId() : null)
                .studentName(assignment.getStudent() != null ? assignment.getStudent().getName() : null)
                .admissionNo(assignment.getStudent() != null ? assignment.getStudent().getAdmissionNo() : null)
                .sectionId(assignment.getSection() != null ? assignment.getSection().getId() : null)
                .sectionName(assignment.getSection() != null ? assignment.getSection().getName() : null)
                .classId(assignment.getSection() != null && assignment.getSection().getClassEntity() != null 
                        ? assignment.getSection().getClassEntity().getId() : null)
                .className(assignment.getSection() != null && assignment.getSection().getClassEntity() != null 
                        ? assignment.getSection().getClassEntity().getName() : null)
                .academicYearId(assignment.getAcademicYear() != null ? assignment.getAcademicYear().getId() : null)
                .academicYearName(assignment.getAcademicYear() != null ? assignment.getAcademicYear().getName() : null)
                .assignedAt(assignment.getAssignedAt())
                .build();
    }
}
