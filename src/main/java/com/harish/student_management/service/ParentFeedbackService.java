
package com.harish.student_management.service;

import com.harish.student_management.dto.ParentFeedbackRequestDto;
import com.harish.student_management.dto.ParentFeedbackResponseDto;
import com.harish.student_management.entity.Parent;
import com.harish.student_management.entity.ParentFeedback;
import com.harish.student_management.entity.Student;
import com.harish.student_management.repository.ParentFeedbackRepository;
import com.harish.student_management.repository.ParentRepository;
import com.harish.student_management.repository.StudentRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ParentFeedbackService {

    private final ParentFeedbackRepository feedbackRepository;
    private final ParentRepository parentRepository;
    private final StudentRepository studentRepository;
    private final CurrentSchoolService currentSchoolService;

    @Transactional
    public ParentFeedbackResponseDto create(
            ParentFeedbackRequestDto request,
            String username) {

        if (request == null || username == null || username.isBlank()) {
            throw new IllegalArgumentException(
                    "Valid feedback request and login are required"
            );
        }

        Long schoolId = currentSchoolService.getCurrentSchoolId();

        Parent parent = parentRepository
                .findByUser_UsernameAndUser_School_Id(username, schoolId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Parent profile not found in the current school"
                ));

        Student student = null;

        if (request.getStudentId() != null) {
            student = studentRepository
                    .findByIdAndSection_ClassEntity_School_Id(
                            request.getStudentId(),
                            schoolId
                    )
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Student not found in the current school"
                    ));

            if (student.getParent() == null
                    || !student.getParent().getId().equals(parent.getId())) {
                throw new AccessDeniedException(
                        "You can submit feedback only for your own child"
                );
            }
        }

        if (request.getSubject() == null
                || request.getSubject().isBlank()
                || request.getMessage() == null
                || request.getMessage().isBlank()
                || request.getType() == null) {
            throw new IllegalArgumentException(
                    "Feedback type, subject and message are required"
            );
        }

        ParentFeedback feedback = ParentFeedback.builder()
                .parent(parent)
                .student(student)
                .type(request.getType())
                .subject(request.getSubject().trim())
                .message(request.getMessage().trim())
                .build();

        return toDto(feedbackRepository.save(feedback));
    }

    @Transactional(readOnly = true)
    public List<ParentFeedbackResponseDto> listForAdmin() {

        Long schoolId = currentSchoolService.getCurrentSchoolId();

        return feedbackRepository
                .findAllByParent_User_School_IdOrderByCreatedAtDesc(schoolId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public void deleteForAdmin(Long feedbackId) {

        if (feedbackId == null) {
            throw new IllegalArgumentException(
                    "Feedback ID is required"
            );
        }

        Long schoolId = currentSchoolService.getCurrentSchoolId();

        ParentFeedback feedback = feedbackRepository
                .findByIdAndParent_User_School_Id(feedbackId, schoolId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Feedback not found in the current school"
                ));

        feedbackRepository.delete(feedback);
    }

    private ParentFeedbackResponseDto toDto(
            ParentFeedback feedback) {

        Parent parent = feedback.getParent();
        Student student = feedback.getStudent();

        String parentName = parent.getFatherName();

        if (parentName == null || parentName.isBlank()) {
            parentName = parent.getMotherName();
        }

        if (parentName == null || parentName.isBlank()) {
            parentName = parent.getGuardianName();
        }

        if (parentName == null || parentName.isBlank()) {
            parentName = "Parent";
        }

        return ParentFeedbackResponseDto.builder()
                .id(feedback.getId())
                .type(feedback.getType())
                .subject(feedback.getSubject())
                .message(feedback.getMessage())
                .createdAt(feedback.getCreatedAt())
                .parentId(parent.getId())
                .parentName(parentName)
                .parentPhone(parent.getContactPhone())
                .parentEmail(parent.getEmail())
                .studentId(student != null ? student.getId() : null)
                .studentName(student != null ? student.getName() : null)
                .admissionNo(student != null ? student.getAdmissionNo() : null)
                .build();
    }
}
