
package com.harish.student_management.dto;

import com.harish.student_management.entity.ParentFeedback.FeedbackType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ParentFeedbackRequestDto {

    @NotNull
    private FeedbackType type;

    // Optional for general suggestions or complaints.
    private Long studentId;

    @NotBlank
    @Size(max = 150)
    private String subject;

    @NotBlank
    @Size(max = 3000)
    private String message;
}
