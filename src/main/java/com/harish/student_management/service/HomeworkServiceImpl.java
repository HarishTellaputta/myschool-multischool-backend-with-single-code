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
public class HomeworkServiceImpl implements HomeworkService {

    private static final Logger logger =
            LoggerFactory.getLogger(HomeworkServiceImpl.class);

    private final HomeworkRepository homeworkRepository;
    private final ClassRepository classRepository;
    private final SectionRepository sectionRepository;
    private final TeacherRepository teacherRepository;
    private final TeacherAccessService teacherAccessService;
    private final SubjectRepository subjectRepository;
    private final ClassSubjectRepository classSubjectRepository;

    @Override
    public HomeworkResponseDto createHomework(HomeworkRequestDto request) {

        logger.info(
                "Creating homework for class: {} by teacher: {}",
                request.getClassId(),
                request.getTeacherId()
        );

        ClassEntity classEntity = classRepository.findById(request.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Class not found with id: " + request.getClassId()
                ));

        Teacher teacher = teacherRepository.findById(request.getTeacherId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Teacher not found with id: " + request.getTeacherId()
                ));

        validateTeacherAccess(request);

        validateDueDate(request.getDueDate());

        Section section = getSection(request.getSectionId());

        if (section != null) {
            validateSectionBelongsToClass(section, classEntity);
        }

        Subject subject = getAndValidateSubject(
                request.getSubjectId(),
                classEntity.getId()
        );

        Homework homework = Homework.builder()
                .classEntity(classEntity)
                .section(section)
                .assignedByTeacher(teacher)
                .subject(subject)
                .title(request.getTitle())
                .description(request.getDescription())
                .dueDate(request.getDueDate())
                .status(
                        request.getStatus() != null
                                ? request.getStatus()
                                : "ACTIVE"
                )
                .attachmentUrl(request.getAttachmentUrl())
                .priority(
                        request.getPriority() != null
                                ? request.getPriority()
                                : "MEDIUM"
                )
                .build();

        Homework savedHomework = homeworkRepository.save(homework);

        logger.info(
                "Homework created successfully with ID: {}",
                savedHomework.getId()
        );

        return mapToResponseDto(savedHomework);
    }

    @Override
    public HomeworkResponseDto getHomeworkById(Long id) {

        logger.info("Fetching homework with ID: {}", id);

        Homework homework = homeworkRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Homework not found with id: " + id
                ));

        return mapToResponseDto(homework);
    }

    @Override
    public List<HomeworkResponseDto> getAllHomework() {

        logger.info("Fetching all homework");

        return homeworkRepository.findAll()
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<HomeworkResponseDto> getHomeworkByClassId(Long classId) {

        logger.info("Fetching homework for class ID: {}", classId);

        classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Class not found with id: " + classId
                ));

        return homeworkRepository.findByClassEntityIdOrderByCreatedAtDescIdDesc(classId)
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<HomeworkResponseDto> getHomeworkBySectionId(Long sectionId) {

        logger.info("Fetching homework for section ID: {}", sectionId);

        sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Section not found with id: " + sectionId
                ));

        return homeworkRepository.findBySectionIdOrderByCreatedAtDescIdDesc(sectionId)
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<HomeworkResponseDto> getHomeworkByClassIdAndSectionId(
            Long classId,
            Long sectionId) {

        logger.info(
                "Fetching homework for class ID: {} and section ID: {}",
                classId,
                sectionId
        );

        classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Class not found with id: " + classId
                ));

        sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Section not found with id: " + sectionId
                ));

        return homeworkRepository
                .findByClassEntityIdAndSectionIdOrderByCreatedAtDescIdDesc(classId, sectionId)
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<HomeworkResponseDto> getHomeworkByTeacherId(Long teacherId) {

        logger.info("Fetching homework for teacher ID: {}", teacherId);

        teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Teacher not found with id: " + teacherId
                ));

        return homeworkRepository.findByAssignedByTeacherId(teacherId)
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<HomeworkResponseDto> getHomeworkBySubject(Long subjectId) {

        logger.info("Fetching homework for subject ID: {}", subjectId);

        subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Subject not found with id: " + subjectId
                ));

        return homeworkRepository.findBySubjectId(subjectId)
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<HomeworkResponseDto> getHomeworkByClassIdAndDueDateRange(
            Long classId,
            LocalDate startDate,
            LocalDate endDate) {

        logger.info(
                "Fetching homework for class ID: {} between {} and {}",
                classId,
                startDate,
                endDate
        );

        classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Class not found with id: " + classId
                ));

        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException(
                    "Start date and end date are required"
            );
        }

        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException(
                    "End date must be after start date"
            );
        }

        return homeworkRepository
                .findByClassIdAndDueDateRange(
                        classId,
                        startDate,
                        endDate
                )
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<HomeworkResponseDto> getActiveHomeworkByClassId(Long classId) {

        logger.info("Fetching active homework for class ID: {}", classId);

        classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Class not found with id: " + classId
                ));

        return homeworkRepository
                .findByClassEntityIdAndDueDateAfterOrderByCreatedAtDescIdDesc(
                        classId,
                        LocalDate.now()
                )
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<HomeworkResponseDto> getHomeworkByClassIdAndSubject(
            Long classId,
            Long subjectId) {

        logger.info(
                "Fetching homework for class ID: {} and subject ID: {}",
                classId,
                subjectId
        );

        classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Class not found with id: " + classId
                ));

        subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Subject not found with id: " + subjectId
                ));

        return homeworkRepository
                .findByClassEntityIdAndSubjectIdOrderByDueDateDesc(
                        classId,
                        subjectId
                )
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }


    @Override
    public HomeworkResponseDto updateHomework(
            Long id,
            HomeworkRequestDto request) {

        logger.info("Updating homework with ID: {}", id);

        Homework homework = homeworkRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Homework not found with id: " + id
                ));

        Teacher teacher = teacherRepository.findById(
                        request.getTeacherId()
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Teacher not found with id: "
                                + request.getTeacherId()
                ));

        validateTeacherAccess(request);

        validateDueDate(request.getDueDate());

        ClassEntity classEntity = classRepository.findById(
                        request.getClassId()
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Class not found with id: "
                                + request.getClassId()
                ));

        Section section = getSection(request.getSectionId());

        if (section != null) {
            validateSectionBelongsToClass(
                    section,
                    classEntity
            );
        }

        Subject subject = getAndValidateSubject(
                request.getSubjectId(),
                classEntity.getId()
        );

        homework.setClassEntity(classEntity);
        homework.setSection(section);
        homework.setAssignedByTeacher(teacher);
        homework.setSubject(subject);
        homework.setTitle(request.getTitle());
        homework.setDescription(request.getDescription());
        homework.setDueDate(request.getDueDate());
        homework.setStatus(
                request.getStatus() != null
                        ? request.getStatus()
                        : "ACTIVE"
        );
        homework.setAttachmentUrl(request.getAttachmentUrl());
        homework.setPriority(
                request.getPriority() != null
                        ? request.getPriority()
                        : "MEDIUM"
        );

        Homework updatedHomework = homeworkRepository.save(homework);

        logger.info(
                "Homework updated successfully with ID: {}",
                updatedHomework.getId()
        );

        return mapToResponseDto(updatedHomework);
    }

    @Override
    public void deleteHomework(Long id) {

        logger.info("Deleting homework with ID: {}", id);

        Homework homework = homeworkRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Homework not found with id: " + id
                ));

        homeworkRepository.delete(homework);

        logger.info(
                "Homework deleted successfully with ID: {}",
                id
        );
    }

    private Section getSection(Long sectionId) {

        if (sectionId == null) {
            return null;
        }

        return sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Section not found with id: " + sectionId
                ));
    }

    private Subject getAndValidateSubject(
            Long subjectId,
            Long classId) {

        if (subjectId == null) {
            return null;
        }

        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Subject not found with id: " + subjectId
                ));

        boolean mapped = classSubjectRepository
                .existsByClassEntityIdAndSubjectId(
                        classId,
                        subjectId
                );

        if (!mapped) {
            throw new IllegalArgumentException(
                    "Subject is not assigned to the selected class"
            );
        }

        return subject;
    }

    private void validateDueDate(LocalDate dueDate) {

        if (dueDate == null) {
            throw new IllegalArgumentException(
                    "Due date is required"
            );
        }

        if (dueDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "Due date cannot be in the past"
            );
        }
    }

    private void validateSectionBelongsToClass(
            Section section,
            ClassEntity classEntity) {

        if (section.getClassEntity() == null
                || !classEntity.getId()
                .equals(section.getClassEntity().getId())) {

            throw new IllegalArgumentException(
                    "Section does not belong to class"
            );
        }
    }

    private void validateTeacherAccess(
            HomeworkRequestDto request) {

        // =========================================================
        // TEACHER + CLASS + SECTION + SUBJECT
        // =========================================================

        if (request.getSectionId() != null
                && request.getSubjectId() != null) {

            teacherAccessService
                    .validateTeacherAccessToClassSectionAndSubject(
                            request.getTeacherId(),
                            request.getClassId(),
                            request.getSectionId(),
                            request.getSubjectId()
                    );

            return;
        }

        // =========================================================
        // BACKWARD COMPATIBILITY
        // =========================================================

        if (request.getSubjectId() != null) {

            teacherAccessService
                    .validateTeacherAccessToClassAndSubject(
                            request.getTeacherId(),
                            request.getClassId(),
                            request.getSubjectId()
                    );

            return;
        }

        teacherAccessService.validateTeacherAccessToClass(
                request.getTeacherId(),
                request.getClassId()
        );
    }
    private HomeworkResponseDto mapToResponseDto(
            Homework homework) {

        return HomeworkResponseDto.builder()
                .id(homework.getId())

                .classId(
                        homework.getClassEntity() != null
                                ? homework.getClassEntity().getId()
                                : null
                )
                .className(
                        homework.getClassEntity() != null
                                ? homework.getClassEntity().getName()
                                : null
                )

                .sectionId(
                        homework.getSection() != null
                                ? homework.getSection().getId()
                                : null
                )
                .sectionName(
                        homework.getSection() != null
                                ? homework.getSection().getName()
                                : null
                )

                .assignedByTeacherId(
                        homework.getAssignedByTeacher() != null
                                ? homework.getAssignedByTeacher().getId()
                                : null
                )
                .assignedByTeacherName(
                        homework.getAssignedByTeacher() != null
                                ? homework.getAssignedByTeacher().getName()
                                : null
                )
                .teacherEmployeeId(
                        homework.getAssignedByTeacher() != null
                                ? homework.getAssignedByTeacher().getEmployeeId()
                                : null
                )

                .subjectId(
                        homework.getSubject() != null
                                ? homework.getSubject().getId()
                                : null
                )
                .subjectName(
                        homework.getSubject() != null
                                ? homework.getSubject().getName()
                                : null
                )
                .subjectCode(
                        homework.getSubject() != null
                                ? homework.getSubject().getCode()
                                : null
                )

                .title(homework.getTitle())
                .description(homework.getDescription())
                .dueDate(homework.getDueDate())
                .status(homework.getStatus())
                .attachmentUrl(homework.getAttachmentUrl())
                .priority(homework.getPriority())
                .createdAt(homework.getCreatedAt())
                .updatedAt(homework.getUpdatedAt())
                .build();
    }




}