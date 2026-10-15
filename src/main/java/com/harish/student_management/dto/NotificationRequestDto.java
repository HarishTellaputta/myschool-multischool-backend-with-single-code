package com.harish.student_management.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NotificationRequestDto {

    @NotBlank
    private String type;

    @NotBlank
    private String title;

    @NotBlank
    private String message;

    private String payload;

    private Long recipientUserId;

    private String recipientRole;

    private Long recipientId;

    // Admin notice fields
    private String category;

    private String audience;

    private String status;
}