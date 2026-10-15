package com.harish.student_management.entity;
import jakarta.persistence.*; import lombok.*;
@Entity @Table(name="mcq_test_answers",uniqueConstraints=@UniqueConstraint(columnNames={"attempt_id","question_id"}))
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class McqTestAnswer {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="attempt_id",nullable=false) private McqTestAttempt attempt;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="question_id",nullable=false) private McqQuestion question;
 @Column(name="selected_answer",length=1) private String selectedAnswer;
}
