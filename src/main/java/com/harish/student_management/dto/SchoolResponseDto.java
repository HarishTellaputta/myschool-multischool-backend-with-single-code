package com.harish.student_management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchoolResponseDto {

    private Long id;
    private String name;
    private String code;
    private String phone;
    private String email;
    private String address;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private String website;
    private String logoUrl;
    private String principalMessage;
    private String principalName;
    private String facilities;
    private String facultyInfo;
    private String establishmentYear;
    private String affiliationNumber;
    private String schoolType;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
