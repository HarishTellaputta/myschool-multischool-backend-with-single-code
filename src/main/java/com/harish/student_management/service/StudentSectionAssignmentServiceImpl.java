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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentSectionAssignmentServiceImpl
        implements StudentSectionAssignmentService {

    private final StudentSectionAssignmentRepository assignmentRepository;
    private final StudentRepository studentRepository;
    private final SectionRepository sectionRepository;
    private final AcademicYearRepository academicYearRepository;
    private final CurrentSchoolService currentSchoolService;

    private Long currentSchoolId() {
        return currentSchoolService.getCurrentSchoolId();
    }

    @Override
    @Transactional
    public StudentSectionAssignmentResponseDto assignStudentToSection(
            StudentSectionAssignmentRequestDto request) {

        Long schoolId = currentSchoolId();

        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Student not found: " + request.getStudentId()));

        Section section = sectionRepository.findById(request.getSectionId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Section not found: " + request.getSectionId()));

        AcademicYear academicYear =
                academicYearRepository.findByIdAndSchool_Id(
                                request.getAcademicYearId(), schoolId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Academic year not found: "
                                        + request.getAcademicYearId()));

        // Validate that the student belongs to the authenticated school.
        if (student.getSection() == null
                || student.getSection().getClassEntity() == null
                || student.getSection().getClassEntity().getSchool() == null
                || !schoolId.equals(
                student.getSection().getClassEntity()
                        .getSchool().getId())) {
            throw new ResourceNotFoundException(
                    "Student not found in the current school");
        }

        // Validate that the requested section belongs to the same school.
        if (section.getClassEntity() == null
                || section.getClassEntity().getSchool() == null
                || !schoolId.equals(
                section.getClassEntity().getSchool().getId())) {
            throw new ResourceNotFoundException(
                    "Section not found in the current school");
        }

        // One assignment per student per academic year.
        if (assignmentRepository
                .findByStudentIdAndAcademicYearIdAndSchoolId(
                        student.getId(), academicYear.getId(), schoolId)
                .isPresent()) {
            throw new IllegalArgumentException(
                    "Student is already assigned for this academic year");
        }

        StudentSectionAssignment assignment =
                StudentSectionAssignment.builder()
                        .student(student)
                        .section(section)
                        .academicYear(academicYear)
                        .build();

        return mapToResponseDto(assignmentRepository.save(assignment));
    }

    @Override
    public StudentSectionAssignmentResponseDto getAssignmentById(Long id) {
        StudentSectionAssignment assignment =
                assignmentRepository.findByIdAndSchoolId(id, currentSchoolId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Assignment not found: " + id));

        return mapToResponseDto(assignment);
    }

    @Override
    public List<StudentSectionAssignmentResponseDto> getAssignmentsByStudentId(
            Long studentId) {

        Long schoolId = currentSchoolId();

        // Confirm the requested student belongs to this school.
        studentRepository.findByIdAndSection_ClassEntity_School_Id(
                        studentId, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Student not found in the current school"));

        return assignmentRepository
                .findByStudentIdAndSchoolId(studentId, schoolId)
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    public List<StudentSectionAssignmentResponseDto> getAssignmentsBySectionId(
            Long sectionId) {

        Long schoolId = currentSchoolId();

        // Verify section ownership before returning records.
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Section not found: " + sectionId));

        if (section.getClassEntity() == null
                || section.getClassEntity().getSchool() == null
                || !schoolId.equals(
                section.getClassEntity().getSchool().getId())) {
            throw new ResourceNotFoundException(
                    "Section not found in the current school");
        }

        return assignmentRepository
                .findBySectionIdAndSchoolId(sectionId, schoolId)
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    public List<StudentSectionAssignmentResponseDto>
    getAssignmentsByAcademicYearId(Long academicYearId) {

        Long schoolId = currentSchoolId();

        academicYearRepository.findByIdAndSchool_Id(academicYearId, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Academic year not found: " + academicYearId));

        return assignmentRepository
                .findByAcademicYearIdAndSchoolId(academicYearId, schoolId)
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    public List<StudentSectionAssignmentResponseDto>
    getAssignmentsBySectionAndYear(Long sectionId, Long academicYearId) {

        Long schoolId = currentSchoolId();

        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Section not found: " + sectionId));

        if (section.getClassEntity() == null
                || section.getClassEntity().getSchool() == null
                || !schoolId.equals(
                section.getClassEntity().getSchool().getId())) {
            throw new ResourceNotFoundException(
                    "Section not found in the current school");
        }

        academicYearRepository.findByIdAndSchool_Id(academicYearId, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Academic year not found: " + academicYearId));

        return assignmentRepository
                .findBySectionIdAndAcademicYearIdAndSchoolId(
                        sectionId, academicYearId, schoolId)
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    @Transactional
    public void removeAssignment(Long id) {
        StudentSectionAssignment assignment =
                assignmentRepository.findByIdAndSchoolId(id, currentSchoolId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Assignment not found: " + id));

        assignmentRepository.delete(assignment);
    }

    private StudentSectionAssignmentResponseDto mapToResponseDto(
            StudentSectionAssignment assignment) {

        Student student = assignment.getStudent();
        Section section = assignment.getSection();
        AcademicYear academicYear = assignment.getAcademicYear();

        return StudentSectionAssignmentResponseDto.builder()
                .id(assignment.getId())
                .studentId(student != null ? student.getId() : null)
                .studentName(student != null ? student.getName() : null)
                .admissionNo(student != null ? student.getAdmissionNo() : null)
                .sectionId(section != null ? section.getId() : null)
                .sectionName(section != null ? section.getName() : null)
                .classId(section != null && section.getClassEntity() != null
                        ? section.getClassEntity().getId() : null)
                .className(section != null && section.getClassEntity() != null
                        ? section.getClassEntity().getName() : null)
                .academicYearId(academicYear != null
                        ? academicYear.getId() : null)
                .academicYearName(academicYear != null
                        ? academicYear.getName() : null)
                .assignedAt(assignment.getAssignedAt())
                .build();
    }
}