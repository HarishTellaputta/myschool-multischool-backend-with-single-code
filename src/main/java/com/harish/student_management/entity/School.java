package com.harish.student_management.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "schools")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class School {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String email;

    @Column(length = 255)
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 50)
    private String state;

    @Column(length = 10)
    private String postalCode;

    @Column(length = 50)
    private String country;

    @Column(length = 255)
    private String website;

    @Column(name = "logo_url", length = 255)
    private String logoUrl;

    @Column(name = "principal_message", columnDefinition = "TEXT")
    private String principalMessage;

    @Column(name = "principal_name", length = 100)
    private String principalName;

    @Column(name = "facilities", columnDefinition = "TEXT")
    private String facilities;

    @Column(name = "faculty_info", columnDefinition = "TEXT")
    private String facultyInfo;

    @Column(name = "establishment_year", length = 4)
    private String establishmentYear;

    @Column(name = "affiliation_number", length = 50)
    private String affiliationNumber;

    @Column(name = "school_type", length = 50)
    private String schoolType;

    @Column(length = 20)
    private String status;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = "ACTIVE";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
