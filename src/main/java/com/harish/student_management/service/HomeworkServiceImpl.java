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
    private final CurrentSchoolService currentSchoolService;

    // =========================================================
    // CREATE HOMEWORK
    // =========================================================

    @Override
    public HomeworkResponseDto createHomework(HomeworkRequestDto request) {

        Long schoolId = getSchoolId();

        logger.info(
                "Creating homework for class: {} by teacher: {}",
                request.getClassId(),
                request.getTeacherId()
        );

        ClassEntity classEntity =
                getClassForCurrentSchool(request.getClassId());

        Teacher teacher =
                getTeacherForCurrentSchool(request.getTeacherId());

        Section section =
                getSectionForCurrentSchool(request.getSectionId());

        validateSectionBelongsToClass(section, classEntity);

        Subject subject = getAndValidateSubject(
                request.getSubjectId(),
                classEntity.getId()
        );

        validateTeacherAccess(request);
        validateDueDate(request.getDueDate());

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

        // Prevent accidental cross-school entity assignment.
        validateEntitiesBelongToSchool(
                schoolId, classEntity, teacher, section, subject
        );

        Homework saved = homeworkRepository.save(homework);

        logger.info("Homework created successfully with ID: {}", saved.getId());

        return mapToResponseDto(saved);
    }

    // =========================================================
    // GET HOMEWORK BY ID
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public HomeworkResponseDto getHomeworkById(Long id) {

        Homework homework = homeworkRepository
                .findByIdAndSchool(id, getSchoolId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Homework not found with id: " + id
                ));

        return mapToResponseDto(homework);
    }

    // =========================================================
    // GET ALL HOMEWORK FOR CURRENT SCHOOL
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<HomeworkResponseDto> getAllHomework() {

        return homeworkRepository
                .findAllBySchoolId(getSchoolId())
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    // =========================================================
    // GET HOMEWORK BY CLASS
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<HomeworkResponseDto> getHomeworkByClassId(Long classId) {

        getClassForCurrentSchool(classId);

        return homeworkRepository
                .findByClassAndSchool(classId, getSchoolId())
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    // =========================================================
    // GET HOMEWORK BY SECTION
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<HomeworkResponseDto> getHomeworkBySectionId(Long sectionId) {

        getSectionForCurrentSchool(sectionId);

        return homeworkRepository
                .findBySectionAndSchool(sectionId, getSchoolId())
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    // =========================================================
    // GET HOMEWORK BY CLASS + SECTION
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<HomeworkResponseDto> getHomeworkByClassIdAndSectionId(
            Long classId,
            Long sectionId) {

        ClassEntity classEntity = getClassForCurrentSchool(classId);
        Section section = getSectionForCurrentSchool(sectionId);

        validateSectionBelongsToClass(section, classEntity);

        return homeworkRepository
                .findByClassSectionAndSchool(
                        classId, sectionId, getSchoolId()
                )
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    // =========================================================
    // GET HOMEWORK BY TEACHER
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<HomeworkResponseDto> getHomeworkByTeacherId(Long teacherId) {

        getTeacherForCurrentSchool(teacherId);

        return homeworkRepository
                .findByTeacherAndSchool(teacherId, getSchoolId())
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    // =========================================================
    // GET HOMEWORK BY SUBJECT
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<HomeworkResponseDto> getHomeworkBySubject(Long subjectId) {

        getSubjectForCurrentSchool(subjectId);

        return homeworkRepository
                .findBySubjectAndSchool(subjectId, getSchoolId())
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    // =========================================================
    // GET HOMEWORK BY CLASS + DATE RANGE
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<HomeworkResponseDto> getHomeworkByClassIdAndDueDateRange(
            Long classId,
            LocalDate startDate,
            LocalDate endDate) {

        getClassForCurrentSchool(classId);
        validateDateRange(startDate, endDate);

        return homeworkRepository
                .findByClassDateRangeAndSchool(
                        classId, getSchoolId(), startDate, endDate
                )
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    // =========================================================
    // GET FUTURE HOMEWORK BY CLASS
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<HomeworkResponseDto> getActiveHomeworkByClassId(
            Long classId) {

        getClassForCurrentSchool(classId);

        return homeworkRepository
                .findFutureByClassAndSchool(
                        classId, getSchoolId(), LocalDate.now()
                )
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    // =========================================================
    // GET HOMEWORK BY CLASS + SUBJECT
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<HomeworkResponseDto> getHomeworkByClassIdAndSubject(
            Long classId,
            Long subjectId) {

        ClassEntity classEntity = getClassForCurrentSchool(classId);
        getSubjectForCurrentSchool(subjectId);

        // Confirm that the subject belongs to the selected class.
        getAndValidateSubject(subjectId, classEntity.getId());

        return homeworkRepository
                .findByClassSubjectAndSchool(
                        classId, subjectId, getSchoolId()
                )
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    // =========================================================
    // UPDATE HOMEWORK
    // =========================================================

    @Override
    public HomeworkResponseDto updateHomework(
            Long id,
            HomeworkRequestDto request) {

        Long schoolId = getSchoolId();

        Homework homework = homeworkRepository
                .findByIdAndSchool(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Homework not found with id: " + id
                ));

        ClassEntity classEntity =
                getClassForCurrentSchool(request.getClassId());

        Teacher teacher =
                getTeacherForCurrentSchool(request.getTeacherId());

        Section section =
                getSectionForCurrentSchool(request.getSectionId());

        validateSectionBelongsToClass(section, classEntity);

        Subject subject = getAndValidateSubject(
                request.getSubjectId(),
                classEntity.getId()
        );

        validateTeacherAccess(request);
        validateDueDate(request.getDueDate());

        validateEntitiesBelongToSchool(
                schoolId, classEntity, teacher, section, subject
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

        Homework updated = homeworkRepository.save(homework);

        logger.info("Homework updated successfully with ID: {}", updated.getId());

        return mapToResponseDto(updated);
    }

    // =========================================================
    // DELETE HOMEWORK
    // =========================================================

    @Override
    public void deleteHomework(Long id) {

        Homework homework = homeworkRepository
                .findByIdAndSchool(id, getSchoolId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Homework not found with id: " + id
                ));

        homeworkRepository.delete(homework);

        logger.info("Homework deleted successfully with ID: {}", id);
    }

    // =========================================================
    // CURRENT SCHOOL HELPERS
    // =========================================================

    private Long getSchoolId() {
        return currentSchoolService.getCurrentSchoolId();
    }

    private ClassEntity getClassForCurrentSchool(Long classId) {

        return classRepository
                .findByIdAndSchool_Id(classId, getSchoolId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Class not found with id: " + classId
                ));
    }

    private Teacher getTeacherForCurrentSchool(Long teacherId) {

        return teacherRepository
                .findByIdAndSchoolId(teacherId, getSchoolId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Teacher not found with id: " + teacherId
                ));
    }

    private Section getSectionForCurrentSchool(Long sectionId) {

        if (sectionId == null) {
            return null;
        }

        Section section = sectionRepository
                .findByIdWithClassAndSchool(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Section not found with id: " + sectionId
                ));

        Long schoolId = getSchoolId();

        if (section.getClassEntity() == null
                || section.getClassEntity().getSchool() == null
                || !schoolId.equals(
                section.getClassEntity().getSchool().getId()
        )) {
            throw new ResourceNotFoundException(
                    "Section not found with id: " + sectionId
            );
        }

        return section;
    }

    private Subject getSubjectForCurrentSchool(Long subjectId) {

        if (subjectId == null) {
            return null;
        }

        return subjectRepository
                .findByIdAndSchoolId(subjectId, getSchoolId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Subject not found with id: " + subjectId
                ));
    }

    // =========================================================
    // VALIDATION
    // =========================================================

    private Subject getAndValidateSubject(
            Long subjectId,
            Long classId) {

        if (subjectId == null) {
            return null;
        }

        Subject subject = getSubjectForCurrentSchool(subjectId);

        boolean mapped = classSubjectRepository
                .existsByClassEntityIdAndSubjectId(classId, subjectId);

        if (!mapped) {
            throw new IllegalArgumentException(
                    "Subject is not assigned to the selected class"
            );
        }

        return subject;
    }

    private void validateEntitiesBelongToSchool(
            Long schoolId,
            ClassEntity classEntity,
            Teacher teacher,
            Section section,
            Subject subject) {

        if (classEntity.getSchool() == null
                || !schoolId.equals(classEntity.getSchool().getId())) {
            throw new ResourceNotFoundException("Class not found");
        }

        if (teacher.getSchool() == null
                || !schoolId.equals(teacher.getSchool().getId())) {
            throw new ResourceNotFoundException("Teacher not found");
        }

        if (section != null
                && (section.getClassEntity() == null
                || section.getClassEntity().getSchool() == null
                || !schoolId.equals(
                section.getClassEntity().getSchool().getId()
        ))) {
            throw new ResourceNotFoundException("Section not found");
        }

        if (subject != null
                && (subject.getSchool() == null
                || !schoolId.equals(subject.getSchool().getId()))) {
            throw new ResourceNotFoundException("Subject not found");
        }
    }

    private void validateSectionBelongsToClass(
            Section section,
            ClassEntity classEntity) {

        if (section != null
                && (section.getClassEntity() == null
                || !classEntity.getId().equals(
                section.getClassEntity().getId()
        ))) {
            throw new IllegalArgumentException(
                    "Section does not belong to class"
            );
        }
    }

    private void validateDueDate(LocalDate dueDate) {

        if (dueDate == null) {
            throw new IllegalArgumentException("Due date is required");
        }

        if (dueDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "Due date cannot be in the past"
            );
        }
    }

    private void validateDateRange(
            LocalDate startDate,
            LocalDate endDate) {

        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException(
                    "Start date and end date are required"
            );
        }

        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException(
                    "End date must be on or after start date"
            );
        }
    }

    private void validateTeacherAccess(HomeworkRequestDto request) {

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

    // =========================================================
    // ENTITY -> RESPONSE DTO
    // =========================================================

    private HomeworkResponseDto mapToResponseDto(Homework homework) {

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