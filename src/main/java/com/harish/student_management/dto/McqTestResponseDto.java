package com.harish.student_management.dto;
import lombok.*; import java.time.*; import java.util.*;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class McqTestResponseDto { private Long id,classId,sectionId; private String className,sectionName,subject,status; private LocalDate date; private LocalTime startTime; private Integer duration,numberOfQuestions; private List<McqTestQuestionDto> questions; }
