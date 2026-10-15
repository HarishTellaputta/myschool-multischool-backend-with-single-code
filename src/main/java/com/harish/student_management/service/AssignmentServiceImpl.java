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
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AssignmentServiceImpl implements AssignmentService {

    private static final Logger logger =
            LoggerFactory.getLogger(AssignmentServiceImpl.class);

    private final AssignmentRepository assignmentRepository;
    private final ClassRepository classRepository;
    private final SectionRepository sectionRepository;
    private final TeacherRepository teacherRepository;
    private final SubjectRepository subjectRepository;
    private final ClassSubjectRepository classSubjectRepository;
    private final TeacherAccessService teacherAccessService;

    // ============================================================
    // CREATE ASSIGNMENT
    // ============================================================

    @Override
    public AssignmentResponseDto createAssignment(
            AssignmentRequestDto request) {

        logger.info(
                "Creating assignment for class: {}, teacher: {}, subject: {}",
                request.getClassId(),
                request.getTeacherId(),
                request.getSubjectId()
        );

        ClassEntity classEntity =
                classRepository.findById(request.getClassId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Class not found with id: "
                                                + request.getClassId()
                                )
                        );

        Teacher teacher =
                teacherRepository.findById(request.getTeacherId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Teacher not found with id: "
                                                + request.getTeacherId()
                                )
                        );

        /*
         * subjectId -> Subject entity
         *
         * If subjectId is null, subject will remain null.
         */
        Subject subject = getAndValidateSubject(
                request.getSubjectId(),
                classEntity
        );

        validateTeacherAccess(request);

        validateDates(request);

        Section section = null;

        if (request.getSectionId() != null) {

            section =
                    sectionRepository.findById(
                                    request.getSectionId()
                            )
                            .orElseThrow(() ->
                                    new ResourceNotFoundException(
                                            "Section not found with id: "
                                                    + request.getSectionId()
                                    )
                            );

            validateSectionBelongsToClass(
                    section,
                    classEntity
            );
        }

        Assignment assignment = Assignment.builder()
                .classEntity(classEntity)
                .section(section)
                .assignedByTeacher(teacher)

                // IMPORTANT:
                // Request has subjectId,
                // Entity has Subject subject.
                .subject(subject)

                .title(request.getTitle())
                .description(request.getDescription())
                .assignedDate(
                        request.getAssignedDate() != null
                                ? request.getAssignedDate()
                                : LocalDate.now()
                )
                .dueDate(request.getDueDate())
                .status(
                        request.getStatus() != null
                                ? request.getStatus()
                                : "ASSIGNED"
                )
                .attachmentUrl(request.getAttachmentUrl())
                .priority(
                        request.getPriority() != null
                                ? request.getPriority()
                                : "MEDIUM"
                )
                .submissionType(request.getSubmissionType())
                .maxMarks(request.getMaxMarks())
                .build();

        Assignment savedAssignment =
                assignmentRepository.save(assignment);

        logger.info(
                "Assignment created successfully with ID: {}",
                savedAssignment.getId()
        );

        return mapToResponseDto(savedAssignment);
    }

    // ============================================================
    // GET BY ID
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public AssignmentResponseDto getAssignmentById(Long id) {

        logger.info(
                "Fetching assignment with ID: {}",
                id
        );

        Assignment assignment =
                assignmentRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Assignment not found with id: " + id
                                )
                        );

        return mapToResponseDto(assignment);
    }

    // ============================================================
    // GET ALL
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentResponseDto> getAllAssignments() {

        logger.info("Fetching all assignments");

        return assignmentRepository.findAll()
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    // ============================================================
    // GET BY CLASS
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentResponseDto> getAssignmentsByClassId(
            Long classId) {

        logger.info(
                "Fetching assignments for class ID: {}",
                classId
        );

        classRepository.findById(classId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Class not found with id: " + classId
                        )
                );

        return assignmentRepository
                .findByClassEntityId(classId)
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    // ============================================================
    // GET BY SECTION
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentResponseDto> getAssignmentsBySectionId(
            Long sectionId) {

        logger.info(
                "Fetching assignments for section ID: {}",
                sectionId
        );

        sectionRepository.findById(sectionId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Section not found with id: " + sectionId
                        )
                );

        return assignmentRepository
                .findBySectionId(sectionId)
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    // ============================================================
    // GET BY CLASS + SECTION
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentResponseDto>
    getAssignmentsByClassIdAndSectionId(
            Long classId,
            Long sectionId) {

        logger.info(
                "Fetching assignments for class ID: {} and section ID: {}",
                classId,
                sectionId
        );

        ClassEntity classEntity =
                classRepository.findById(classId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Class not found with id: " + classId
                                )
                        );

        Section section =
                sectionRepository.findById(sectionId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Section not found with id: " + sectionId
                                )
                        );

        validateSectionBelongsToClass(
                section,
                classEntity
        );

        return assignmentRepository
                .findByClassEntityIdAndSectionId(
                        classId,
                        sectionId
                )
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    // ============================================================
    // GET BY TEACHER
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentResponseDto> getAssignmentsByTeacherId(
            Long teacherId) {

        logger.info(
                "Fetching assignments for teacher ID: {}",
                teacherId
        );

        teacherRepository.findById(teacherId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Teacher not found with id: " + teacherId
                        )
                );

        return assignmentRepository
                .findByAssignedByTeacherId(teacherId)
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    // ============================================================
    // GET BY SUBJECT ID
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentResponseDto> getAssignmentsBySubject(
            Long subjectId) {

        logger.info(
                "Fetching assignments for subject ID: {}",
                subjectId
        );

        subjectRepository.findById(subjectId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Subject not found with id: " + subjectId
                        )
                );

        return assignmentRepository
                .findBySubjectId(subjectId)
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    // ============================================================
    // GET BY CLASS + DUE DATE RANGE
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentResponseDto>
    getAssignmentsByClassIdAndDueDateRange(
            Long classId,
            LocalDate startDate,
            LocalDate endDate) {

        logger.info(
                "Fetching assignments for class ID: {} between {} and {}",
                classId,
                startDate,
                endDate
        );

        classRepository.findById(classId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Class not found with id: " + classId
                        )
                );

        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException(
                    "End date must be after start date"
            );
        }

        return assignmentRepository
                .findByClassIdAndDueDateRange(
                        classId,
                        startDate,
                        endDate
                )
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    // ============================================================
    // GET ACTIVE ASSIGNMENTS
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentResponseDto>
    getActiveAssignmentsByClassId(Long classId) {

        logger.info(
                "Fetching active assignments for class ID: {}",
                classId
        );

        classRepository.findById(classId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Class not found with id: " + classId
                        )
                );

        return assignmentRepository
                .findByClassEntityIdAndDueDateAfter(
                        classId,
                        LocalDate.now()
                )
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    // ============================================================
    // GET BY CLASS + SUBJECT ID
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentResponseDto>
    getAssignmentsByClassIdAndSubject(
            Long classId,
            Long subjectId) {

        logger.info(
                "Fetching assignments for class ID: {} and subject ID: {}",
                classId,
                subjectId
        );

        ClassEntity classEntity =
                classRepository.findById(classId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Class not found with id: " + classId
                                )
                        );

        getAndValidateSubject(
                subjectId,
                classEntity
        );

        return assignmentRepository
                .findByClassEntityIdAndSubjectIdOrderByDueDate(
                        classId,
                        subjectId
                )
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    // ============================================================
    // GET BY STATUS
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentResponseDto> getAssignmentsByStatus(
            String status) {

        logger.info(
                "Fetching assignments with status: {}",
                status
        );

        return assignmentRepository
                .findByStatus(status)
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    // ============================================================
    // UPDATE
    // ============================================================

    @Override
    public AssignmentResponseDto updateAssignment(
            Long id,
            AssignmentRequestDto request) {

        logger.info(
                "Updating assignment with ID: {}",
                id
        );

        Assignment assignment =
                assignmentRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Assignment not found with id: " + id
                                )
                        );

        Teacher teacher =
                teacherRepository.findById(request.getTeacherId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Teacher not found with id: "
                                                + request.getTeacherId()
                                )
                        );

        ClassEntity classEntity =
                classRepository.findById(request.getClassId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Class not found with id: "
                                                + request.getClassId()
                                )
                        );

        /*
         * subjectId from request -> Subject entity
         */
        Subject subject = getAndValidateSubject(
                request.getSubjectId(),
                classEntity
        );

        validateTeacherAccess(request);

        validateDates(request);

        Section section = null;

        if (request.getSectionId() != null) {

            section =
                    sectionRepository.findById(
                                    request.getSectionId()
                            )
                            .orElseThrow(() ->
                                    new ResourceNotFoundException(
                                            "Section not found with id: "
                                                    + request.getSectionId()
                                    )
                            );

            validateSectionBelongsToClass(
                    section,
                    classEntity
            );
        }

        assignment.setClassEntity(classEntity);
        assignment.setSection(section);
        assignment.setAssignedByTeacher(teacher);

        // subjectId -> Subject
        assignment.setSubject(subject);

        assignment.setTitle(request.getTitle());
        assignment.setDescription(request.getDescription());

        if (request.getAssignedDate() != null) {
            assignment.setAssignedDate(
                    request.getAssignedDate()
            );
        }

        assignment.setDueDate(request.getDueDate());

        if (request.getStatus() != null) {
            assignment.setStatus(
                    request.getStatus()
            );
        }

        assignment.setAttachmentUrl(
                request.getAttachmentUrl()
        );

        if (request.getPriority() != null) {
            assignment.setPriority(
                    request.getPriority()
            );
        }

        assignment.setSubmissionType(
                request.getSubmissionType()
        );

        assignment.setMaxMarks(
                request.getMaxMarks()
        );

        Assignment updatedAssignment =
                assignmentRepository.save(assignment);

        logger.info(
                "Assignment updated successfully with ID: {}",
                updatedAssignment.getId()
        );

        return mapToResponseDto(updatedAssignment);
    }

    // ============================================================
    // DELETE
    // ============================================================

    @Override
    public void deleteAssignment(Long id) {

        logger.info(
                "Deleting assignment with ID: {}",
                id
        );

        Assignment assignment =
                assignmentRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Assignment not found with id: " + id
                                )
                        );

        assignmentRepository.delete(assignment);

        logger.info(
                "Assignment deleted successfully"
        );
    }

    // ============================================================
    // SUBJECT VALIDATION
    // ============================================================

    private Subject getAndValidateSubject(
            Long subjectId,
            ClassEntity classEntity) {

        /*
         * Subject is optional.
         */
        if (subjectId == null) {
            return null;
        }

        Subject subject =
                subjectRepository.findById(subjectId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Subject not found with id: "
                                                + subjectId
                                )
                        );

        /*
         * Make sure this subject is assigned to this class.
         */
        boolean assignedToClass =
                classSubjectRepository
                        .existsByClassEntityIdAndSubjectId(
                                classEntity.getId(),
                                subjectId
                        );

        if (!assignedToClass) {
            throw new IllegalArgumentException(
                    "Subject is not assigned to this class"
            );
        }

        /*
         * Make sure Subject and Class belong to same school.
         */
        if (subject.getSchool() == null
                || classEntity.getSchool() == null
                || !subject.getSchool()
                .getId()
                .equals(classEntity.getSchool().getId())) {

            throw new IllegalArgumentException(
                    "Subject does not belong to the same school as the class"
            );
        }

        return subject;
    }

    // ============================================================
    // TEACHER ACCESS
    // ============================================================

    private void validateTeacherAccess(
            AssignmentRequestDto request) {

        /*
         * No subject selected:
         * teacher only needs class access.
         */
        if (request.getSubjectId() == null) {

            teacherAccessService
                    .validateTeacherAccessToClass(
                            request.getTeacherId(),
                            request.getClassId()
                    );

        } else {

            /*
             * Subject selected:
             * teacher must have class + subject assignment.
             */
            teacherAccessService
                    .validateTeacherAccessToClassAndSubject(
                            request.getTeacherId(),
                            request.getClassId(),
                            request.getSubjectId()
                    );
        }
    }

    // ============================================================
    // DATE VALIDATION
    // ============================================================

    private void validateDates(
            AssignmentRequestDto request) {

        if (request.getDueDate() == null) {
            throw new IllegalArgumentException(
                    "Due date is required"
            );
        }

        if (request.getDueDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "Due date cannot be in the past"
            );
        }

        if (request.getAssignedDate() != null
                && request.getAssignedDate()
                .isAfter(request.getDueDate())) {

            throw new IllegalArgumentException(
                    "Assigned date cannot be after due date"
            );
        }
    }

    // ============================================================
    // SECTION VALIDATION
    // ============================================================

    private void validateSectionBelongsToClass(
            Section section,
            ClassEntity classEntity) {

        if (section.getClassEntity() == null
                || !classEntity.getId()
                .equals(
                        section.getClassEntity().getId()
                )) {

            throw new IllegalArgumentException(
                    "Section does not belong to class"
            );
        }
    }

    // ============================================================
    // RESPONSE MAPPING
    // ============================================================

    private AssignmentResponseDto mapToResponseDto(
            Assignment assignment) {

        Subject subject = assignment.getSubject();

        return AssignmentResponseDto.builder()

                .id(assignment.getId())

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

                .assignedByTeacherId(
                        assignment.getAssignedByTeacher() != null
                                ? assignment.getAssignedByTeacher().getId()
                                : null
                )

                .assignedByTeacherName(
                        assignment.getAssignedByTeacher() != null
                                ? assignment.getAssignedByTeacher().getName()
                                : null
                )

                .teacherEmployeeId(
                        assignment.getAssignedByTeacher() != null
                                ? assignment.getAssignedByTeacher().getEmployeeId()
                                : null
                )

                // Subject information
                .subjectId(
                        subject != null
                                ? subject.getId()
                                : null
                )

                .subjectName(
                        subject != null
                                ? subject.getName()
                                : null
                )

                .subjectCode(
                        subject != null
                                ? subject.getCode()
                                : null
                )

                .title(assignment.getTitle())
                .description(assignment.getDescription())
                .assignedDate(assignment.getAssignedDate())
                .dueDate(assignment.getDueDate())
                .status(assignment.getStatus())
                .attachmentUrl(assignment.getAttachmentUrl())
                .priority(assignment.getPriority())
                .submissionType(assignment.getSubmissionType())
                .maxMarks(assignment.getMaxMarks())
                .createdAt(assignment.getCreatedAt())
                .updatedAt(assignment.getUpdatedAt())

                .build();
    }
}