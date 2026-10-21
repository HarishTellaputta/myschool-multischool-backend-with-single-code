
package com.harish.student_management.service;

import com.harish.student_management.dto.AvailableTeacherUserResponseDto;
import com.harish.student_management.dto.TeacherRequestDto;
import com.harish.student_management.dto.TeacherResponseDto;
import com.harish.student_management.entity.School;
import com.harish.student_management.entity.Teacher;
import com.harish.student_management.entity.User;
import com.harish.student_management.exception.ResourceNotFoundException;
import com.harish.student_management.repository.SchoolRepository;
import com.harish.student_management.repository.TeacherRepository;
import com.harish.student_management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TeacherServiceImpl implements TeacherService {

    private static final Logger logger =
            LoggerFactory.getLogger(TeacherServiceImpl.class);

    private final TeacherRepository teacherRepository;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final EmployeeIdGeneratorService employeeIdGeneratorService;
    private final CurrentSchoolService currentSchoolService;

    @Override
    public TeacherResponseDto createTeacher(TeacherRequestDto request) {

        Long schoolId = currentSchoolService.getCurrentSchoolId();

        if (request.getUserId() == null) {
            throw new IllegalArgumentException(
                    "Registered teacher user is required"
            );
        }

        // Always use the authenticated user's school.
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "School not found with id: " + schoolId
                ));

        // Do not allow a request to select another school.
        if (request.getSchoolId() != null
                && !request.getSchoolId().equals(schoolId)) {
            throw new IllegalArgumentException(
                    "You cannot create a teacher for another school"
            );
        }

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Registered user not found with id: "
                                + request.getUserId()
                ));

        validateTeacherUser(user, schoolId);

        if (teacherRepository.existsByUserId(user.getId())) {
            throw new IllegalArgumentException(
                    "This user is already registered as a teacher"
            );
        }

        // Employee IDs remain globally unique with the current DB constraint.
        String employeeId =
                employeeIdGeneratorService.generateNextEmployeeId(schoolId);

        Teacher teacher = Teacher.builder()
                .school(school)
                .user(user)
                .employeeId(employeeId)
                .name(user.getUsername())
                .surname(request.getSurname())
                .email(user.getEmail())
                .phone(request.getPhone())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .joiningDate(request.getJoiningDate())
                .qualification(request.getQualification())
                .designation(request.getDesignation())
                .address(request.getAddress())
                .status(request.getStatus() != null
                        ? request.getStatus()
                        : "ACTIVE")
                .build();

        Teacher savedTeacher = teacherRepository.save(teacher);

        logger.info(
                "Teacher created. Teacher ID: {}, School ID: {}",
                savedTeacher.getId(),
                schoolId
        );

        return mapToResponseDto(savedTeacher);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvailableTeacherUserResponseDto> getAvailableTeacherUsers() {

        Long schoolId = currentSchoolService.getCurrentSchoolId();

        return userRepository
                .findAvailableUsersByRoleAndSchoolId("TEACHER", schoolId)
                .stream()
                .map(user -> AvailableTeacherUserResponseDto.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .status(user.getStatus())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherResponseDto getTeacherById(Long id) {

        Long schoolId = currentSchoolService.getCurrentSchoolId();

        Teacher teacher = findTeacherInCurrentSchool(id, schoolId);

        verifyTeacherOwnProfileAccess(teacher);

        return mapToResponseDto(teacher);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherResponseDto> getAllTeachers() {

        Long schoolId = currentSchoolService.getCurrentSchoolId();

        return teacherRepository.findAllBySchool_Id(schoolId)
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherResponseDto> getTeachersBySchoolId(Long schoolId) {

        Long currentSchoolId = currentSchoolService.getCurrentSchoolId();

        if (schoolId == null || !schoolId.equals(currentSchoolId)) {
            throw new IllegalArgumentException(
                    "You can access teachers only from your own school"
            );
        }

        return teacherRepository.findAllBySchool_Id(currentSchoolId)
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherResponseDto> getTeachersByStatus(String status) {

        Long schoolId = currentSchoolService.getCurrentSchoolId();

        return teacherRepository
                .findAllBySchool_IdAndStatusIgnoreCase(schoolId, status)
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    public TeacherResponseDto updateTeacher(
            Long id,
            TeacherRequestDto request) {

        Long schoolId = currentSchoolService.getCurrentSchoolId();

        Teacher teacher = findTeacherInCurrentSchool(id, schoolId);

        verifyTeacherOwnProfileAccess(teacher);

        // School reassignment through this endpoint is not allowed.
        if (request.getSchoolId() != null
                && !request.getSchoolId().equals(schoolId)) {
            throw new IllegalArgumentException(
                    "A teacher cannot be reassigned to another school"
            );
        }

        if (request.getUserId() != null
                && !request.getUserId().equals(
                teacher.getUser() != null
                        ? teacher.getUser().getId()
                        : null)) {

            User user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "User not found with id: " + request.getUserId()
                    ));

            validateTeacherUser(user, schoolId);

            Teacher existing = teacherRepository.findByUserId(user.getId())
                    .orElse(null);

            if (existing != null
                    && !existing.getId().equals(teacher.getId())) {
                throw new IllegalArgumentException(
                        "Selected user already has a teacher profile"
                );
            }

            teacher.setUser(user);
            teacher.setName(user.getUsername());
            teacher.setEmail(user.getEmail());
        }

        // Employee ID and school remain unchanged.
        if (request.getSurname() != null) {
            teacher.setSurname(request.getSurname());
        }

        if (request.getGender() != null) {
            teacher.setGender(request.getGender());
        }

        if (request.getPhone() != null) {
            teacher.setPhone(request.getPhone());
        }

        if (request.getJoiningDate() != null) {
            teacher.setJoiningDate(request.getJoiningDate());
        }

        if (request.getDateOfBirth() != null) {
            teacher.setDateOfBirth(request.getDateOfBirth());
        }

        if (request.getQualification() != null) {
            teacher.setQualification(request.getQualification());
        }

        if (request.getDesignation() != null) {
            teacher.setDesignation(request.getDesignation());
        }

        if (request.getAddress() != null) {
            teacher.setAddress(request.getAddress());
        }

        if (request.getStatus() != null) {
            teacher.setStatus(request.getStatus());
        }

        Teacher updatedTeacher = teacherRepository.save(teacher);

        return mapToResponseDto(updatedTeacher);
    }

    @Override
    public void deleteTeacher(Long id) {

        Long schoolId = currentSchoolService.getCurrentSchoolId();

        Teacher teacher = findTeacherInCurrentSchool(id, schoolId);

        teacherRepository.delete(teacher);

        logger.info(
                "Teacher deleted. Teacher ID: {}, School ID: {}",
                id,
                schoolId
        );
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherResponseDto getTeacherByUserId(Long userId) {

        Long schoolId = currentSchoolService.getCurrentSchoolId();

        if (userId == null) {
            throw new IllegalArgumentException("User ID is required");
        }

        Teacher teacher = teacherRepository
                .findByUser_IdAndSchool_Id(userId, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Teacher not found for user ID: " + userId
                ));

        verifyTeacherOwnProfileAccess(teacher);

        return mapToResponseDto(teacher);
    }

    private Teacher findTeacherInCurrentSchool(
            Long teacherId,
            Long schoolId) {

        return teacherRepository
                .findByIdAndSchoolId(teacherId, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Teacher not found with id: " + teacherId
                ));
    }

    private void validateTeacherUser(User user, Long schoolId) {

        if (user.getRole() == null
                || !"TEACHER".equalsIgnoreCase(user.getRole().getName())) {
            throw new IllegalArgumentException(
                    "Selected user is not registered with TEACHER role"
            );
        }

        if (user.getSchool() == null
                || user.getSchool().getId() == null
                || !user.getSchool().getId().equals(schoolId)) {
            throw new IllegalArgumentException(
                    "Selected teacher user does not belong to your school"
            );
        }
    }

    /**
     * If the authenticated principal is a TEACHER, it may access
     * only the profile linked to its own username.
     * School admins remain restricted to their own school by the
     * school-scoped lookup performed before this check.
     */
    private void verifyTeacherOwnProfileAccess(Teacher teacher) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null
                || "anonymousUser".equals(authentication.getName())) {
            throw new AuthenticationCredentialsNotFoundException(
                    "Authenticated user not found"
            );
        }

        boolean isTeacher = authentication.getAuthorities()
                .stream()
                .anyMatch(authority -> {
                    String role = authority.getAuthority();
                    return "ROLE_TEACHER".equals(role)
                            || "TEACHER".equals(role);
                });

        if (isTeacher) {
            String username = authentication.getName();

            if (teacher.getUser() == null
                    || teacher.getUser().getUsername() == null
                    || !teacher.getUser().getUsername().equals(username)) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "Teachers can access only their own profile"
                );
            }
        }
    }

    private TeacherResponseDto mapToResponseDto(Teacher teacher) {

        return TeacherResponseDto.builder()
                .id(teacher.getId())
                .schoolId(
                        teacher.getSchool() != null
                                ? teacher.getSchool().getId()
                                : null
                )
                .schoolName(
                        teacher.getSchool() != null
                                ? teacher.getSchool().getName()
                                : null
                )
                .userId(
                        teacher.getUser() != null
                                ? teacher.getUser().getId()
                                : null
                )
                .employeeId(teacher.getEmployeeId())
                .name(teacher.getName())
                .surname(teacher.getSurname())
                .email(teacher.getEmail())
                .phone(teacher.getPhone())
                .dateOfBirth(teacher.getDateOfBirth())
                .gender(teacher.getGender())
                .joiningDate(teacher.getJoiningDate())
                .qualification(teacher.getQualification())
                .designation(teacher.getDesignation())
                .address(teacher.getAddress())
                .status(teacher.getStatus())
                .createdAt(teacher.getCreatedAt())
                .updatedAt(teacher.getUpdatedAt())
                .build();
    }
}
