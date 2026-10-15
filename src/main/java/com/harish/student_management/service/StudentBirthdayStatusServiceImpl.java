package com.harish.student_management.service;

import com.harish.student_management.dto.StudentBirthdayChatResponse;
import com.harish.student_management.dto.StudentBirthdayStatusRequest;
import com.harish.student_management.dto.StudentBirthdayStatusResponse;
import com.harish.student_management.dto.StudentBirthdayStatusUpdateRequest;
import com.harish.student_management.entity.Parent;
import com.harish.student_management.entity.Student;
import com.harish.student_management.entity.StudentBirthdayStatus;
import com.harish.student_management.entity.User;
import com.harish.student_management.exception.ResourceNotFoundException;
import com.harish.student_management.repository.ParentRepository;
import com.harish.student_management.repository.StudentBirthdayStatusRepository;
import com.harish.student_management.repository.StudentRepository;
import com.harish.student_management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class StudentBirthdayStatusServiceImpl
        implements StudentBirthdayStatusService {

    private final StudentBirthdayStatusRepository birthdayStatusRepository;
    private final StudentRepository studentRepository;
    private final ParentRepository parentRepository;
    private final UserRepository userRepository;

    // =========================================================
    // CREATE
    // =========================================================

    @Override
    public StudentBirthdayStatusResponse create(
            String username,
            StudentBirthdayStatusRequest request
    ) {

        Parent parent = getParentFromUsername(username);

        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Student not found with ID: "
                                        + request.getStudentId()
                        )
                );

        validateStudentBelongsToParent(student, parent);

        boolean exists =
                birthdayStatusRepository.existsByStudentIdAndBirthdayDate(
                        student.getId(),
                        request.getBirthdayDate()
                );

        if (exists) {
            throw new IllegalArgumentException(
                    "Birthday status already exists for this student and date"
            );
        }

        StudentBirthdayStatus birthdayStatus =
                StudentBirthdayStatus.builder()
                        .student(student)
                        .birthdayDate(request.getBirthdayDate())
                        .photoUrl(normalizePhotoUrl(request.getPhotoUrl()))
                        .build();

        try {

            StudentBirthdayStatus saved =
                    birthdayStatusRepository.save(birthdayStatus);

            return toResponse(saved);

        } catch (DataIntegrityViolationException ex) {

            throw new IllegalArgumentException(
                    "Birthday status already exists for this student and date"
            );
        }
    }

    // =========================================================
    // GET BY ID
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public StudentBirthdayStatusResponse getById(
            String username,
            Long id
    ) {

        Parent parent = getParentFromUsername(username);

        StudentBirthdayStatus birthdayStatus =
                birthdayStatusRepository.findByIdAndParentId(
                                id,
                                parent.getId()
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Birthday status not found"
                                )
                        );

        return toResponse(birthdayStatus);
    }

    // =========================================================
    // GET ALL
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<StudentBirthdayStatusResponse> getAll(
            String username
    ) {

        Parent parent = getParentFromUsername(username);

        return birthdayStatusRepository
                .findAllByParentId(parent.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // =========================================================
    // GET TODAY
    // =========================================================

    // =========================================================
// GET TODAY
// =========================================================

    // =========================================================
// GET TODAY
// =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<StudentBirthdayStatusResponse> getToday(
            String username,
            LocalDate date
    ) {

        // Create a final/effectively-final date for lambda usage
        final LocalDate targetDate =
                date != null ? date : LocalDate.now();

        System.out.println(
                "🎂 TODAY BIRTHDAYS REQUESTED BY: " + username
        );

        System.out.println(
                "🎂 TODAY DATE: " + targetDate
        );

        // =========================================================
        // GET ALL STUDENTS WHOSE BIRTHDAY IS TODAY
        // Works for:
        // SCHOOL_ADMIN
        // TEACHER
        // PARENT
        // STUDENT
        // =========================================================

        List<Student> students =
                studentRepository.findSchoolStudentsHavingBirthdayToday(
                        targetDate.getMonthValue(),
                        targetDate.getDayOfMonth()
                );

        System.out.println(
                "🎂 TODAY BIRTHDAY STUDENTS COUNT: "
                        + students.size()
        );

        for (Student student : students) {

            System.out.println(
                    "🎂 Birthday Student: "
                            + student.getId()
                            + " | "
                            + student.getName()
                            + " | DOB: "
                            + student.getDateOfBirth()
            );
        }

        // =========================================================
        // BUILD RESPONSE
        // =========================================================

        return students.stream()
                .map(student -> {

                    LocalDate birthdayDate = LocalDate.of(
                            targetDate.getYear(),
                            student.getDateOfBirth().getMonth(),
                            student.getDateOfBirth().getDayOfMonth()
                    );

                    Optional<StudentBirthdayStatus> status =
                            birthdayStatusRepository
                                    .findByStudentIdAndBirthdayDate(
                                            student.getId(),
                                            birthdayDate
                                    );

                    return StudentBirthdayStatusResponse.builder()
                            .id(
                                    status.map(
                                            StudentBirthdayStatus::getId
                                    ).orElse(null)
                            )
                            .studentId(student.getId())
                            .studentName(student.getName())
                            .admissionNo(student.getAdmissionNo())
                            .dateOfBirth(student.getDateOfBirth())
                            .birthdayDate(birthdayDate)
                            .photoUrl(
                                    status.map(
                                            StudentBirthdayStatus::getPhotoUrl
                                    ).orElse(null)
                            )
                            .createdAt(
                                    status.map(
                                            StudentBirthdayStatus::getCreatedAt
                                    ).orElse(null)
                            )
                            .build();
                })
                .toList();
    }

    // =========================================================
    // GET BY STUDENT
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<StudentBirthdayStatusResponse> getByStudent(
            String username,
            Long studentId
    ) {

        Parent parent = getParentFromUsername(username);

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Student not found with ID: " + studentId
                        )
                );

        validateStudentBelongsToParent(student, parent);

        return birthdayStatusRepository
                .findByStudentIdOrderByBirthdayDateDesc(studentId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // =========================================================
    // UPDATE
    // =========================================================

    @Override
    public StudentBirthdayStatusResponse update(
            String username,
            Long id,
            StudentBirthdayStatusUpdateRequest request
    ) {

        Parent parent = getParentFromUsername(username);

        StudentBirthdayStatus birthdayStatus =
                birthdayStatusRepository.findByIdAndParentId(
                                id,
                                parent.getId()
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Birthday status not found"
                                )
                        );

        LocalDate newBirthdayDate =
                request.getBirthdayDate();

        if (newBirthdayDate == null) {
            throw new IllegalArgumentException(
                    "Birthday date is required"
            );
        }

        boolean dateChanged =
                !newBirthdayDate.equals(
                        birthdayStatus.getBirthdayDate()
                );

        if (dateChanged) {

            boolean exists =
                    birthdayStatusRepository
                            .existsByStudentIdAndBirthdayDate(
                                    birthdayStatus
                                            .getStudent()
                                            .getId(),
                                    newBirthdayDate
                            );

            if (exists) {
                throw new IllegalArgumentException(
                        "Birthday status already exists for this student and date"
                );
            }
        }

        birthdayStatus.setBirthdayDate(newBirthdayDate);

        birthdayStatus.setPhotoUrl(
                normalizePhotoUrl(
                        request.getPhotoUrl()
                )
        );

        StudentBirthdayStatus updated =
                birthdayStatusRepository.save(
                        birthdayStatus
                );

        return toResponse(updated);
    }

    // =========================================================
    // DELETE
    // =========================================================

    @Override
    public void delete(
            String username,
            Long id
    ) {

        Parent parent = getParentFromUsername(username);

        StudentBirthdayStatus birthdayStatus =
                birthdayStatusRepository.findByIdAndParentId(
                                id,
                                parent.getId()
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Birthday status not found"
                                )
                        );

        birthdayStatusRepository.delete(
                birthdayStatus
        );
    }

    // =========================================================
    // FIND LOGGED-IN PARENT
    // =========================================================

    private Parent getParentFromUsername(
            String username
    ) {

        User user = userRepository
                .findByUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        )
                );

        return parentRepository
                .findByUserId(user.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Parent profile not found for user: "
                                        + username
                        )
                );
    }

    // =========================================================
    // OWNERSHIP VALIDATION
    // =========================================================

    private void validateStudentBelongsToParent(
            Student student,
            Parent parent
    ) {

        if (student.getParent() == null) {
            throw new IllegalArgumentException(
                    "Student is not linked to a parent"
            );
        }

        if (!parent.getId().equals(
                student.getParent().getId()
        )) {

            throw new SecurityException(
                    "You are not authorized to access this student"
            );
        }
    }

    // =========================================================
    // PHOTO URL
    // =========================================================

    private String normalizePhotoUrl(
            String photoUrl
    ) {

        if (photoUrl == null) {
            return null;
        }

        String trimmed =
                photoUrl.trim();

        return trimmed.isEmpty()
                ? null
                : trimmed;
    }

    // =========================================================
    // RESPONSE MAPPER
    // =========================================================

    private StudentBirthdayStatusResponse toResponse(
            StudentBirthdayStatus birthdayStatus
    ) {

        Student student =
                birthdayStatus.getStudent();

        return StudentBirthdayStatusResponse.builder()
                .id(birthdayStatus.getId())
                .studentId(student.getId())
                .studentName(student.getName())
                .admissionNo(student.getAdmissionNo())
                .dateOfBirth(student.getDateOfBirth())
                .birthdayDate(
                        birthdayStatus.getBirthdayDate()
                )
                .photoUrl(
                        birthdayStatus.getPhotoUrl()
                )
                .createdAt(
                        birthdayStatus.getCreatedAt()
                )
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentBirthdayChatResponse> getBirthdayChat(
            String username
    ) {

        LocalDate today = LocalDate.now();

        System.out.println("🎂 Birthday chat requested by: " + username);
        System.out.println("🎂 Today: " + today);

        // =========================================================
        // GET ALL STUDENTS WHOSE BIRTHDAY IS TODAY
        // This app is school-specific, so all students belong
        // to the same school's application.
        // =========================================================

        List<Student> students =
                studentRepository.findSchoolStudentsHavingBirthdayToday(
                        today.getMonthValue(),
                        today.getDayOfMonth()
                );

        System.out.println(
                "🎂 Today's birthday students count: "
                        + students.size()
        );

        for (Student student : students) {
            System.out.println(
                    "🎂 Birthday Student: "
                            + student.getId()
                            + " | "
                            + student.getName()
                            + " | DOB: "
                            + student.getDateOfBirth()
            );
        }

        // =========================================================
        // BUILD RESPONSE
        // =========================================================

        return students.stream()
                .map(student -> {

                    LocalDate birthdayDate = LocalDate.of(
                            today.getYear(),
                            student.getDateOfBirth().getMonth(),
                            student.getDateOfBirth().getDayOfMonth()
                    );

                    Optional<StudentBirthdayStatus> status =
                            birthdayStatusRepository
                                    .findByStudentIdAndBirthdayDate(
                                            student.getId(),
                                            birthdayDate
                                    );

                    boolean photoAlreadyAdded =
                            status.isPresent();

                    return StudentBirthdayChatResponse.builder()
                            .studentId(student.getId())
                            .studentName(student.getName())
                            .admissionNo(student.getAdmissionNo())
                            .dateOfBirth(student.getDateOfBirth())
                            .birthdayDate(birthdayDate)
                            .birthdayToday(true)
                            .photoAlreadyAdded(photoAlreadyAdded)
                            .birthdayStatusId(
                                    status.map(
                                            StudentBirthdayStatus::getId
                                    ).orElse(null)
                            )
                            .photoUrl(
                                    status.map(
                                            StudentBirthdayStatus::getPhotoUrl
                                    ).orElse(null)
                            )
                            .message(
                                    photoAlreadyAdded
                                            ? "Birthday status already added for "
                                            + student.getName()
                                            : "🎂 " + student.getName()
                                            + "'s birthday is today!"
                            )
                            .build();
                })
                .toList();
    }
}