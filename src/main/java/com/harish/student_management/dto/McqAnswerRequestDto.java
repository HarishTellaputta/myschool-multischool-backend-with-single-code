package com.harish.student_management.dto;
import jakarta.validation.constraints.*; import lombok.*;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class McqAnswerRequestDto { @NotNull private Long questionId; @NotBlank @Pattern(regexp="(?i)[ABCD]") private String answer; }
