package com.harish.student_management.entity;
import jakarta.persistence.*; import lombok.*; import java.time.*;
@Entity @Table(name="mcq_test_attempts",uniqueConstraints=@UniqueConstraint(columnNames={"test_id","student_id"}))
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class McqTestAttempt {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="test_id",nullable=false) private McqTest test;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="student_id",nullable=false) private Student student;
 @Column(name="started_at",nullable=false) private LocalDateTime startedAt;
 @Column(name="expires_at",nullable=false) private LocalDateTime expiresAt;
 @Column(name="submitted_at") private LocalDateTime submittedAt;
 @Column(nullable=false,length=20) private String status;
 @Column(name="score") private Integer score;
 @Column(name="total_questions") private Integer totalQuestions;
 @Column(name="correct_answers") private Integer correctAnswers;
 @Column(name="wrong_answers") private Integer wrongAnswers;
 @Column(name="percentage") private Double percentage;
 @PrePersist void create(){if(status==null)status="IN_PROGRESS";}
}
