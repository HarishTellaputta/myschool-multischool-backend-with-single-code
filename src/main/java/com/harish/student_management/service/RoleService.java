package com.harish.student_management.service;

import com.harish.student_management.dto.RoleRequestDto;
import com.harish.student_management.dto.RoleResponseDto;

import java.util.List;

public interface RoleService {

    RoleResponseDto createRole(RoleRequestDto request);

    RoleResponseDto getRoleById(Long id);

    List<RoleResponseDto> getAllRoles();

    RoleResponseDto updateRole(Long id, RoleRequestDto request);

    void deleteRole(Long id);
}
