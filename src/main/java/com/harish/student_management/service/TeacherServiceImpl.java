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

    @Override
    public TeacherResponseDto createTeacher(TeacherRequestDto request) {

        logger.info(
                "Creating teacher profile for user ID: {}",
                request.getUserId()
        );

        if (request.getUserId() == null) {
            throw new IllegalArgumentException(
                    "Registered teacher user is required"
            );
        }

        /*
         * Find registered user
         */
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Registered user not found with id: "
                                        + request.getUserId()
                        )
                );

        /*
         * User must have TEACHER role
         */
        if (user.getRole() == null
                || !"TEACHER".equalsIgnoreCase(
                user.getRole().getName())) {

            throw new IllegalArgumentException(
                    "Selected user is not registered with TEACHER role"
            );
        }

        /*
         * One User = One Teacher profile
         */
        if (teacherRepository.existsByUserId(user.getId())) {
            throw new IllegalArgumentException(
                    "This user is already registered as a teacher"
            );
        }

        /*
         * School
         */
        School school = null;

        if (request.getSchoolId() != null) {

            school = schoolRepository.findById(
                            request.getSchoolId()
                    )
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "School not found with id: "
                                            + request.getSchoolId()
                            )
                    );
        }

        /*
         * AUTO GENERATE EMPLOYEE ID
         */
        String employeeId =
                employeeIdGeneratorService.generateNextEmployeeId();

        /*
         * User information comes from USER table.
         *
         * No manual name/email/phone required.
         */
        Teacher teacher = Teacher.builder()
                .school(school)
                .user(user)

                .employeeId(employeeId)

                .name(user.getUsername())
                .email(user.getEmail())

                .phone(request.getPhone())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .joiningDate(request.getJoiningDate())
                .qualification(request.getQualification())
                .designation(request.getDesignation())
                .address(request.getAddress())

                .status(
                        request.getStatus() != null
                                ? request.getStatus()
                                : "ACTIVE"
                )
                .build();

        Teacher savedTeacher = teacherRepository.save(teacher);

        logger.info(
                "Teacher created successfully. Teacher ID: {}, Employee ID: {}",
                savedTeacher.getId(),
                savedTeacher.getEmployeeId()
        );

        return mapToResponseDto(savedTeacher);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvailableTeacherUserResponseDto>
    getAvailableTeacherUsers() {

        logger.info("Fetching registered TEACHER users without teacher profile");

        return userRepository
                .findAvailableUsersByRole("TEACHER")
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

        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Teacher not found with id: " + id
                        ));

        return mapToResponseDto(teacher);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherResponseDto> getAllTeachers() {

        return teacherRepository.findAll()
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherResponseDto> getTeachersBySchoolId(
            Long schoolId) {

        return teacherRepository.findBySchoolId(schoolId)
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherResponseDto> getTeachersByStatus(
            String status) {

        return teacherRepository.findByStatus(status)
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    public TeacherResponseDto updateTeacher(
            Long id,
            TeacherRequestDto request) {

        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Teacher not found with id: " + id
                        ));

        /*
         * User can be changed if required.
         */
        if (request.getUserId() != null
                && !request.getUserId().equals(
                teacher.getUser() != null
                        ? teacher.getUser().getId()
                        : null)) {

            User user = userRepository.findById(
                            request.getUserId()
                    )
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "User not found with id: "
                                            + request.getUserId()
                            ));

            if (user.getRole() == null
                    || !"TEACHER".equalsIgnoreCase(
                    user.getRole().getName())) {

                throw new IllegalArgumentException(
                        "Selected user is not a TEACHER"
                );
            }

            if (teacherRepository.existsByUserId(user.getId())) {

                Teacher existing =
                        teacherRepository.findByUserId(user.getId())
                                .orElse(null);

                if (existing != null
                        && !existing.getId().equals(id)) {

                    throw new IllegalArgumentException(
                            "Selected user already has a teacher profile"
                    );
                }
            }

            teacher.setUser(user);
            teacher.setName(user.getUsername());
            teacher.setEmail(user.getEmail());
        }

        /*
         * Employee ID remains unchanged.
         */
        if (request.getSchoolId() != null) {

            School school = schoolRepository.findById(
                            request.getSchoolId()
                    )
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "School not found with id: "
                                            + request.getSchoolId()
                            ));

            teacher.setSchool(school);
        }

        /*
         * Teacher-specific details
         */
        teacher.setGender(request.getGender());
        teacher.setPhone(request.getPhone());
        teacher.setJoiningDate(request.getJoiningDate());
        teacher.setQualification(request.getQualification());
        teacher.setDesignation(request.getDesignation());
        teacher.setAddress(request.getAddress());

        if (request.getStatus() != null) {
            teacher.setStatus(request.getStatus());
        }

        Teacher updatedTeacher =
                teacherRepository.save(teacher);

        return mapToResponseDto(updatedTeacher);
    }

    @Override
    public void deleteTeacher(Long id) {

        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Teacher not found with id: " + id
                        ));

        teacherRepository.delete(teacher);
    }

    private TeacherResponseDto mapToResponseDto(
            Teacher teacher) {

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

    @Override
    public TeacherResponseDto getTeacherByUserId(Long userId) {

        Teacher teacher = teacherRepository.findByUserId(userId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Teacher not found for user ID: " + userId
                        )
                );

        return mapToResponseDto(teacher);
    }
}