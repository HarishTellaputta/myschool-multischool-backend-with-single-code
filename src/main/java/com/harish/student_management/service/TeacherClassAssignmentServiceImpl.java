package com.harish.student_management.service;

import com.harish.student_management.dto.TeacherClassAssignmentRequestDto;
import com.harish.student_management.dto.TeacherClassAssignmentResponseDto;
import com.harish.student_management.entity.ClassEntity;
import com.harish.student_management.entity.Section;
import com.harish.student_management.entity.Subject;
import com.harish.student_management.entity.Teacher;
import com.harish.student_management.entity.TeacherClassAssignment;
import com.harish.student_management.repository.ClassRepository;
import com.harish.student_management.repository.ClassSubjectRepository;
import com.harish.student_management.repository.SectionRepository;
import com.harish.student_management.repository.SubjectRepository;
import com.harish.student_management.repository.TeacherClassAssignmentRepository;
import com.harish.student_management.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TeacherClassAssignmentServiceImpl
        implements TeacherClassAssignmentService {

    private final TeacherClassAssignmentRepository assignmentRepository;
    private final TeacherRepository teacherRepository;
    private final ClassRepository classRepository;
    private final SubjectRepository subjectRepository;
    private final ClassSubjectRepository classSubjectRepository;
    private final SectionRepository sectionRepository;
    private final CurrentSchoolService currentSchoolService;

    // ------------------------------------------------------------
    // ASSIGN TEACHER
    // ------------------------------------------------------------

    @Override
    public TeacherClassAssignmentResponseDto assignTeacherToClass(
            TeacherClassAssignmentRequestDto request) {

        Long schoolId = currentSchoolService.getCurrentSchoolId();

        Teacher teacher = teacherRepository.findById(request.getTeacherId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Teacher not found"
                ));

        ClassEntity classEntity = classRepository.findById(request.getClassId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Class not found"
                ));

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Subject not found"
                ));

        // All three records must belong to the authenticated school.
        if (teacher.getSchool() == null
                || teacher.getSchool().getId() == null
                || !schoolId.equals(teacher.getSchool().getId())) {
            throw new IllegalArgumentException(
                    "Teacher does not belong to your school"
            );
        }

        if (classEntity.getSchool() == null
                || classEntity.getSchool().getId() == null
                || !schoolId.equals(classEntity.getSchool().getId())) {
            throw new IllegalArgumentException(
                    "Class does not belong to your school"
            );
        }

        if (subject.getSchool() == null
                || subject.getSchool().getId() == null
                || !schoolId.equals(subject.getSchool().getId())) {
            throw new IllegalArgumentException(
                    "Subject does not belong to your school"
            );
        }

        // Validate that the subject is assigned to this class.
        if (!classSubjectRepository.existsByClassEntityIdAndSubjectId(
                classEntity.getId(),
                subject.getId())) {
            throw new IllegalArgumentException(
                    "Subject is not assigned to this class"
            );
        }

        Section section = null;

        if (request.getSectionId() != null) {
            section = sectionRepository.findByIdAndClassEntityId(
                            request.getSectionId(),
                            classEntity.getId()
                    )
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Section not found for this class"
                    ));

            if (section.getClassEntity() == null
                    || section.getClassEntity().getSchool() == null
                    || section.getClassEntity().getSchool().getId() == null
                    || !schoolId.equals(
                    section.getClassEntity().getSchool().getId())) {
                throw new IllegalArgumentException(
                        "Section does not belong to your school"
                );
            }

            // Preserve the existing rule:
            // one teacher per class + section + subject.
            boolean sectionAssignmentExists =
                    assignmentRepository
                            .existsByClassEntityIdAndSectionIdAndSubjectId(
                                    classEntity.getId(),
                                    section.getId(),
                                    subject.getId()
                            );

            if (sectionAssignmentExists) {
                throw new IllegalArgumentException(
                        "Teacher already assigned for this class, section and subject"
                );
            }

        } else {
            // Preserve the existing sectionless duplicate rule.
            boolean assignmentExists =
                    assignmentRepository
                            .existsByTeacherIdAndClassEntityIdAndSubjectId(
                                    teacher.getId(),
                                    classEntity.getId(),
                                    subject.getId()
                            );

            if (assignmentExists) {
                throw new IllegalArgumentException(
                        "Teacher is already assigned to this class and subject"
                );
            }
        }

        TeacherClassAssignment assignment =
                TeacherClassAssignment.builder()
                        .teacher(teacher)
                        .classEntity(classEntity)
                        .subject(subject)
                        .section(section)
                        .build();

        return mapToResponse(assignmentRepository.save(assignment));
    }

    // ------------------------------------------------------------
    // GET ASSIGNMENT BY ID
    // ------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public TeacherClassAssignmentResponseDto getAssignmentById(Long id) {

        Long schoolId = currentSchoolService.getCurrentSchoolId();

        TeacherClassAssignment assignment =
                assignmentRepository.findByIdAndSchoolId(id, schoolId)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Teacher class assignment not found"
                        ));

        return mapToResponse(assignment);
    }

    // ------------------------------------------------------------
    // GET ASSIGNMENTS BY TEACHER
    // ------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<TeacherClassAssignmentResponseDto> getAssignmentsByTeacherId(
            Long teacherId) {

        Long schoolId = currentSchoolService.getCurrentSchoolId();

        validateTeacherBelongsToSchool(teacherId, schoolId);

        return assignmentRepository
                .findByTeacherIdAndSchoolId(teacherId, schoolId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ------------------------------------------------------------
    // GET ASSIGNMENTS BY CLASS
    // ------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<TeacherClassAssignmentResponseDto> getAssignmentsByClassId(
            Long classId) {

        Long schoolId = currentSchoolService.getCurrentSchoolId();

        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Class not found"
                ));

        if (classEntity.getSchool() == null
                || classEntity.getSchool().getId() == null
                || !schoolId.equals(classEntity.getSchool().getId())) {
            throw new IllegalArgumentException(
                    "Class does not belong to your school"
            );
        }

        return assignmentRepository
                .findByClassIdAndSchoolId(classId, schoolId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ------------------------------------------------------------
    // GET ASSIGNMENTS BY TEACHER + SUBJECT
    // ------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<TeacherClassAssignmentResponseDto>
    getAssignmentsByTeacherIdAndSubject(Long teacherId, Long subjectId) {

        Long schoolId = currentSchoolService.getCurrentSchoolId();

        validateTeacherBelongsToSchool(teacherId, schoolId);

        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Subject not found"
                ));

        if (subject.getSchool() == null
                || subject.getSchool().getId() == null
                || !schoolId.equals(subject.getSchool().getId())) {
            throw new IllegalArgumentException(
                    "Subject does not belong to your school"
            );
        }

        return assignmentRepository
                .findByTeacherIdAndSubjectIdAndSchoolId(
                        teacherId,
                        subjectId,
                        schoolId
                )
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ------------------------------------------------------------
    // GET SUBJECTS BY TEACHER
    // ------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public Set<String> getSubjectsByTeacherId(Long teacherId) {

        Long schoolId = currentSchoolService.getCurrentSchoolId();

        validateTeacherBelongsToSchool(teacherId, schoolId);

        return assignmentRepository
                .findByTeacherIdAndSchoolId(teacherId, schoolId)
                .stream()
                .map(assignment -> assignment.getSubject().getName())
                .collect(Collectors.toSet());
    }

    // ------------------------------------------------------------
    // REMOVE ASSIGNMENT
    // ------------------------------------------------------------

    @Override
    public void removeAssignment(Long id) {

        Long schoolId = currentSchoolService.getCurrentSchoolId();

        TeacherClassAssignment assignment =
                assignmentRepository.findByIdAndSchoolId(id, schoolId)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Teacher class assignment not found"
                        ));

        // Delete only the assignment belonging to the current school.
        assignmentRepository.delete(assignment);
    }

    // ------------------------------------------------------------
    // VALIDATE TEACHER SCHOOL
    // ------------------------------------------------------------

    private void validateTeacherBelongsToSchool(
            Long teacherId,
            Long schoolId) {

        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Teacher not found"
                ));

        if (teacher.getSchool() == null
                || teacher.getSchool().getId() == null
                || !schoolId.equals(teacher.getSchool().getId())) {
            throw new IllegalArgumentException(
                    "Teacher does not belong to your school"
            );
        }
    }

    // ------------------------------------------------------------
    // MAP ENTITY TO RESPONSE DTO
    // ------------------------------------------------------------

    private TeacherClassAssignmentResponseDto mapToResponse(
            TeacherClassAssignment assignment) {

        return TeacherClassAssignmentResponseDto.builder()
                .id(assignment.getId())

                .teacherId(
                        assignment.getTeacher() != null
                                ? assignment.getTeacher().getId()
                                : null
                )
                .teacherName(
                        assignment.getTeacher() != null
                                ? assignment.getTeacher().getName()
                                : null
                )

                .classId(
                        assignment.getClassEntity() != null
                                ? assignment.getClassEntity().getId()
                                : null
                )
                .className(
                        assignment.getClassEntity() != null
                                ? assignment.getClassEntity().getName()
                                : null
                )

                .sectionId(
                        assignment.getSection() != null
                                ? assignment.getSection().getId()
                                : null
                )
                .sectionName(
                        assignment.getSection() != null
                                ? assignment.getSection().getName()
                                : null
                )

                .subjectId(
                        assignment.getSubject() != null
                                ? assignment.getSubject().getId()
                                : null
                )
                .subjectName(
                        assignment.getSubject() != null
                                ? assignment.getSubject().getName()
                                : null
                )
                .subjectCode(
                        assignment.getSubject() != null
                                ? assignment.getSubject().getCode()
                                : null
                )

                .assignedAt(assignment.getAssignedAt())
                .build();
    }
}