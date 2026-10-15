package com.harish.student_management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ConversationRequestDto {

    /*
     * Required only when PARENT starts chat.
     *
     * Teacher start:
     * Backend gets parent from Student.
     */
    private Long parentId;

    /*
     * Required only when PARENT starts chat.
     *
     * Teacher start:
     * Backend gets teacher from authenticated user.
     */
    private Long teacherId;

    @NotNull
    private Long studentId;

    private String subject;

    @NotBlank
    private String initialMessage;
}