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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

        User currentUser = getCurrentUser();

        if (currentUser.getSchool() == null) {
            throw new IllegalArgumentException(
                    "Logged-in user is not assigned to a school"
            );
        }

        logger.info("Creating parent record");

        // Prevent duplicate parent profiles for the same contact details.
        if (parentRepository.findByContactPhone(request.getContactPhone()).isPresent()) {
            throw new IllegalArgumentException(
                    "A parent profile already exists with this phone number. "
                            + "Please update the existing profile."
            );
        }

        if (request.getContactEmail() != null
                && !request.getContactEmail().isBlank()
                && parentRepository.findByEmail(request.getContactEmail()).isPresent()) {

            throw new IllegalArgumentException(
                    "A parent profile already exists with this email. "
                            + "Please update the existing profile."
            );
        }

        // Reuse an existing teacher/user account when its phone or email matches.
        User user = createParentUser(
                request.getContactPhone(),
                request.getContactEmail(),
                currentUser
        );

        // One user can be linked to only one Parent profile.
        if (parentRepository.findByUserId(user.getId()).isPresent()) {
            throw new IllegalArgumentException(
                    "A parent profile is already linked to this user. "
                            + "Please update the existing profile."
            );
        }

        Parent parent = Parent.builder()
                .fatherName(request.getFatherName())
                .surname(request.getSurname())
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

        Parent parent = parentRepository
                .findByIdAndUser_School_Id(id, getCurrentSchoolId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Parent not found with id: " + id
                        )
                );

        return mapToResponseDto(parent);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParentResponseDto> getAllParents() {
        Long schoolId = getCurrentSchoolId();

        return parentRepository
                .findAllByUser_School_Id(schoolId)
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    public ParentResponseDto updateParent(
            Long id,
            ParentRequestDto request) {

        logger.info("Updating parent with ID: {}", id);



        Parent parent = parentRepository
                .findByIdAndUser_School_Id(id, getCurrentSchoolId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Parent not found with id: " + id
                        ));

        String requestedPhone = request.getContactPhone();

        if (requestedPhone != null
                && !requestedPhone.equals(parent.getContactPhone())) {

            Optional<Parent> existingParent =
                    parentRepository.findByContactPhone(requestedPhone);

            if (existingParent.isPresent()
                    && !existingParent.get().getId().equals(parent.getId())) {
                throw new IllegalArgumentException(
                        "A parent profile already exists with this phone number"
                );
            }
        }

        parent.setFatherName(request.getFatherName());
        if (request.getSurname() != null) {
            parent.setSurname(request.getSurname());
        }
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

        Parent parent = parentRepository
                .findByIdAndUser_School_Id(id, getCurrentSchoolId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Parent not found with id: " + id
                        ));


        /*
         * Before deleting parent,
         * remove parent relation from all students.
         */
        Long schoolId = getCurrentSchoolId();

        List<Student> students =
                studentRepository.findByParentId(id);

        for (Student student : students) {

            if (student.getSection() == null
                    || student.getSection().getClassEntity() == null
                    || student.getSection().getClassEntity().getSchool() == null
                    || !schoolId.equals(
                    student.getSection()
                            .getClassEntity()
                            .getSchool()
                            .getId())) {

                throw new IllegalStateException(
                        "Parent has a student linked outside your school. "
                                + "Resolve the relationship before deleting."
                );
            }

            student.setParent(null);
        }

        studentRepository.saveAll(students);
        parentRepository.delete(parent);

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

        Parent parent = parentRepository
                .findByIdAndUser_School_Id(parentId, getCurrentSchoolId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Parent not found with id: " + parentId
                        )
                );

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Student not found with id: " + studentId
                        ));

        Long schoolId = getCurrentSchoolId();

        if (student.getSection() == null
                || student.getSection().getClassEntity() == null
                || student.getSection().getClassEntity().getSchool() == null
                || !schoolId.equals(
                student.getSection()
                        .getClassEntity()
                        .getSchool()
                        .getId())) {
            throw new IllegalArgumentException(
                    "Student does not belong to your school"
            );
        }
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

        Long schoolId = getCurrentSchoolId();

        // Only allow parents belonging to the logged-in user's school
        Parent parent = parentRepository
                .findByIdAndUser_School_Id(parentId, schoolId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Parent not found with id: " + parentId
                        )
                );

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Student not found with id: " + studentId
                        )
                );

        // Verify that the student belongs to the same school
        if (student.getSection() == null
                || student.getSection().getClassEntity() == null
                || student.getSection().getClassEntity().getSchool() == null
                || !schoolId.equals(
                student.getSection()
                        .getClassEntity()
                        .getSchool()
                        .getId())) {

            throw new IllegalArgumentException(
                    "Student does not belong to your school"
            );
        }

        // Verify that this student is linked to the specified parent
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

        Long schoolId = parent.getUser() != null
                && parent.getUser().getSchool() != null
                ? parent.getUser().getSchool().getId()
                : null;

        if (schoolId == null) {
            throw new IllegalStateException(
                    "Parent user is not assigned to a school"
            );
        }

        List<Student> students =
                studentRepository
                        .findByParent_IdAndSection_ClassEntity_School_Id(
                                parent.getId(),
                                schoolId
                        );

        List<StudentResponseDto> studentDtos =
                students.stream()
                        .map(this::mapStudentToResponseDto)
                        .toList();

        return ParentResponseDto.builder()
                .id(parent.getId())
                .fatherName(parent.getFatherName())
                .surname(parent.getSurname())
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
                .surname(student.getSurname())
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

        User currentUser = getCurrentUser();

        if (!currentUser.getUsername().equals(username)) {
            throw new IllegalArgumentException(
                    "You cannot access another user's parent profile"
            );
        }

        Parent parent = parentRepository.findByUserId(currentUser.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Parent profile not found"
                        ));

        return mapToResponseDto(parent);
    }


    private User createParentUser(String phone, String email, User currentUser) {

        Role parentRole = roleRepository.findByName("PARENT")
                .orElseThrow(() ->
                        new IllegalArgumentException("PARENT role not found")
                );

        Optional<User> userByPhone =
                userRepository.findByUsername(phone);

        Optional<User> userByEmail = Optional.empty();

        if (email != null && !email.isBlank()) {
            userByEmail = userRepository.findByEmail(email);
        }

        // Phone and email must not identify two different user accounts.
        if (userByPhone.isPresent()
                && userByEmail.isPresent()
                && !userByPhone.get().getId()
                .equals(userByEmail.get().getId())) {

            throw new IllegalArgumentException(
                    "The phone number and email belong to different user accounts. "
                            + "Please verify the parent details."
            );
        }

        User existingUser = userByPhone.orElse(null);

        if (existingUser == null) {
            existingUser = userByEmail.orElse(null);
        }

        // Reuse an existing account, including a Teacher account.
        if (existingUser != null) {
            if (existingUser.getSchool() == null
                    || !existingUser.getSchool().getId()
                    .equals(currentUser.getSchool().getId())) {
                throw new IllegalArgumentException(
                        "User belongs to another school or has no school assigned"
                );
            }

            boolean alreadyHasParentRole =
                    existingUser.getAllRoleNames()
                            .stream()
                            .anyMatch(role ->
                                    "PARENT".equalsIgnoreCase(role)
                            );

            if (existingUser.getRole() == null) {
                existingUser.setRole(parentRole);
            } else if (!alreadyHasParentRole) {
                // Preserve the primary role and add PARENT as an additional role.
                existingUser.addAdditionalRole(parentRole);
            }

            return userRepository.save(existingUser);
        }

        // Create a new account only when no matching user exists.
        String temporaryPassword = "Parent@123";

        User user = User.builder()
                .username(phone)
                .email(email)
                .password(passwordEncoder.encode(temporaryPassword))
                .role(parentRole)
                .school(currentUser.getSchool())
                .status("ACTIVE")
                .build();

        return userRepository.save(user);
    }

    private User getCurrentUser() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            throw new RuntimeException("User is not authenticated");
        }

        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() ->
                        new RuntimeException("Authenticated user not found"));
    }

    private Long getCurrentSchoolId() {
        User user = getCurrentUser();

        if (user.getSchool() == null) {
            throw new RuntimeException(
                    "Logged-in user is not assigned to a school");
        }

        return user.getSchool().getId();
    }
}