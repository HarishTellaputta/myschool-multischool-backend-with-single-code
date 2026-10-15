package com.harish.student_management.service;

import com.harish.student_management.dto.ParentRequestDto;
import com.harish.student_management.dto.ParentResponseDto;

import java.util.List;

public interface ParentService {

    ParentResponseDto createParent(ParentRequestDto request);

    ParentResponseDto getParentById(Long id);

    List<ParentResponseDto> getAllParents();

    ParentResponseDto updateParent(Long id, ParentRequestDto request);

    void deleteParent(Long id);

    ParentResponseDto linkStudent(Long parentId, Long studentId);

    ParentResponseDto unlinkStudent(Long parentId, Long studentId);

    ParentResponseDto getMyProfile(String username);
}