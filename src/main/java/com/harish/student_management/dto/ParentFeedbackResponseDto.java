
package com.harish.student_management.dto;

import com.harish.student_management.entity.ParentFeedback.FeedbackType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ParentFeedbackResponseDto {

    private Long id;
    private FeedbackType type;
    private String subject;
    private String message;
    private LocalDateTime createdAt;

    private Long parentId;
    private String parentName;
    private String parentPhone;
    private String parentEmail;

    private Long studentId;
    private String studentName;
    private String admissionNo;
}
