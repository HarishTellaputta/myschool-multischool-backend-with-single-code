package com.harish.student_management.service;

import com.harish.student_management.dto.McqQuestionRequestDto;
import com.harish.student_management.dto.McqQuestionResponseDto;
import com.harish.student_management.entity.ClassEntity;
import com.harish.student_management.entity.McqQuestion;
import com.harish.student_management.entity.Section;
import com.harish.student_management.entity.Subject;
import com.harish.student_management.entity.Teacher;
import com.harish.student_management.exception.ResourceNotFoundException;
import com.harish.student_management.repository.ClassRepository;
import com.harish.student_management.repository.ClassSubjectRepository;
import com.harish.student_management.repository.McqQuestionRepository;
import com.harish.student_management.repository.SubjectRepository;
import com.harish.student_management.repository.TeacherRepository;
import com.harish.student_management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class McqQuestionServiceImpl implements McqQuestionService {

    private final McqQuestionRepository repository;
    private final ClassRepository classRepository;
    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final ClassSubjectRepository classSubjectRepository;
    private final TeacherAccessService teacherAccessService;

    // ============================================================
    // CREATE
    // ============================================================

    @Override
    public McqQuestionResponseDto create(
            McqQuestionRequestDto request,
            String username,
            boolean admin) {

        ClassEntity clazz = findClass(request.getClassId());

        Teacher teacher = resolveTeacher(username, admin);

        Subject subject = findAndValidateSubject(
                request.getSubjectId(),
                clazz
        );

        if (!admin) {
            teacherAccessService.validateTeacherAccessToClassAndSubject(
                    teacher.getId(),
                    clazz.getId(),
                    subject.getId()
            );
        }

        McqQuestion question = new McqQuestion();

        question.setCreatedByTeacher(teacher);

        apply(
                question,
                request,
                clazz,
                subject
        );

        return map(repository.save(question));
    }

    // ============================================================
    // UPDATE
    // ============================================================

    @Override
    public McqQuestionResponseDto update(
            Long id,
            McqQuestionRequestDto request,
            String username,
            boolean admin) {

        McqQuestion question = find(id);

        ClassEntity clazz =
                findClass(request.getClassId());

        Teacher teacher =
                resolveTeacher(username, admin);

        Subject subject =
                findAndValidateSubject(
                        request.getSubjectId(),
                        clazz
                );

        if (!admin) {

            teacherAccessService
                    .validateTeacherAccessToClassAndSubject(
                            teacher.getId(),
                            clazz.getId(),
                            subject.getId()
                    );

            if (question.getCreatedByTeacher() != null
                    && !question.getCreatedByTeacher()
                    .getId()
                    .equals(teacher.getId())) {

                throw new SecurityException(
                        "Teacher may only update their own questions"
                );
            }
        }

        apply(
                question,
                request,
                clazz,
                subject
        );

        return map(question);
    }

    // ============================================================
    // GET
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public McqQuestionResponseDto get(Long id) {

        return map(find(id));
    }

    // ============================================================
    // LIST
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public List<McqQuestionResponseDto> list(
            LocalDate date,
            Long classId,
            Long subjectId) {

        List<McqQuestion> questions;

        if (date != null && classId != null && subjectId != null) {

            questions =
                    repository.findByQuestionDateAndClassEntityIdAndSubjectId(
                            date,
                            classId,
                            subjectId
                    );

        } else if (classId != null && subjectId != null) {

            questions =
                    repository.findByClassEntityIdAndSubjectId(
                            classId,
                            subjectId
                    );

        } else if (classId != null) {

            questions =
                    repository.findByClassEntityId(classId);

        } else if (subjectId != null) {

            questions =
                    repository.findBySubjectId(subjectId);

        } else if (date != null) {

            questions =
                    repository.findByQuestionDate(date);

        } else {

            questions =
                    repository.findAll();
        }

        return questions
                .stream()
                .map(this::map)
                .toList();
    }

    // ============================================================
    // DELETE
    // ============================================================

    @Override
    public void delete(
            Long id,
            String username,
            boolean admin) {

        McqQuestion question = find(id);

        Teacher teacher =
                resolveTeacher(username, admin);

        if (!admin) {

            if (question.getSubject() == null) {
                throw new SecurityException(
                        "MCQ question has no subject assigned"
                );
            }

            teacherAccessService
                    .validateTeacherAccessToClassAndSubject(
                            teacher.getId(),
                            question.getClassEntity().getId(),
                            question.getSubject().getId()
                    );

            if (question.getCreatedByTeacher() != null
                    && !question.getCreatedByTeacher()
                    .getId()
                    .equals(teacher.getId())) {

                throw new SecurityException(
                        "Teacher may only delete their own questions"
                );
            }
        }

        repository.delete(question);
    }

    // ============================================================
    // APPLY
    // ============================================================

    private void apply(
            McqQuestion question,
            McqQuestionRequestDto request,
            ClassEntity clazz,
            Subject subject) {

        question.setSubject(subject);

        question.setClassEntity(clazz);

        question.setQuestionDate(
                request.getQuestionDate() == null
                        ? LocalDate.now()
                        : request.getQuestionDate()
        );

        question.setQuestion(
                request.getQuestion().trim()
        );

        question.setQuestionImageUrl(
                request.getQuestionImageUrl()
        );

        question.setOptionA(
                request.getOptionA().trim()
        );

        question.setOptionAImageUrl(
                request.getOptionAImageUrl()
        );

        question.setOptionB(
                request.getOptionB().trim()
        );

        question.setOptionBImageUrl(
                request.getOptionBImageUrl()
        );

        question.setOptionC(
                request.getOptionC().trim()
        );

        question.setOptionCImageUrl(
                request.getOptionCImageUrl()
        );

        question.setOptionD(
                request.getOptionD().trim()
        );

        question.setOptionDImageUrl(
                request.getOptionDImageUrl()
        );

        question.setCorrectAnswer(
                request.getCorrectAnswer()
                        .trim()
                        .toUpperCase()
        );

        question.setMarks(
                request.getMarks()
        );

        question.setExplanation(
                request.getExplanation() == null
                        ? null
                        : request.getExplanation().trim()
        );
    }

    // ============================================================
    // SUBJECT VALIDATION
    // ============================================================

    private Subject findAndValidateSubject(
            Long subjectId,
            ClassEntity clazz) {

        Subject subject =
                subjectRepository.findById(subjectId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Subject not found: "
                                                + subjectId
                                )
                        );

        boolean assignedToClass =
                classSubjectRepository
                        .existsByClassEntityIdAndSubjectId(
                                clazz.getId(),
                                subjectId
                        );

        if (!assignedToClass) {
            throw new IllegalArgumentException(
                    "Subject is not assigned to this class"
            );
        }

        if (subject.getSchool() == null
                || clazz.getSchool() == null
                || !subject.getSchool()
                .getId()
                .equals(clazz.getSchool().getId())) {

            throw new IllegalArgumentException(
                    "Subject does not belong to the same school as the class"
            );
        }

        return subject;
    }

    // ============================================================
    // TEACHER
    // ============================================================

    private Teacher resolveTeacher(
            String username,
            boolean admin) {

        if (admin) {
            return null;
        }

        Long userId =
                userRepository
                        .findByUsername(username)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Authenticated user not found"
                                )
                        )
                        .getId();

        return teacherRepository
                .findByUserId(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Teacher profile not found for authenticated user"
                        )
                );
    }

    // ============================================================
    // CLASS
    // ============================================================

    private ClassEntity findClass(Long id) {

        return classRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Class not found: " + id
                        )
                );
    }

    // ============================================================
    // MCQ
    // ============================================================

    private McqQuestion find(Long id) {

        return repository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "MCQ question not found: " + id
                        )
                );
    }

    // ============================================================
    // MAPPER
    // ============================================================

    private McqQuestionResponseDto map(
            McqQuestion question) {

        Subject subject =
                question.getSubject();

        return McqQuestionResponseDto.builder()

                .id(question.getId())

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

                .classId(
                        question.getClassEntity() != null
                                ? question.getClassEntity().getId()
                                : null
                )

                .className(
                        question.getClassEntity() != null
                                ? question.getClassEntity().getName()
                                : null
                )

                .createdByTeacherId(
                        question.getCreatedByTeacher() == null
                                ? null
                                : question.getCreatedByTeacher().getId()
                )

                .questionDate(
                        question.getQuestionDate()
                )

                .question(
                        question.getQuestion()
                )

                .questionImageUrl(
                        question.getQuestionImageUrl()
                )

                .optionA(
                        question.getOptionA()
                )

                .optionAImageUrl(
                        question.getOptionAImageUrl()
                )

                .optionB(
                        question.getOptionB()
                )

                .optionBImageUrl(
                        question.getOptionBImageUrl()
                )

                .optionC(
                        question.getOptionC()
                )

                .optionCImageUrl(
                        question.getOptionCImageUrl()
                )

                .optionD(
                        question.getOptionD()
                )

                .optionDImageUrl(
                        question.getOptionDImageUrl()
                )

                .correctAnswer(
                        question.getCorrectAnswer()
                )

                .marks(
                        question.getMarks()
                )

                .explanation(
                        question.getExplanation()
                )

                .createdAt(
                        question.getCreatedAt()
                )

                .updatedAt(
                        question.getUpdatedAt()
                )

                .build();
    }
}