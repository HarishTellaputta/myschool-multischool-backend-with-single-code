
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

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.http.HttpStatus;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ParentFeedbackService {

    private final ParentFeedbackRepository feedbackRepository;
    private final ParentRepository parentRepository;
    private final StudentRepository studentRepository;

    @Transactional
    public ParentFeedbackResponseDto create(
            ParentFeedbackRequestDto request,
            String username) {

        Parent parent = parentRepository
                .findByUserUsername(username)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Parent profile not found for this login"
                ));

        Student student = null;

        if (request.getStudentId() != null) {
            student = studentRepository.findById(request.getStudentId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Student not found"
                    ));

            // A parent may submit feedback only for their own child.
            if (student.getParent() == null
                    || !student.getParent().getId().equals(parent.getId())) {
                throw new AccessDeniedException(
                        "You cannot submit feedback for this student"
                );
            }
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
        return feedbackRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public void deleteForAdmin(Long feedbackId) {
        ParentFeedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Complaint or suggestion not found"
                ));

        // Deletes only this feedback record.
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
