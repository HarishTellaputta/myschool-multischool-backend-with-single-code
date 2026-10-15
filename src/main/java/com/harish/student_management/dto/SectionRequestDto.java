package com.harish.student_management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SectionRequestDto {

    @NotNull(message = "Class ID is required")
    private Long classId;

    @NotBlank(message = "Section name is required")
    @Size(min = 1, max = 20, message = "Section name must be between 1 and 20 characters")
    private String name;

    private Integer capacity;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;
}
