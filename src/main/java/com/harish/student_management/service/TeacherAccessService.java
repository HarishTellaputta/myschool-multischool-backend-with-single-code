package com.harish.student_management.service;

import com.harish.student_management.entity.Teacher;
import com.harish.student_management.exception.ResourceNotFoundException;
import com.harish.student_management.repository.TeacherClassAssignmentRepository;
import com.harish.student_management.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TeacherAccessService {

    private static final Logger logger =
            LoggerFactory.getLogger(TeacherAccessService.class);

    private final TeacherRepository teacherRepository;
    private final TeacherClassAssignmentRepository assignmentRepository;


    // =========================================================
    // CHECK TEACHER ACCESS TO CLASS
    // =========================================================

    public boolean canTeacherAccessClass(
            Long teacherId,
            Long classId) {

        logger.debug(
                "Checking if teacher {} can access class {}",
                teacherId,
                classId
        );

        List<Long> accessibleClassIds =
                assignmentRepository
                        .findByTeacherId(teacherId)
                        .stream()
                        .map(assignment ->
                                assignment.getClassEntity().getId()
                        )
                        .distinct()
                        .toList();

        return accessibleClassIds.contains(classId);
    }


    // =========================================================
    // CHECK TEACHER ACCESS TO SUBJECT
    // =========================================================

    public boolean canTeacherAccessSubject(
            Long teacherId,
            Long subjectId) {

        logger.debug(
                "Checking if teacher {} can access subject {}",
                teacherId,
                subjectId
        );

        return assignmentRepository
                .existsByTeacherIdAndSubjectId(
                        teacherId,
                        subjectId
                );
    }


    // =========================================================
    // CHECK TEACHER ACCESS TO CLASS + SUBJECT
    // =========================================================

    public boolean canTeacherAccessClassAndSubject(
            Long teacherId,
            Long classId,
            Long subjectId) {

        logger.debug(
                "Checking if teacher {} can access class {} and subject {}",
                teacherId,
                classId,
                subjectId
        );

        return assignmentRepository
                .existsByTeacherIdAndClassEntityIdAndSubjectId(
                        teacherId,
                        classId,
                        subjectId
                );
    }


    // =========================================================
    // GET TEACHER BY USER ID
    // =========================================================

    public Teacher getTeacherByUserId(Long userId) {

        return teacherRepository
                .findByUserId(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Teacher profile not found for user ID: "
                                        + userId
                        )
                );
    }


    // =========================================================
    // VALIDATE TEACHER ACCESS TO CLASS
    // =========================================================

    public void validateTeacherAccessToClass(
            Long teacherId,
            Long classId) {

        if (!canTeacherAccessClass(
                teacherId,
                classId
        )) {

            throw new SecurityException(
                    "Teacher does not have access to this class"
            );
        }
    }


    // =========================================================
    // VALIDATE TEACHER ACCESS TO SUBJECT
    // =========================================================

    public void validateTeacherAccessToSubject(
            Long teacherId,
            Long subjectId) {

        if (!canTeacherAccessSubject(
                teacherId,
                subjectId
        )) {

            throw new SecurityException(
                    "Teacher does not have access to this subject"
            );
        }
    }


    // =========================================================
    // VALIDATE TEACHER ACCESS TO CLASS + SUBJECT
    // =========================================================

    public void validateTeacherAccessToClassAndSubject(
            Long teacherId,
            Long classId,
            Long subjectId) {

        if (!canTeacherAccessClassAndSubject(
                teacherId,
                classId,
                subjectId
        )) {

            throw new SecurityException(
                    "Teacher does not have access to this class for the specified subject"
            );
        }
    }

    public boolean canTeacherAccessClassSectionAndSubject(
            Long teacherId,
            Long classId,
            Long sectionId,
            Long subjectId) {

        return assignmentRepository
                .findByTeacherIdAndClassEntityIdAndSectionIdAndSubjectId(
                        teacherId,
                        classId,
                        sectionId,
                        subjectId
                )
                .isPresent();
    }
    public void validateTeacherAccessToClassSectionAndSubject(
            Long teacherId,
            Long classId,
            Long sectionId,
            Long subjectId) {

        if (!canTeacherAccessClassSectionAndSubject(
                teacherId,
                classId,
                sectionId,
                subjectId
        )) {
            throw new SecurityException(
                    "Teacher does not have access to this class, section and subject"
            );
        }
    }
}