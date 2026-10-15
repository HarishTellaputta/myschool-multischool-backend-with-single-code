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
public class SchoolRequestDto {

    @NotBlank(message = "School name is required")
    @Size(min = 3, max = 100, message = "School name must be between 3 and 100 characters")
    private String name;

    @NotBlank(message = "School code is required")
    @Size(min = 2, max = 50, message = "School code must be between 2 and 50 characters")
    private String code;

    @Pattern(regexp = "^[0-9]{10,15}$", message = "Phone number must be between 10 and 15 digits")
    private String phone;

    @Email(message = "Enter a valid email")
    private String email;

    @Size(max = 255, message = "Address must not exceed 255 characters")
    private String address;

    @Size(max = 100, message = "City must not exceed 100 characters")
    private String city;

    @Size(max = 50, message = "State must not exceed 50 characters")
    private String state;

    @Pattern(regexp = "^[0-9]{6,10}$", message = "Postal code must be between 6 and 10 digits")
    private String postalCode;

    @Size(max = 50, message = "Country must not exceed 50 characters")
    private String country;

    @Size(max = 255, message = "Website must not exceed 255 characters")
    private String website;

    @Size(max = 255, message = "Logo URL must not exceed 255 characters")
    private String logoUrl;

    @Size(max = 2000, message = "Principal message must not exceed 2000 characters")
    private String principalMessage;

    @Size(max = 100, message = "Principal name must not exceed 100 characters")
    private String principalName;

    @Size(max = 2000, message = "Facilities must not exceed 2000 characters")
    private String facilities;

    @Size(max = 2000, message = "Faculty info must not exceed 2000 characters")
    private String facultyInfo;

    @Pattern(regexp = "^[0-9]{4}$", message = "Establishment year must be 4 digits")
    private String establishmentYear;

    @Size(max = 50, message = "Affiliation number must not exceed 50 characters")
    private String affiliationNumber;

    @Size(max = 50, message = "School type must not exceed 50 characters")
    private String schoolType;

    @Size(max = 20, message = "Status must not exceed 20 characters")
    private String status;
}
