package com.harish.student_management.service;

import com.harish.student_management.dto.RoleRequestDto;
import com.harish.student_management.dto.RoleResponseDto;
import com.harish.student_management.entity.Role;
import com.harish.student_management.exception.ResourceNotFoundException;
import com.harish.student_management.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private static final Logger logger = LoggerFactory.getLogger(RoleServiceImpl.class);

    private final RoleRepository roleRepository;

    @Override
    public RoleResponseDto createRole(RoleRequestDto request) {
        logger.info("Creating role with name: {}", request.getName());

        if (roleRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Role already exists with name: " + request.getName());
        }

        Role role = Role.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();

        Role savedRole = roleRepository.save(role);
        logger.info("Role created successfully with ID: {}", savedRole.getId());

        return mapToResponseDto(savedRole);
    }

    @Override
    public RoleResponseDto getRoleById(Long id) {
        logger.info("Fetching role with ID: {}", id);
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + id));
        return mapToResponseDto(role);
    }

    @Override
    public List<RoleResponseDto> getAllRoles() {
        logger.info("Fetching all roles");
        return roleRepository.findAll().stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    public RoleResponseDto updateRole(Long id, RoleRequestDto request) {
        logger.info("Updating role with ID: {}", id);
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + id));

        if (!role.getName().equals(request.getName()) && roleRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Role already exists with name: " + request.getName());
        }

        role.setName(request.getName());
        role.setDescription(request.getDescription());

        Role updatedRole = roleRepository.save(role);
        logger.info("Role updated successfully with ID: {}", updatedRole.getId());

        return mapToResponseDto(updatedRole);
    }

    @Override
    public void deleteRole(Long id) {
        logger.info("Deleting role with ID: {}", id);
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + id));
        roleRepository.delete(role);
        logger.info("Role deleted successfully with ID: {}", id);
    }

    private RoleResponseDto mapToResponseDto(Role role) {
        return RoleResponseDto.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .createdAt(role.getCreatedAt())
                .updatedAt(role.getUpdatedAt())
                .build();
    }
}
