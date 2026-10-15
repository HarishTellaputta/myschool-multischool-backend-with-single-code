
package com.harish.student_management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GroupChatReactionRequest {

    @NotBlank(message = "Reaction is required")
    @Size(max = 20, message = "Reaction is too long")
    private String reaction;
}
