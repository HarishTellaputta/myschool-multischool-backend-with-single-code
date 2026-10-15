package com.harish.student_management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BirthdayChatReactionRequest {

    @NotBlank(message = "Reaction is required")
    @Size(max = 20)
    private String reaction;
}