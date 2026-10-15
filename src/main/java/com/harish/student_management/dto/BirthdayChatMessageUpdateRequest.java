package com.harish.student_management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BirthdayChatMessageUpdateRequest {

    @NotBlank(message = "Message is required")
    @Size(
            max = 2000,
            message = "Message must not exceed 2000 characters"
    )
    private String message;
}