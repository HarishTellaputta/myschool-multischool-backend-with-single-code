package com.harish.student_management.dto;

import lombok.*;
import java.time.*;

@Data
@Builder
public class NotificationResponseDto {

    Long id;

    String type;
    String title;
    String message;
    String payload;

    String recipientRole;

    Long recipientUserId;
    Long recipientId;

    Boolean read;

    LocalDateTime createdAt;
    LocalDateTime readAt;

    // Admin notice fields
    String category;
    String audience;
    String status;
    LocalDateTime publishedAt;
}