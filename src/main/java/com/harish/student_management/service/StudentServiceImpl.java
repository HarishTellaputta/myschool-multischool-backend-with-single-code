package com.harish.student_management.service;

import com.harish.student_management.dto.FileUploadResponseDto;
import com.harish.student_management.dto.StudentGenderSummaryDto;
import com.harish.student_management.dto.StudentRequestDto;
import com.harish.student_management.dto.StudentResponseDto;
import com.harish.student_management.entity.Section;
import com.harish.student_management.entity.Student;
import com.harish.student_management.exception.FileUploadException;
import com.harish.student_management.exception.StudentNotFoundException;
import com.harish.student_management.mapper.StudentMapper;
import com.harish.student_management.repository.SectionRepository;
import com.harish.student_management.repository.StudentRepository;
import com.harish.student_management.entity.User;
import com.harish.student_management.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.harish.student_management.entity.Parent;
import com.harish.student_management.entity.AcademicYear;
import com.harish.student_management.repository.ParentRepository;
import com.harish.student_management.repository.AcademicYearRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor
@Service
public class StudentServiceImpl implements StudentService {

    private static final Logger logger =
            LoggerFactory.getLogger(StudentServiceImpl.class);

    private final StudentRepository repository;
    private final SectionRepository sectionRepository;
    private final StudentMapper mapper;
    private final UserRepository userRepository;

    private final ParentRepository parentRepository;
    private final AcademicYearRepository academicYearRepository;


    // ============================================================
    // SAVE STUDENT
    // ============================================================

