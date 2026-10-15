package com.harish.student_management.dto;

import lombok.*;

import java.time.*;

@Data
@Builder
public class SchoolNoticeResponseDto {
    Long id;
    String title;
    String content;
    String audience;
    String category;
    String status;
    Long classId;
    Long sectionId;
    String className;
    String sectionName;
    LocalDateTime publishedAt;
    LocalDateTime createdAt;
}