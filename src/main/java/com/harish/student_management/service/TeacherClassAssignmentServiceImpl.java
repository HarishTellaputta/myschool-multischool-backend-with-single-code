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
import com.harish.student_management.service.TeacherClassAssignmentService;
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


    // ============================================================
    // ASSIGN TEACHER
    // ============================================================

    @Override
    public TeacherClassAssignmentResponseDto assignTeacherToClass(
            TeacherClassAssignmentRequestDto request
    ) {

        Teacher teacher = teacherRepository.findById(request.getTeacherId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Teacher not found: " + request.getTeacherId()
                        )
                );

        ClassEntity classEntity = classRepository.findById(request.getClassId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Class not found: " + request.getClassId()
                        )
                );

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Subject not found: " + request.getSubjectId()
                        )
                );


        // ========================================================
        // SCHOOL VALIDATION
        // ========================================================

        if (classEntity.getSchool() == null ||
                subject.getSchool() == null ||
                !classEntity.getSchool().getId()
                        .equals(subject.getSchool().getId())) {

            throw new IllegalArgumentException(
                    "Class and Subject must belong to the same school"
            );
        }

        if (teacher.getSchool() == null ||
                !teacher.getSchool().getId()
                        .equals(classEntity.getSchool().getId())) {

            throw new IllegalArgumentException(
                    "Teacher does not belong to the same school as the class"
            );
        }


        // ========================================================
        // CLASS-SUBJECT VALIDATION
        // ========================================================

        boolean classSubjectExists =
                classSubjectRepository
                        .existsByClassEntityIdAndSubjectId(
                                request.getClassId(),
                                request.getSubjectId()
                        );

        if (!classSubjectExists) {

            throw new IllegalArgumentException(
                    "Subject is not assigned to this class"
            );
        }


        // ========================================================
        // SECTION
        // ========================================================

        Section section = null;

        if (request.getSectionId() != null) {

            section = sectionRepository
                    .findByIdAndClassEntityId(
                            request.getSectionId(),
                            request.getClassId()
                    )
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Section not found for this class: "
                                            + request.getSectionId()
                            )
                    );


            // ----------------------------------------------------
            // SECTION SCHOOL VALIDATION
            // ----------------------------------------------------

            if (section.getClassEntity() == null ||
                    section.getClassEntity().getSchool() == null ||
                    !section.getClassEntity()
                            .getSchool()
                            .getId()
                            .equals(classEntity.getSchool().getId())) {

                throw new IllegalArgumentException(
                        "Section does not belong to the same school as the class"
                );
            }


            // ====================================================
            // SECTION-WISE DUPLICATE CHECK
            // ====================================================
            //
            // One teacher only for:
            //
            // Class + Section + Subject
            //
            // Example:
            //
            // 1st + A + English -> Suresh
            //
            // Another teacher cannot be assigned to:
            //
            // 1st + A + English
            //
            // But the same teacher CAN teach:
            //
            // 1st + A + English
            // 1st + B + English
            // 1st + C + English
            //
            // ====================================================

            boolean sectionAssignmentExists =
                    assignmentRepository
                            .existsByClassEntityIdAndSectionIdAndSubjectId(
                                    request.getClassId(),
                                    request.getSectionId(),
                                    request.getSubjectId()
                            );

            if (sectionAssignmentExists) {

                throw new IllegalArgumentException(
                        "Teacher already assigned for this class, section and subject"
                );
            }

        } else {

            // ====================================================
            // OLD / SECTIONLESS DUPLICATE CHECK
            // ====================================================
            //
            // Keep existing behaviour for old records/API calls.
            //
            // Teacher + Class + Subject
            //
            // ====================================================

            boolean assignmentExists =
                    assignmentRepository
                            .existsByTeacherIdAndClassEntityIdAndSubjectId(
                                    request.getTeacherId(),
                                    request.getClassId(),
                                    request.getSubjectId()
                            );

            if (assignmentExists) {

                throw new IllegalArgumentException(
                        "Teacher is already assigned to this class and subject"
                );
            }
        }


        // ========================================================
        // CREATE ASSIGNMENT
        // ========================================================

        TeacherClassAssignment assignment =
                TeacherClassAssignment.builder()
                        .teacher(teacher)
                        .classEntity(classEntity)
                        .subject(subject)
                        .section(section)
                        .build();


        TeacherClassAssignment saved =
                assignmentRepository.save(assignment);

        return mapToResponse(saved);
    }


    // ============================================================
    // GET BY ID
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public TeacherClassAssignmentResponseDto getAssignmentById(
            Long id
    ) {

        TeacherClassAssignment assignment =
                assignmentRepository.findById(id)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Teacher class assignment not found: " + id
                                )
                        );

        return mapToResponse(assignment);
    }


    // ============================================================
    // GET BY TEACHER
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public List<TeacherClassAssignmentResponseDto> getAssignmentsByTeacherId(
            Long teacherId
    ) {

        return assignmentRepository
                .findByTeacherId(teacherId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }


    // ============================================================
    // GET BY CLASS
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public List<TeacherClassAssignmentResponseDto> getAssignmentsByClassId(
            Long classId
    ) {

        return assignmentRepository
                .findByClassEntityId(classId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }


    // ============================================================
    // GET BY TEACHER + SUBJECT
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public List<TeacherClassAssignmentResponseDto>
    getAssignmentsByTeacherIdAndSubject(
            Long teacherId,
            Long subjectId
    ) {

        return assignmentRepository
                .findByTeacherIdAndSubjectId(
                        teacherId,
                        subjectId
                )
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }


    // ============================================================
    // GET SUBJECTS BY TEACHER
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public Set<String> getSubjectsByTeacherId(
            Long teacherId
    ) {

        return assignmentRepository
                .findByTeacherId(teacherId)
                .stream()
                .map(assignment ->
                        assignment.getSubject().getName()
                )
                .collect(Collectors.toSet());
    }


    // ============================================================
    // REMOVE ASSIGNMENT
    // ============================================================

    @Override
    public void removeAssignment(Long id) {

        if (!assignmentRepository.existsById(id)) {

            throw new IllegalArgumentException(
                    "Teacher class assignment not found: " + id
            );
        }

        assignmentRepository.deleteById(id);
    }


    // ============================================================
    // TEACHER ACCESS SUBJECT
    // ============================================================

    private boolean canTeacherAccessSubject(
            Long teacherId,
            Long subjectId
    ) {

        return assignmentRepository
                .existsByTeacherIdAndSubjectId(
                        teacherId,
                        subjectId
                );
    }


    // ============================================================
    // VALIDATE ASSIGNMENT
    // ============================================================

    private boolean validateAssignment(
            Long teacherId,
            Long classId,
            Long subjectId
    ) {

        return assignmentRepository
                .existsByTeacherIdAndClassEntityIdAndSubjectId(
                        teacherId,
                        classId,
                        subjectId
                );
    }


    // ============================================================
    // MAP ENTITY -> RESPONSE DTO
    // ============================================================

    private TeacherClassAssignmentResponseDto mapToResponse(
            TeacherClassAssignment assignment
    ) {

        return TeacherClassAssignmentResponseDto.builder()

                .id(assignment.getId())

                // Teacher
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

                // Class
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

                // Section
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

                // Subject
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

                // Date
                .assignedAt(
                        assignment.getAssignedAt()
                )

                .build();
    }
}