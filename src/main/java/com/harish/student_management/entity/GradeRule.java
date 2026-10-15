package com.harish.student_management.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "grade_rules", uniqueConstraints = @UniqueConstraint(columnNames = "grade"))
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class GradeRule {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 20)
    private String grade;
    @Column(nullable = false)
    private Double minimumPercentage;
    @Column(nullable = false)
    private Double maximumPercentage;
}