    @Override
    @Transactional
    public StudentResponseDto saveStudent(StudentRequestDto student) {

        logger.info("Starting student creation");

        /*
         * ============================================================
         * 1. SECTION IS REQUIRED
         * ============================================================
         *
         * Student
         *   -> Section
         *      -> Class
         *         -> School
         *
         * Section is required because:
         *
         * - School is needed for admission number
         * - Class is needed for roll number
         */
        if (student.getSectionId() == null) {
            throw new IllegalArgumentException(
                    "Section is required to generate admission and roll number."
            );
        }

        /*
         * ============================================================
         * 2. RESOLVE SELECTED SECTION
         * ============================================================
         */
        if (student.getSectionId() == null) {
            throw new IllegalArgumentException(
                    "Section is required"
            );
        }
        Section section =
                sectionRepository
                        .findByIdWithClassAndSchool(student.getSectionId())
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Selected section was not found."
                                )
                        );

        // Validate that the selected section belongs to the logged-in school
        Long currentSchoolId = getCurrentSchoolId();

        if (section.getClassEntity() == null
                || section.getClassEntity().getSchool() == null
                || section.getClassEntity().getSchool().getId() == null
                || !currentSchoolId.equals(
                section.getClassEntity().getSchool().getId())) {

            throw new org.springframework.security.access.AccessDeniedException(
                    "Selected section does not belong to your school"
            );
        }

        /*
         * ============================================================
         * 3. VALIDATE CLASS
         * ============================================================
         *
         * Roll number is generated based on Class.
         */
        if (section.getClassEntity() == null
                || section.getClassEntity().getId() == null) {

            throw new IllegalArgumentException(
                    "Unable to determine class from selected section."
            );
        }

        Long classId =
                section.getClassEntity().getId();

        /*
         * ============================================================
         * 4. VALIDATE SCHOOL
         * ============================================================
         *
         * School is only required for admission number generation.
         */
        if (section.getClassEntity().getSchool() == null
                || section.getClassEntity().getSchool().getId() == null) {

            throw new IllegalArgumentException(
                    "Unable to determine school from selected section."
            );
        }

        Long schoolId =
                section.getClassEntity().getSchool().getId();

        /*
         * ============================================================
         * 5. GENERATE ADMISSION NUMBER
         * ============================================================
         *
         * Admission number is generated school-wise.
         *
         * Example:
         *
         * Existing:
         * ADM001
         * ADM002
         * ADM003
         *
         * New:
         * ADM004
         *
         * Frontend admission number is completely ignored.
         */
        String generatedAdmissionNo =
                generateAdmissionNo(schoolId);

        logger.info(
                "Generated admission number {} for school {}",
                generatedAdmissionNo,
                schoolId
        );

        /*
         * ============================================================
         * 6. GENERATE ROLL NUMBER
         * ============================================================
         *
         * Roll number is generated CLASS-WISE.
         *
         * Section is NOT used for roll-number sequence.
         *
         * Example:
         *
         * Class 5:
         *
         * Section A -> 1, 2, 3
         * Section B -> 4, 5, 6
         * Section C -> 7, 8
         *
         * New Class 5 student -> 9
         *
         * Class 6 has its own sequence:
         *
         * Section A -> 1, 2
         *
         * New Class 6 student -> 3
         *
         * Frontend roll number is completely ignored.
         */
        String generatedRollNumber =
                generateRollNumber(classId);

        logger.info(
                "Generated roll number {} for class {}",
                generatedRollNumber,
                classId
        );

        /*
         * ============================================================
         * 7. CONVERT DTO -> ENTITY
         * ============================================================
         */
        Student studentEntity =
                mapper.toEntity(student);


        if (student.getParentId() != null) {
            Parent parent = parentRepository
                    .findByIdAndUser_School_Id(
                            student.getParentId(),
                            currentSchoolId
                    )
                    .orElseThrow(() ->
                            new org.springframework.security.access.AccessDeniedException(
                                    "Selected parent does not belong to your school"
                            )
                    );

            studentEntity.setParent(parent);
        }

        if (student.getAcademicYearId() != null) {
            AcademicYear academicYear = academicYearRepository
                    .findByIdAndSchool_Id(
                            student.getAcademicYearId(),
                            currentSchoolId
                    )
                    .orElseThrow(() ->
                            new org.springframework.security.access.AccessDeniedException(
                                    "Selected academic year does not belong to your school"
                            )
                    );

            studentEntity.setAcademicYear(academicYear);
        }


        /*
         * ============================================================
         * 8. BACKEND AUTHORITATIVE VALUES
         * ============================================================
         *
         * Even if frontend sends:
         *
         * admissionNo = "ADM999"
         * rollNumber  = "999"
         *
         * those values are ignored.
         *
         * Backend-generated values are always stored.
         */
        studentEntity.setAdmissionNo(generatedAdmissionNo);

        studentEntity.setRollNumber(generatedRollNumber);

        /*
         * ============================================================
         * 9. SET SELECTED SECTION
         * ============================================================
         *
         * Make sure the student is saved under the section
         * selected by the frontend.
         */
        studentEntity.setSection(section);

        /*
         * ============================================================
         * 10. SAVE STUDENT
         * ============================================================
         */
        Student savedStudent =
                repository.save(studentEntity);

        /*
         * ============================================================
         * 11. LOG SAVED DATA
         * ============================================================
         */
        logger.info(
                "Student saved successfully. " +
                        "ID: {}, admissionNo: {}, rollNumber: {}, classId: {}, schoolId: {}",
                savedStudent.getId(),
                savedStudent.getAdmissionNo(),
                savedStudent.getRollNumber(),
                classId,
                schoolId
        );

        /*
         * ============================================================
         * 12. RETURN RESPONSE
         * ============================================================
         */
        return mapper.toResponseDto(savedStudent);
    }
    // ============================================================
    // GENERATE ADMISSION NUMBER
    // ============================================================

    /**
     * Generates the next admission number for a particular school.
     *
     * Examples:
     *
     * ADM001  -> ADM002
     * ADM099  -> ADM100
     * 1001    -> 1002
     * 2026001 -> 2026002
     * STU-009 -> STU-010
     *
     * The latest valid admission number of the school determines
     * the format.
     */
    private String generateAdmissionNo(Long schoolId) {

        List<String> admissionNumbers =
                repository.findAdmissionNumbersBySchoolOrderByLatest(schoolId);

        /*
         * Pattern:
         *
         * Group 1 = prefix
         * Group 2 = trailing number
         *
         * Examples:
         *
         * ADM001
         * ^(.*?)  -> ADM
         * (\\d+)$ -> 001
         *
         * STU-009 -> STU- + 009
         * 2026001 -> empty prefix + 2026001
         */
        Pattern pattern =
                Pattern.compile("^(.*?)(\\d+)$");

        /*
         * Newest student first.
         *
         * If newest admission number is invalid, for example:
         *
         * TEMP
         *
         * we continue searching older records until we find
         * a valid numeric-ending admission number.
         */
        for (String admissionNo : admissionNumbers) {

            if (admissionNo == null
                    || admissionNo.trim().isEmpty()) {
                continue;
            }

            String value = admissionNo.trim();

            Matcher matcher = pattern.matcher(value);

            if (!matcher.matches()) {
                continue;
            }

            String prefix = matcher.group(1);
            String numericPart = matcher.group(2);

            try {

                /*
                 * BigInteger avoids overflow if the numeric portion
                 * becomes very large.
                 */
                BigInteger current =
                        new BigInteger(numericPart);

                BigInteger next =
                        current.add(BigInteger.ONE);

                /*
                 * Preserve leading zeroes.
                 *
                 * 009 -> 010
                 * 099 -> 100
                 * 0009 -> 0010
                 */
                int originalWidth =
                        numericPart.length();

                String nextNumber =
                        next.toString();

                if (nextNumber.length() < originalWidth) {

                    nextNumber =
                            "0".repeat(
                                    originalWidth
                                            - nextNumber.length()
                            )
                                    + nextNumber;
                }

                String generated =
                        prefix + nextNumber;

                /*
                 * Do not reuse an already existing admission number.
                 *
                 * This also protects against old/deleted-number
                 * situations if another record already contains
                 * the generated value.
                 */
                while (repository.existsByAdmissionNoAndSection_ClassEntity_School_Id(
                        generated, schoolId)) {

                    next = next.add(BigInteger.ONE);

                    nextNumber = next.toString();

                    if (nextNumber.length() < originalWidth) {

                        nextNumber =
                                "0".repeat(
                                        originalWidth
                                                - nextNumber.length()
                                )
                                        + nextNumber;
                    }

                    generated =
                            prefix + nextNumber;
                }

                return generated;

            } catch (NumberFormatException ex) {

                /*
                 * Invalid numeric part.
                 *
                 * Continue checking older admission numbers.
                 */
                logger.warn(
                        "Invalid admission number format found: {}",
                        admissionNo
                );
            }
        }

        /*
         * No valid existing format was found.
         *
         * We intentionally do NOT create something like ADM001
         * automatically because the requirement is to continue
         * the school's existing admission-number format.
         */
        throw new IllegalStateException(
                "Unable to generate admission number automatically. "
                        + "No valid existing admission number format "
                        + "found for this school."
        );
    }


    // ============================================================
    // GET ALL STUDENTS
    // ============================================================

    @Override
    public Page<StudentResponseDto> getAllStudents(
            int page,
            int size,
            String sortBy,
            String sortDirectionS) {

        logger.info(
                "Before Fetching all students page {}, size {}, sortBy {}, sortDirectionS {}",
                page,
                size,
                sortBy,
                sortDirectionS
        );

        Sort sort =
                sortDirectionS.equalsIgnoreCase("asc")
                        ? Sort.by(sortBy).ascending()
                        : Sort.by(sortBy).descending();

        Pageable pageable =
                PageRequest.of(page, size, sort);

        Long schoolId = getCurrentSchoolId();

        Page<Student> students =
                repository.findAllBySection_ClassEntity_School_Id(
                        schoolId,
                        pageable
                );

        return students.map(mapper::toResponseDto);
    }


    // ============================================================
    // GET STUDENT BY ID
    // ============================================================

    @Override
    public StudentResponseDto getStudentById(Long id) {

        logger.info("Student {}", id);

        Long schoolId = getCurrentSchoolId();

        Student student =
                repository.findByIdAndSection_ClassEntity_School_Id(
                                id,
                                schoolId
                        )
                        .orElseThrow(() ->
                                new StudentNotFoundException(
                                        "Student not found with id : " + id
                                )
                        );

        logger.info(
                "Successfully fetched Student details by id {}",
                id
        );

        return mapper.toResponseDto(student);
    }


    // ============================================================
    // UPDATE STUDENT
    // ============================================================

    @Override
    public StudentResponseDto updateStudent(
            Long id,
            StudentRequestDto student) {

        logger.info(
                "Before updating Student by {}",
                id
        );

        Long schoolId = getCurrentSchoolId();

        Student dbStudent =
                repository.findByIdAndSection_ClassEntity_School_Id(
                                id,
                                schoolId
                        )
                        .orElseThrow(() ->
                                new StudentNotFoundException(
                                        "Student not found with id : " + id
                                )
                        );

        logger.info(
                "Successfully fetched Student details by id {}",
                id
        );

        /*
         * ========================================================
         * IMPORTANT
         * ========================================================
         *
         * Admission number is IMMUTABLE.
         *
         * We intentionally DO NOT execute:
         *
         * dbStudent.setAdmissionNo(...)
         *
         * here.
         *
         * Even if frontend sends a different admissionNo,
         * it will be completely ignored during update.
         */

        // Basic information
        dbStudent.setRollNumber(student.getRollNumber());
        dbStudent.setName(student.getName());
        if (student.getSurname() != null) {
            dbStudent.setSurname(student.getSurname());
        }
        dbStudent.setEmail(student.getEmail());
        dbStudent.setPhone(student.getPhone());

        // Personal information
        dbStudent.setDateOfBirth(student.getDateOfBirth());
        dbStudent.setGender(student.getGender());
        dbStudent.setBloodGroup(student.getBloodGroup());

        // Academic information
        dbStudent.setAdmissionDate(student.getAdmissionDate());

        // Address
        dbStudent.setAddress(student.getAddress());

        logger.info(
                "========== STUDENT UPDATE RELATIONSHIPS =========="
        );

        logger.info(
                "Section ID received: {}",
                student.getSectionId()
        );

        logger.info(
                "Parent ID received: {}",
                student.getParentId()
        );

        logger.info(
                "Academic Year ID received: {}",
                student.getAcademicYearId()
        );

        if (student.getSectionId() == null) {
            throw new IllegalArgumentException("Section is required");
        }

        Section section = sectionRepository
                .findByIdWithClassAndSchool(student.getSectionId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Section not found"
                        )
                );

        if (section.getClassEntity() == null
                || section.getClassEntity().getSchool() == null
                || !schoolId.equals(
                section.getClassEntity().getSchool().getId()
        )) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Selected section does not belong to your school"
            );
        }

        dbStudent.setSection(section);



        Parent parent = null;

        if (student.getParentId() != null) {
            parent = parentRepository
                    .findByIdAndUser_School_Id(
                            student.getParentId(),
                            schoolId
                    )
                    .orElseThrow(() ->
                            new org.springframework.security.access.AccessDeniedException(
                                    "Selected parent does not belong to your school"
                            )
                    );
        }

        dbStudent.setParent(parent);

        AcademicYear academicYear = null;

        if (student.getAcademicYearId() != null) {
            academicYear = academicYearRepository
                    .findByIdAndSchool_Id(
                            student.getAcademicYearId(),
                            schoolId
                    )
                    .orElseThrow(() ->
                            new org.springframework.security.access.AccessDeniedException(
                                    "Selected academic year does not belong to your school"
                            )
                    );
        }

        dbStudent.setAcademicYear(academicYear);


        logger.info(
                "Section after mapping: {}",
                dbStudent.getSection()
        );

        logger.info(
                "Parent after mapping: {}",
                dbStudent.getParent()
        );

        logger.info(
                "Academic Year after mapping: {}",
                dbStudent.getAcademicYear()
        );

        logger.info(
                "=================================================="
        );

        // Status
        dbStudent.setStatus(student.getStatus());

        // Transport
        if (student.getTransportRequired() != null) {
            dbStudent.setTransportRequired(
                    student.getTransportRequired()
            );
        }

        Student updatedStudent =
                repository.save(dbStudent);

        logger.info(
                "Student updated successfully with ID: {}. Admission number remains unchanged: {}",
                id,
                updatedStudent.getAdmissionNo()
        );

        return mapper.toResponseDto(updatedStudent);
    }


    // ============================================================
    // DELETE STUDENT
    // ============================================================

    @Override
    public void deleteStudent(Long id) {

        Long schoolId = getCurrentSchoolId();

        Student student =
                repository.findByIdAndSection_ClassEntity_School_Id(
                                id,
                                schoolId
                        )
                        .orElseThrow(() ->
                                new StudentNotFoundException(
                                        "Student not found with id : " + id
                                )
                        );

        repository.delete(student);
    }

    // ============================================================
    // FILE UPLOAD
    // ============================================================


    @Override
    public FileUploadResponseDto uploadFile(MultipartFile file) throws IOException {

        // 1. Authenticate the current school
        Long schoolId = getCurrentSchoolId();

        // 2. Empty file validation
        if (file == null || file.isEmpty()) {
            throw new FileUploadException(
                    "Please select a file to upload"
            );
        }

        // 3. File type validation
        String contentType = file.getContentType();

        if (contentType == null ||
                !(contentType.equals("application/pdf")
                        || contentType.equals("image/jpeg")
                        || contentType.equals("image/png"))) {

            throw new FileUploadException(
                    "Only PDF, JPG and PNG files are allowed"
            );
        }

        // 4. File size validation
        long maxSize = 5L * 1024 * 1024;

        if (file.getSize() > maxSize) {
            throw new FileUploadException(
                    "File size should not exceed 5 MB"
            );
        }

        // 5. Create a separate directory for each school
        Path uploadPath = Paths.get("uploads", "school-" + schoolId)
                .toAbsolutePath()
                .normalize();

        Files.createDirectories(uploadPath);

        // 6. Generate a safe, unique filename
        String extension;

        if ("application/pdf".equals(contentType)) {
            extension = ".pdf";
        } else if ("image/jpeg".equals(contentType)) {
            extension = ".jpg";
        } else {
            extension = ".png";
        }

        String fileName = UUID.randomUUID() + extension;

        Path filePath = uploadPath.resolve(fileName).normalize();

        if (!filePath.startsWith(uploadPath)) {
            throw new FileUploadException("Invalid file path");
        }

        // 7. Save the file
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, filePath);
        }

        return FileUploadResponseDto.builder()
                .message("File uploaded successfully")
                .fileName(fileName)
                .fileType(contentType)
                .fileSize(file.getSize())
                .build();
    }

    // ============================================================
    // DOWNLOAD FILE
    // ============================================================


    @Override
    public Resource downloadFile(String fileName)
            throws MalformedURLException {

        Long schoolId = getCurrentSchoolId();

        // Reject empty names and path traversal attempts
        if (fileName == null
                || fileName.isBlank()
                || !fileName.equals(Paths.get(fileName).getFileName().toString())
                || fileName.contains("/")
                || fileName.contains("\\")) {

            throw new FileUploadException("Invalid file name");
        }

        // Access only the current school's directory
        Path uploadPath = Paths.get(
                "uploads",
                "school-" + schoolId
        ).toAbsolutePath().normalize();

        Path filePath = uploadPath.resolve(fileName).normalize();

        if (!filePath.startsWith(uploadPath)) {
            throw new FileUploadException("Invalid file path");
        }

        if (!Files.isRegularFile(filePath)) {
            throw new FileUploadException(
                    "File not found : " + fileName
            );
        }

        Resource resource = new UrlResource(filePath.toUri());

        if (!resource.exists() || !resource.isReadable()) {
            throw new FileUploadException(
                    "File not found or cannot be read : " + fileName
            );
        }

        return resource;
    }


    // ============================================================
    // DELETE FILE
    // ============================================================


    @Override
    public String deleteFile(String fileName) throws IOException {

        Long schoolId = getCurrentSchoolId();

        // 1. Validate the file name
        if (fileName == null
                || fileName.isBlank()
                || !fileName.equals(Paths.get(fileName).getFileName().toString())
                || fileName.contains("/")
                || fileName.contains("\\")) {

            throw new FileUploadException("Invalid file name");
        }

        // 2. Restrict access to the current school's folder
        Path uploadPath = Paths.get(
                "uploads",
                "school-" + schoolId
        ).toAbsolutePath().normalize();

        Path filePath = uploadPath.resolve(fileName).normalize();

        if (!filePath.startsWith(uploadPath)) {
            throw new FileUploadException("Invalid file path");
        }

        // 3. Ensure the file exists and is a regular file
        if (!Files.isRegularFile(filePath)) {
            throw new FileUploadException(
                    "File not found : " + fileName
            );
        }

        // 4. Delete only the file inside this school's folder
        Files.delete(filePath);

        return "File deleted successfully";
    }

    // ============================================================
    // GET STUDENTS BY CLASS
    // ============================================================

    @Override
    public List<StudentResponseDto> getStudentsByClassId(
            Long classId) {

        logger.info(
                "Fetching students for class ID: {}",
                classId
        );

        Long schoolId = getCurrentSchoolId();

        return repository
                .findBySection_ClassEntity_IdAndSection_ClassEntity_School_Id(
                        classId,
                        schoolId
                )
                .stream()
                .map(mapper::toResponseDto)
                .toList();
    }


    // ============================================================
    // GET STUDENTS BY SECTION
    // ============================================================

    @Override
    public List<StudentResponseDto> getStudentsBySectionId(
            Long sectionId) {

        logger.info(
                "Fetching students for section ID: {}",
                sectionId
        );

        Long schoolId = getCurrentSchoolId();

        return repository
                .findBySection_IdAndSection_ClassEntity_School_Id(
                        sectionId,
                        schoolId
                )
                .stream()
                .map(mapper::toResponseDto)
                .toList();
    }


    // ============================================================
    // STUDENT GENDER SUMMARY
    // ============================================================

    @Override
    public StudentGenderSummaryDto getStudentGenderSummary() {

        Long schoolId = getCurrentSchoolId();

        long totalStudents =
                repository.countBySection_ClassEntity_School_Id(schoolId);

        long boys =
                repository.countByGenderIgnoreCaseAndSection_ClassEntity_School_Id(
                        "MALE",
                        schoolId
                );

        long girls =
                repository.countByGenderIgnoreCaseAndSection_ClassEntity_School_Id(
                        "FEMALE",
                        schoolId
                );


        return StudentGenderSummaryDto.builder()
                .totalStudents(totalStudents)
                .boys(boys)
                .girls(girls)
                .build();
    }

    private String generateRollNumber(Long classId) {

        List<String> rollNumbers =
                repository.findRollNumbersByClassOrderByLatest(classId);

        int maxRollNumber = 0;

        for (String rollNumber : rollNumbers) {

            if (rollNumber == null || rollNumber.trim().isEmpty()) {
                continue;
            }

            try {
                int number = Integer.parseInt(rollNumber.trim());

                if (number > maxRollNumber) {
                    maxRollNumber = number;
                }

            } catch (NumberFormatException ignored) {
                // Ignore non-numeric old roll numbers
            }
        }

        return String.valueOf(maxRollNumber + 1);
    }

    private User getCurrentUser() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "User is not authenticated"
            );
        }

        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() ->
                        new org.springframework.security.access.AccessDeniedException(
                                "Authenticated user not found"
                        )
                );
    }

    private Long getCurrentSchoolId() {
        User user = getCurrentUser();

        if (user.getSchool() == null
                || user.getSchool().getId() == null) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Logged-in user is not assigned to a school"
            );
        }

        return user.getSchool().getId();
    }
}