package com.harish.student_management.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.*;

@Entity @Table(name="mcq_tests", uniqueConstraints=@UniqueConstraint(columnNames={"class_id","section_id","subject","test_date"}))
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class McqTest {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="class_id",nullable=false) private ClassEntity classEntity;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="section_id") private Section section;
 @Column(nullable=false,length=100) private String subject;
 @Column(name="test_date",nullable=false) private LocalDate testDate;
 @Column(name="start_time",nullable=false) private LocalTime startTime;
 @Column(nullable=false) private Integer duration;
 @Column(name="number_of_questions",nullable=false) private Integer numberOfQuestions;
 @Column(nullable=false,length=20) private String status;
 @Column(name="created_at",updatable=false) private LocalDateTime createdAt;
 @PrePersist void create(){createdAt=LocalDateTime.now(); if(status==null)status="SCHEDULED";}
}
