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
        Section section =
                sectionRepository
                        .findByIdWithClassAndSchool(student.getSectionId())
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Selected section was not found."
                                )
                        );

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
                while (repository.existsByAdmissionNo(generated)) {

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

        Page<Student> students =
                repository.findAll(pageable);

        return students.map(mapper::toResponseDto);
    }


    // ============================================================
    // GET STUDENT BY ID
    // ============================================================

    @Override
    public StudentResponseDto getStudentById(Long id) {

        logger.info("Student {}", id);

        Student student =
                repository.findById(id)
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

        Student dbStudent =
                repository.findById(id)
                        .orElseThrow(() ->
                                new StudentNotFoundException(
                                        "Student not found with id : "
                                                + id
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

        dbStudent.setSection(
                mapper.mapSectionIdToSection(
                        student.getSectionId()
                )
        );

        dbStudent.setParent(
                mapper.mapParentIdToParent(
                        student.getParentId()
                )
        );

        dbStudent.setAcademicYear(
                mapper.mapAcademicYearIdToAcademicYear(
                        student.getAcademicYearId()
                )
        );

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

        logger.info("Student {}", id);

        Student student =
                repository.findById(id)
                        .orElseThrow(() ->
                                new StudentNotFoundException(
                                        "Student not found with id : "
                                                + id
                                )
                        );

        logger.info(
                "Successfully fetched Student details by id {}",
                id
        );

        repository.delete(student);

        logger.info(
                "Student deleted successfully. Admission number {} will not be reused.",
                student.getAdmissionNo()
        );
    }


    // ============================================================
    // FILE UPLOAD
    // ============================================================

    @Override
    public FileUploadResponseDto uploadFile(
            MultipartFile file) throws IOException {

        // 1. Empty file validation
        if (file.isEmpty()) {
            throw new FileUploadException(
                    "Please select a file to upload"
            );
        }

        // 2. File type validation
        String contentType =
                file.getContentType();

        if (contentType == null ||
                !(contentType.equals("application/pdf")
                        || contentType.equals("image/jpeg")
                        || contentType.equals("image/png"))) {

            throw new FileUploadException(
                    "Only PDF, JPG and PNG files are allowed"
            );
        }

        // 3. File size validation
        long maxSize =
                5 * 1024 * 1024;

        if (file.getSize() > maxSize) {
            throw new FileUploadException(
                    "File size should not exceed 5 MB"
            );
        }

        Path uploadPath =
                Paths.get("uploads");

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String fileName =
                UUID.randomUUID()
                        + "_"
                        + file.getOriginalFilename();

        Path filePath =
                uploadPath.resolve(fileName);

        Files.copy(
                file.getInputStream(),
                filePath,
                StandardCopyOption.REPLACE_EXISTING
        );

        return FileUploadResponseDto.builder()
                .message("File uploaded successfully")
                .fileName(fileName)
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .build();
    }


    // ============================================================
    // DOWNLOAD FILE
    // ============================================================

    @Override
    public Resource downloadFile(
            String fileName)
            throws MalformedURLException {

        Path filePath =
                Paths.get("uploads")
                        .resolve(fileName);

        Resource resource =
                new UrlResource(
                        filePath.toUri()
                );

        if (!resource.exists()) {
            throw new FileUploadException(
                    "File not found : " + fileName
            );
        }

        return resource;
    }


    // ============================================================
    // DELETE FILE
    // ============================================================

    @Override
    public String deleteFile(
            String fileName)
            throws IOException {

        Path filePath =
                Paths.get("uploads")
                        .resolve(fileName);

        if (!Files.exists(filePath)) {
            throw new FileUploadException(
                    "File not found : " + fileName
            );
        }

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

        return repository
                .findBySection_ClassEntity_Id(classId)
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

        return repository
                .findBySectionId(sectionId)
                .stream()
                .map(mapper::toResponseDto)
                .toList();
    }


    // ============================================================
    // STUDENT GENDER SUMMARY
    // ============================================================

    @Override
    public StudentGenderSummaryDto getStudentGenderSummary() {

        long totalStudents =
                repository.count();

        long boys =
                repository.countByGenderIgnoreCase("MALE");

        long girls =
                repository.countByGenderIgnoreCase("FEMALE");

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
}