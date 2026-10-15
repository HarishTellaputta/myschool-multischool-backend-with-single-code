package com.harish.student_management.service;

import com.harish.student_management.dto.StudentBirthdayChatResponse;
import com.harish.student_management.dto.StudentBirthdayStatusRequest;
import com.harish.student_management.dto.StudentBirthdayStatusResponse;
import com.harish.student_management.dto.StudentBirthdayStatusUpdateRequest;

import java.time.LocalDate;
import java.util.List;

public interface StudentBirthdayStatusService {

    StudentBirthdayStatusResponse create(
            String username,
            StudentBirthdayStatusRequest request
    );

    StudentBirthdayStatusResponse getById(
            String username,
            Long id
    );

    List<StudentBirthdayStatusResponse> getAll(
            String username
    );

    List<StudentBirthdayStatusResponse> getToday(
            String username,
            LocalDate date
    );

    List<StudentBirthdayStatusResponse> getByStudent(
            String username,
            Long studentId
    );

    StudentBirthdayStatusResponse update(
            String username,
            Long id,
            StudentBirthdayStatusUpdateRequest request
    );

    void delete(
            String username,
            Long id
    );

    List<StudentBirthdayChatResponse> getBirthdayChat(
            String username
    );
}