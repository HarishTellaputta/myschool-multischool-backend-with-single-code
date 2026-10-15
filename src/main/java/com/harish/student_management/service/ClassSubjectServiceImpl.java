package com.harish.student_management.service;
import com.harish.student_management.dto.ClassSubjectResponse;
import com.harish.student_management.entity.ClassEntity;
import com.harish.student_management.entity.ClassSubject;
import com.harish.student_management.entity.Subject;
import com.harish.student_management.repository.ClassRepository;
import com.harish.student_management.repository.ClassSubjectRepository;
import com.harish.student_management.repository.SubjectRepository;
import com.harish.student_management.service.ClassSubjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ClassSubjectServiceImpl
        implements ClassSubjectService {

    private final ClassRepository classRepository;
    private final SubjectRepository subjectRepository;
    private final ClassSubjectRepository classSubjectRepository;

    @Override
    public ClassSubjectResponse assignSubjectToClass(
            Long classId,
            Long subjectId) {

        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() ->
                        new RuntimeException("Class not found"));

        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() ->
                        new RuntimeException("Subject not found"));

        if (!classEntity.getSchool().getId()
                .equals(subject.getSchool().getId())) {

            throw new RuntimeException(
                    "Class and Subject belong to different schools"
            );
        }

        if (classSubjectRepository
                .existsByClassEntityIdAndSubjectId(
                        classId,
                        subjectId)) {

            throw new RuntimeException(
                    "Subject already assigned to this class"
            );
        }

        ClassSubject classSubject = ClassSubject.builder()
                .classEntity(classEntity)
                .subject(subject)
                .build();

        return mapToResponse(
                classSubjectRepository.save(classSubject)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassSubjectResponse> getSubjectsByClass(
            Long classId) {

        return classSubjectRepository
                .findByClassEntityId(classId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public void removeSubjectFromClass(
            Long classId,
            Long subjectId) {

        if (!classSubjectRepository
                .existsByClassEntityIdAndSubjectId(
                        classId,
                        subjectId)) {

            throw new RuntimeException(
                    "Subject is not assigned to this class"
            );
        }

        classSubjectRepository
                .deleteByClassEntityIdAndSubjectId(
                        classId,
                        subjectId
                );
    }

    private ClassSubjectResponse mapToResponse(
            ClassSubject classSubject) {

        return ClassSubjectResponse.builder()
                .id(classSubject.getId())
                .classId(
                        classSubject.getClassEntity().getId()
                )
                .className(
                        classSubject.getClassEntity().getName()
                )
                .subjectId(
                        classSubject.getSubject().getId()
                )
                .subjectName(
                        classSubject.getSubject().getName()
                )
                .subjectCode(
                        classSubject.getSubject().getCode()
                )
                .build();
    }
}