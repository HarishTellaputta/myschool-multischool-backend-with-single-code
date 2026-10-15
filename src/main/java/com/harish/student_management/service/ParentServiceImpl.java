package com.harish.student_management.service;

import com.harish.student_management.dto.ParentRequestDto;
import com.harish.student_management.dto.ParentResponseDto;
import com.harish.student_management.dto.StudentResponseDto;
import com.harish.student_management.entity.Parent;
import com.harish.student_management.entity.Role;
import com.harish.student_management.entity.Student;
import com.harish.student_management.entity.User;
import com.harish.student_management.exception.ResourceNotFoundException;
import com.harish.student_management.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ParentServiceImpl implements ParentService {

    private static final Logger logger =
            LoggerFactory.getLogger(ParentServiceImpl.class);

    private final ParentRepository parentRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public ParentResponseDto createParent(ParentRequestDto request) {

        logger.info("Creating parent record");

        User user = createParentUser(
                request.getContactPhone(),
                request.getContactEmail()
        );

        Parent parent = Parent.builder()
                .fatherName(request.getFatherName())
                .motherName(request.getMotherName())
                .guardianName(request.getGuardianName())
                .contactPhone(request.getContactPhone())
                .email(request.getContactEmail())
                .relationship(request.getRelationship())
                .address(request.getAddress())
                .user(user)
                .build();

        Parent savedParent = parentRepository.save(parent);

        logger.info(
                "Parent record created successfully with ID: {}",
                savedParent.getId()
        );

        return mapToResponseDto(savedParent);
    }

    @Override
    @Transactional(readOnly = true)
    public ParentResponseDto getParentById(Long id) {

        logger.info("Fetching parent with ID: {}", id);

        Parent parent = parentRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Parent not found with id: " + id
                        ));

        return mapToResponseDto(parent);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParentResponseDto> getAllParents() {

        logger.info("Fetching all parents");

        return parentRepository.findAll()
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    public ParentResponseDto updateParent(
            Long id,
            ParentRequestDto request) {

        logger.info("Updating parent with ID: {}", id);

        Parent parent = parentRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Parent not found with id: " + id
                        ));

        parent.setFatherName(request.getFatherName());
        parent.setMotherName(request.getMotherName());
        parent.setGuardianName(request.getGuardianName());
        parent.setContactPhone(request.getContactPhone());
        parent.setEmail(request.getContactEmail());
        parent.setRelationship(request.getRelationship());
        parent.setAddress(request.getAddress());

        Parent updatedParent = parentRepository.save(parent);

        logger.info(
                "Parent updated successfully with ID: {}",
                updatedParent.getId()
        );

        return mapToResponseDto(updatedParent);
    }

    @Override
    public void deleteParent(Long id) {

        logger.info("Deleting parent with ID: {}", id);

        Parent parent = parentRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Parent not found with id: " + id
                        ));

        /*
         * Before deleting parent,
         * remove parent relation from all students.
         */
        List<Student> students =
                studentRepository.findByParentId(id);

        for (Student student : students) {
            student.setParent(null);
        }

        studentRepository.saveAll(students);

        parentRepository.delete(parent);

        logger.info(
                "Parent deleted successfully with ID: {}",
                id
        );
    }

    @Override
    public ParentResponseDto linkStudent(
            Long parentId,
            Long studentId) {

        logger.info(
                "Linking student {} to parent {}",
                studentId,
                parentId
        );

        Parent parent = parentRepository.findById(parentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Parent not found with id: " + parentId
                        ));

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Student not found with id: " + studentId
                        ));

        /*
         * Don't allow a student to have two parents
         * using the current Student -> Parent relationship.
         */
        if (student.getParent() != null
                && !student.getParent().getId().equals(parentId)) {

            throw new IllegalArgumentException(
                    "Student is already linked to another parent"
            );
        }

        student.setParent(parent);

        studentRepository.save(student);

        logger.info(
                "Student {} linked successfully to parent {}",
                studentId,
                parentId
        );

        return mapToResponseDto(parent);
    }

    @Override
    public ParentResponseDto unlinkStudent(
            Long parentId,
            Long studentId) {

        logger.info(
                "Unlinking student {} from parent {}",
                studentId,
                parentId
        );

        Parent parent = parentRepository.findById(parentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Parent not found with id: " + parentId
                        ));

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Student not found with id: " + studentId
                        ));

        if (student.getParent() == null
                || !student.getParent().getId().equals(parentId)) {

            throw new IllegalArgumentException(
                    "Student is not linked to this parent"
            );
        }

        student.setParent(null);

        studentRepository.save(student);

        logger.info(
                "Student {} unlinked successfully from parent {}",
                studentId,
                parentId
        );

        return mapToResponseDto(parent);
    }

    private ParentResponseDto mapToResponseDto(Parent parent) {

        List<Student> students =
                studentRepository.findByParentId(parent.getId());

        List<StudentResponseDto> studentDtos =
                students.stream()
                        .map(this::mapStudentToResponseDto)
                        .toList();

        return ParentResponseDto.builder()
                .id(parent.getId())
                .fatherName(parent.getFatherName())
                .motherName(parent.getMotherName())
                .guardianName(parent.getGuardianName())
                .contactPhone(parent.getContactPhone())
                .contactEmail(parent.getEmail())
                .relationship(parent.getRelationship())
                .address(parent.getAddress())
                .createdAt(parent.getCreatedAt())
                .updatedAt(parent.getUpdatedAt())
                .students(studentDtos)
                .build();
    }

 
    private StudentResponseDto mapStudentToResponseDto(
            Student student) {

        return StudentResponseDto.builder()
                .id(student.getId())
                .admissionNo(student.getAdmissionNo())
                .name(student.getName())
                .email(student.getEmail())
                .phone(student.getPhone())
                .dateOfBirth(student.getDateOfBirth())
                .gender(student.getGender())
                .bloodGroup(student.getBloodGroup())
                .admissionDate(student.getAdmissionDate())

                // =====================================================
                // CLASS
                // =====================================================
                .classId(
                        student.getSection() != null
                                && student.getSection().getClassEntity() != null
                                ? student.getSection().getClassEntity().getId()
                                : null
                )
                .className(
                        student.getSection() != null
                                && student.getSection().getClassEntity() != null
                                ? student.getSection().getClassEntity().getName()
                                : null
                )

                // =====================================================
                // SECTION
                // =====================================================
                .sectionId(
                        student.getSection() != null
                                ? student.getSection().getId()
                                : null
                )
                .sectionName(
                        student.getSection() != null
                                ? student.getSection().getName()
                                : null
                )

                .parentId(
                        student.getParent() != null
                                ? student.getParent().getId()
                                : null
                )
                .parentName(
                        student.getParent() != null
                                ? student.getParent().getFatherName()
                                : null
                )
                .academicYearId(
                        student.getAcademicYear() != null
                                ? student.getAcademicYear().getId()
                                : null
                )
                .academicYearName(
                        student.getAcademicYear() != null
                                ? student.getAcademicYear().getName()
                                : null
                )
                .status(student.getStatus())
                .createdAt(student.getCreatedAt())
                .updatedAt(student.getUpdatedAt())
                .build();
    }


    @Override
    @Transactional(readOnly = true)
    public ParentResponseDto getMyProfile(String username) {

        logger.info(
                "Fetching logged-in parent profile for username: {}",
                username
        );

        Parent parent = parentRepository.findByUserUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Parent profile not found for username: " + username
                        ));

        return mapToResponseDto(parent);
    }


    private User createParentUser(
            String phone,
            String email) {

        String username = phone;

        Optional<User> existingByUsername =
                userRepository.findByUsername(username);

        if (existingByUsername.isPresent()) {

            User existingUser = existingByUsername.get();

            if (existingUser.getRole() != null
                    && "PARENT".equalsIgnoreCase(
                    existingUser.getRole().getName())) {

                return existingUser;
            }

            throw new IllegalArgumentException(
                    "Username already belongs to another user: " + username);
        }

        if (email != null && !email.isBlank()) {

            Optional<User> existingByEmail =
                    userRepository.findByEmail(email);

            if (existingByEmail.isPresent()) {

                User existingUser = existingByEmail.get();

                if (existingUser.getRole() != null
                        && "PARENT".equalsIgnoreCase(
                        existingUser.getRole().getName())) {

                    return existingUser;
                }

                throw new IllegalArgumentException(
                        "Email already belongs to another user: " + email);
            }
        }

        Role parentRole = roleRepository.findByName("PARENT")
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "PARENT role not found"));

        String temporaryPassword = "Parent@123";

        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(temporaryPassword))
                .role(parentRole)
                .status("ACTIVE")
                .build();

        return userRepository.save(user);
    }
}