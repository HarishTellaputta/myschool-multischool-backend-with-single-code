package com.harish.student_management.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParentRequestDto {

    @NotBlank(message = "Father name is required")
    @Size(min = 3, max = 100, message = "Father name must be between 3 and 100 characters")
    private String fatherName;

    @Size(max = 100, message = "Surname must not exceed 100 characters")
    private String surname;

    @Size(max = 100, message = "Mother name must not exceed 100 characters")
    private String motherName;

    @Size(max = 100, message = "Guardian name must not exceed 100 characters")
    private String guardianName;

    @Pattern(regexp = "^[0-9]{10,15}$", message = "Contact phone must be between 10 and 15 digits")
    private String contactPhone;

    @Email(message = "Enter a valid email")
    private String contactEmail;

    @Size(max = 20, message = "Relationship must not exceed 20 characters")
    private String relationship;

    @Size(max = 255, message = "Address must not exceed 255 characters")
    private String address;
}
