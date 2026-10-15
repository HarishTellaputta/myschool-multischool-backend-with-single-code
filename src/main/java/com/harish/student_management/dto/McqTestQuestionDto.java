package com.harish.student_management.dto;
import lombok.*;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class McqTestQuestionDto { private Long id; private String question,optionA,optionB,optionC,optionD; private Integer marks; }
