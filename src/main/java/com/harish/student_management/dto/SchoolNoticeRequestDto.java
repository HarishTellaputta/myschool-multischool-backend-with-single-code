package com.harish.student_management.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
public class SchoolNoticeRequestDto {
    @NotBlank
    String title;
    @NotBlank
    String content;
    @NotBlank
    String audience;
    String category;
    Long classId;
    Long sectionId;
    String status;
}
