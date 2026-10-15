package com.harish.student_management.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SubjectRequest {

    @NotBlank(message = "Subject name is required")
    @Size(max = 100)
    private String name;

    @NotBlank(message = "Subject code is required")
    @Size(max = 30)
    private String code;

    @Size(max = 255)
    private String description;

    private String status;

    private Long schoolId;
}