package com.harish.student_management.service;

import com.harish.student_management.entity.*;
import com.harish.student_management.repository.*;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ExcelImportService {

    private final StudentRepository studentRepository;
    private final ParentRepository parentRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SectionRepository sectionRepository;
    private final ClassRepository classRepository;
    private final AcademicYearRepository academicYearRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentSchoolService currentSchoolService;
    private final SchoolRepository schoolRepository;

    /*
     * Excel date format:
     * 03-01-2015
     * 01-06-2026
     */
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy");

    @Transactional
    public String importStudentsAndParents(MultipartFile file) {

        Long schoolId = currentSchoolService.getCurrentSchoolId();
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "Please upload an Excel file"
            );
        }

        String fileName = file.getOriginalFilename();

        if (fileName == null ||
                !(fileName.toLowerCase().endsWith(".xlsx")
                        || fileName.toLowerCase().endsWith(".xls"))) {

            throw new IllegalArgumentException(
                    "Only .xlsx or .xls Excel files are allowed"
            );
        }

        int createdStudents = 0;
        int updatedStudents = 0;
        int createdParents = 0;
        int updatedParents = 0;

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {

            if (workbook.getNumberOfSheets() == 0) {
                throw new IllegalArgumentException(
                        "Excel file does not contain any sheet"
                );
            }

            Sheet sheet = workbook.getSheetAt(0);

            if (sheet.getPhysicalNumberOfRows() < 2) {
                throw new IllegalArgumentException(
                        "Excel file must contain header and data rows"
                );
            }

            Row headerRow = sheet.getRow(0);

            if (headerRow == null) {
                throw new IllegalArgumentException(
                        "Excel header row is missing"
                );
            }

            Map<String, Integer> columns = readHeaders(headerRow);

            validateRequiredColumns(columns);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {

                Row row = sheet.getRow(i);

                if (row == null || isEmptyRow(row)) {
                    continue;
                }

                int excelRowNumber = i + 1;

                String admissionNo =
                        getString(row, columns, "admissionNo");

                if (admissionNo == null || admissionNo.isBlank()) {
                    throw new IllegalArgumentException(
                            "Admission number is required at Excel row "
                                    + excelRowNumber
                    );
                }

                /*
                 * ---------------------------------------------------------
                 * PARENT
                 * ---------------------------------------------------------
                 */

                String parentPhone =
                        getString(row, columns, "parentPhone");

                Parent parent = null;

                if (parentPhone != null && !parentPhone.isBlank()) {

                    Optional<Parent> existingParent =
                            parentRepository.findByContactPhoneAndUser_School_Id(
                                    parentPhone,
                                    schoolId
                            );
                    if (existingParent.isPresent()) {

                        parent = existingParent.get();

                        updateParent(parent, row, columns);

                        /*
                         * Existing parent does not have login account.
                         * Create PARENT user automatically.
                         */
                        if (parent.getUser() == null) {

                            String email =
                                    getString(
                                            row,
                                            columns,
                                            "parentEmail"
                                    );

                            User user = createParentUser(
                                    parentPhone,
                                    email,
                                    schoolId
                            );

                            parent.setUser(user);
                        }

                        parentRepository.save(parent);

                        updatedParents++;

                    } else {

                        parent = createParent(
                                row,
                                columns,
                                parentPhone,
                                schoolId
                        );

                        parentRepository.save(parent);

                        createdParents++;
                    }
                }

                /*
                 * ---------------------------------------------------------
                 * STUDENT
                 * ---------------------------------------------------------
                 */

                Optional<Student> existingStudent =
                        studentRepository
                                .findByAdmissionNoAndSection_ClassEntity_School_Id(
                                        admissionNo,
                                        schoolId
                                );

                Student student;

                if (existingStudent.isPresent()) {

                    student = existingStudent.get();

                    updateStudent(
                            student,
                            row,
                            columns,
                            parent
                    );

                    studentRepository.save(student);

                    updatedStudents++;

                } else {

                    student = createStudent(
                            row,
                            columns,
                            parent
                    );

                    studentRepository.save(student);

                    createdStudents++;
                }
            }

        } catch (Exception e) {

            /*
             * Transactional method:
             * if any row fails, the whole import is rolled back.
             */
            throw new IllegalArgumentException(
                    "Excel import failed: " + e.getMessage(),
                    e
            );
        }

        return String.format(
                "Import completed. Students created: %d, updated: %d, Parents created: %d, updated: %d",
                createdStudents,
                updatedStudents,
                createdParents,
                updatedParents
        );
    }

    /*
     * ============================================================
     * PARENT
     * ============================================================
     */

    private Parent createParent(
            Row row,
            Map<String, Integer> columns,
            String parentPhone,
            Long schoolId) {

        String email =
                getString(
                        row,
                        columns,
                        "parentEmail"
                );

        User user = createParentUser(
                parentPhone,
                email,
                schoolId
        );

        return Parent.builder()
                .fatherName(
                        getString(
                                row,
                                columns,
                                "fatherName"
                        )
                )
                .surname(
                        getString(
                                row,
                                columns,
                                "parentSurname"
                        )
                )
                .motherName(
                        getString(
                                row,
                                columns,
                                "motherName"
                        )
                )
                .guardianName(
                        getString(
                                row,
                                columns,
                                "guardianName"
                        )
                )
                .contactPhone(parentPhone)
                .email(email)
                .relationship(
                        getString(
                                row,
                                columns,
                                "relationship"
                        )
                )
                .address(
                        getString(
                                row,
                                columns,
                                "parentAddress"
                        )
                )
                .user(user)
                .build();
    }

    private void updateParent(
            Parent parent,
            Row row,
            Map<String, Integer> columns) {

        String value;

        value = getString(
                row,
                columns,
                "fatherName"
        );

        if (value != null) {
            parent.setFatherName(value);
        }

        value = getString(
                row,
                columns,
                "parentSurname"
        );

        if (value != null) {
            parent.setSurname(value);
        }

        value = getString(
                row,
                columns,
                "motherName"
        );

        if (value != null) {
            parent.setMotherName(value);
        }

        value = getString(
                row,
                columns,
                "guardianName"
        );

        if (value != null) {
            parent.setGuardianName(value);
        }

        value = getString(
                row,
                columns,
                "parentEmail"
        );

        if (value != null && !value.isBlank()) {

            String newEmail = value.trim();

            User linkedUser = parent.getUser();

            if (linkedUser != null
                    && linkedUser.getEmail() != null
                    && !linkedUser.getEmail().equalsIgnoreCase(newEmail)) {

                throw new IllegalArgumentException(
                        "Parent email change must be handled through "
                                + "the user account update flow: " + newEmail
                );
            }

            parent.setEmail(newEmail);
        }

        value = getString(
                row,
                columns,
                "relationship"
        );

        if (value != null) {
            parent.setRelationship(value);
        }

        value = getString(
                row,
                columns,
                "parentAddress"
        );

        if (value != null) {
            parent.setAddress(value);
        }
    }

    private User createParentUser(
            String phone,
            String email,
            Long schoolId) {

        String username = phone;

        /*
         * ---------------------------------------------------------
         * USERNAME CHECK
         * ---------------------------------------------------------
         */

        Optional<User> existingByUsername =
                userRepository.findByUsernameAndSchool_Id(
                        username,
                        schoolId
                );

        if (existingByUsername.isPresent()) {

            User existingUser = existingByUsername.get();

            if (existingUser.getRole() != null
                    && "PARENT".equalsIgnoreCase(
                    existingUser.getRole().getName())) {
                return existingUser;
            }

            throw new IllegalArgumentException(
                    "Username already belongs to another user: "
                            + username
            );
        }

// Username is globally unique.
// If it exists but not in this school, do not reuse it.
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException(
                    "This phone number is already registered "
                            + "under another school: " + username
            );
        }
        /*
         * ---------------------------------------------------------
         * EMAIL CHECK
         * ---------------------------------------------------------
         */

        if (email != null && !email.isBlank()) {

            Optional<User> existingByEmail =
                    userRepository.findByEmailAndSchool_Id(
                            email,
                            schoolId
                    );

            if (existingByEmail.isPresent()) {
                User existingUser = existingByEmail.get();

                if (existingUser.getRole() != null
                        && "PARENT".equalsIgnoreCase(
                        existingUser.getRole().getName())) {
                    return existingUser;
                }

                throw new IllegalArgumentException(
                        "Email already belongs to another user in your school: "
                                + email
                );
            }

            // Email is globally unique in User entity.
            // Do not reuse an account belonging to another school.
            if (userRepository.existsByEmail(email)) {
                throw new IllegalArgumentException(
                        "This email is already registered. "
                                + "Use a different email: " + email
                );
            }
        }
        /*
         * ---------------------------------------------------------
         * PARENT ROLE
         * ---------------------------------------------------------
         */

        Role parentRole =
                roleRepository.findByName("PARENT")
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "PARENT role not found"
                                )
                        );

        /*
         * Initial password.
         *
         * Parent can change/reset this later.
         */
        String temporaryPassword = "Parent@123";

        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "School not found: " + schoolId
                        )
                );

        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(temporaryPassword))
                .role(parentRole)
                .school(school)
                .status("ACTIVE")
                .build();

        return userRepository.save(user);
    }

    /*
     * ============================================================
     * STUDENT CREATE
     * ============================================================
     */

    private Student createStudent(
            Row row,
            Map<String, Integer> columns,
            Parent parent) {

        Student student = new Student();

        student.setAdmissionNo(
                getString(
                        row,
                        columns,
                        "admissionNo"
                )
        );

        student.setRollNumber(
                getString(
                        row,
                        columns,
                        "rollNumber"
                )
        );

        student.setName(
                getString(
                        row,
                        columns,
                        "name"
                )
        );

        student.setSurname(
                getString(
                        row,
                        columns,
                        "surname"
                )
        );

        student.setEmail(
                getString(
                        row,
                        columns,
                        "email"
                )
        );

        student.setPhone(
                getString(
                        row,
                        columns,
                        "phone"
                )
        );

        student.setDateOfBirth(
                getDate(
                        row,
                        columns,
                        "dateOfBirth"
                )
        );

        student.setGender(
                getString(
                        row,
                        columns,
                        "gender"
                )
        );

        student.setBloodGroup(
                getString(
                        row,
                        columns,
                        "bloodGroup"
                )
        );

        student.setAdmissionDate(
                getDate(
                        row,
                        columns,
                        "admissionDate"
                )
        );

        student.setAddress(
                getString(
                        row,
                        columns,
                        "address"
                )
        );

        student.setStatus(
                getString(
                        row,
                        columns,
                        "status"
                )
        );

        Boolean transportRequired =
                getBoolean(
                        row,
                        columns,
                        "transportRequired"
                );

        student.setTransportRequired(
                transportRequired != null
                        ? transportRequired
                        : false
        );

        student.setParent(parent);

        /*
         * CLASS + SECTION
         *
         * Excel:
         * className   = 4th Class
         * sectionName = A
         */
        setClassAndSection(
                student,
                row,
                columns
        );

        /*
         * ACADEMIC YEAR
         *
         * Excel:
         * academicYear = 2026-2027
         */
        setAcademicYear(
                student,
                row,
                columns
        );

        return student;
    }

    /*
     * ============================================================
     * STUDENT UPDATE
     * ============================================================
     */

    private void updateStudent(
            Student student,
            Row row,
            Map<String, Integer> columns,
            Parent parent) {

        String value;

        value = getString(
                row,
                columns,
                "rollNumber"
        );

        if (value != null) {
            student.setRollNumber(value);
        }

        value = getString(
                row,
                columns,
                "name"
        );

        if (value != null) {
            student.setName(value);
        }

        value = getString(
                row,
                columns,
                "surname"
        );

        if (value != null) {
            student.setSurname(value);
        }

        value = getString(
                row,
                columns,
                "email"
        );

        if (value != null) {
            student.setEmail(value);
        }

        value = getString(
                row,
                columns,
                "phone"
        );

        if (value != null) {
            student.setPhone(value);
        }

        LocalDate date =
                getDate(
                        row,
                        columns,
                        "dateOfBirth"
                );

        if (date != null) {
            student.setDateOfBirth(date);
        }

        value = getString(
                row,
                columns,
                "gender"
        );

        if (value != null) {
            student.setGender(value);
        }

        value = getString(
                row,
                columns,
                "bloodGroup"
        );

        if (value != null) {
            student.setBloodGroup(value);
        }

        date =
                getDate(
                        row,
                        columns,
                        "admissionDate"
                );

        if (date != null) {
            student.setAdmissionDate(date);
        }

        value = getString(
                row,
                columns,
                "address"
        );

        if (value != null) {
            student.setAddress(value);
        }

        value = getString(
                row,
                columns,
                "status"
        );

        if (value != null) {
            student.setStatus(value);
        }

        Boolean transportRequired =
                getBoolean(
                        row,
                        columns,
                        "transportRequired"
                );

        if (transportRequired != null) {
            student.setTransportRequired(
                    transportRequired
            );
        }

        /*
         * Only replace parent when Excel contains
         * a parent for this row.
         */
        if (parent != null) {
            student.setParent(parent);
        }

        /*
         * Update class + section.
         */
        setClassAndSection(
                student,
                row,
                columns
        );

        /*
         * Update academic year.
         */
        setAcademicYear(
                student,
                row,
                columns
        );
    }

    /*
     * ============================================================
     * CLASS + SECTION
     * ============================================================
     */

    private void setClassAndSection(
            Student student,
            Row row,
            Map<String, Integer> columns) {

        String className =
                getString(
                        row,
                        columns,
                        "className"
                );

        String sectionName =
                getString(
                        row,
                        columns,
                        "sectionName"
                );

        /*
         * Class is required.
         */
        if (className == null || className.isBlank()) {

            throw new IllegalArgumentException(
                    "Class name is required"
            );
        }

        /*
         * Section is required.
         */
        if (sectionName == null || sectionName.isBlank()) {

            throw new IllegalArgumentException(
                    "Section name is required for class: "
                            + className
            );
        }

        /*
         * Find ClassEntity by class name.
         *
         * Example:
         * 4th Class
         */
        Long schoolId = currentSchoolService.getCurrentSchoolId();

        ClassEntity classEntity =
                classRepository
                        .findByNameIgnoreCaseAndSchool_Id(
                                className,
                                schoolId
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Class not found in your school: "
                                                + className
                                )
                        );

        /*
         * Find Section by:
         *
         * class + section name
         *
         * Example:
         * 4th Class + A
         */
        Section section =
                sectionRepository
                        .findByNameIgnoreCaseAndClassEntityIdAndClassEntity_School_Id(
                                sectionName,
                                classEntity.getId(),
                                schoolId
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Section '" + sectionName
                                                + "' not found in your school, class '"
                                                + className + "'"
                                )
                        );

        student.setSection(section);
    }

    /*
     * ============================================================
     * ACADEMIC YEAR
     * ============================================================
     */

    private void setAcademicYear(
            Student student,
            Row row,
            Map<String, Integer> columns) {

        String academicYearName =
                getString(
                        row,
                        columns,
                        "academicYear"
                );

        if (academicYearName == null
                || academicYearName.isBlank()) {

            throw new IllegalArgumentException(
                    "Academic year is required"
            );
        }

        Long schoolId = currentSchoolService.getCurrentSchoolId();

        AcademicYear academicYear =
                academicYearRepository
                        .findBySchool_IdAndNameIgnoreCase(
                                schoolId,
                                academicYearName.trim()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Academic year not found in your school: "
                                                + academicYearName
                                )
                        );

        student.setAcademicYear(academicYear);
    }

    /*
     * ============================================================
     * BOOLEAN
     * ============================================================
     */

    private Boolean getBoolean(
            Row row,
            Map<String, Integer> columns,
            String column) {

        Integer index =
                columns.get(
                        column.toLowerCase()
                );

        if (index == null) {
            return null;
        }

        String value =
                getString(
                        row,
                        columns,
                        column
                );

        if (value == null) {
            return null;
        }

        if (value.equalsIgnoreCase("YES")
                || value.equalsIgnoreCase("TRUE")
                || value.equalsIgnoreCase("1")) {

            return true;
        }

        if (value.equalsIgnoreCase("NO")
                || value.equalsIgnoreCase("FALSE")
                || value.equalsIgnoreCase("0")) {

            return false;
        }

        throw new IllegalArgumentException(
                "Invalid value in column "
                        + column
                        + ": "
                        + value
                        + ". Use TRUE or FALSE"
        );
    }

    /*
     * ============================================================
     * EXCEL HEADERS
     * ============================================================
     */

    private Map<String, Integer> readHeaders(Row row) {

        Map<String, Integer> columns =
                new HashMap<>();

        DataFormatter formatter =
                new DataFormatter();

        for (Cell cell : row) {

            String header =
                    formatter
                            .formatCellValue(cell)
                            .trim()
                            .toLowerCase();

            if (!header.isBlank()) {

                columns.put(
                        header,
                        cell.getColumnIndex()
                );
            }
        }

        return columns;
    }

    /*
     * ============================================================
     * REQUIRED COLUMNS
     * ============================================================
     */

    private void validateRequiredColumns(
            Map<String, Integer> columns) {

        /*
         * These are the columns that must be present
         * in the final Excel format.
         */
        List<String> required = List.of(
                "admissionNo",
                "name",
                "className",
                "sectionName",
                "academicYear"
        );

        for (String column : required) {

            if (!columns.containsKey(
                    column.toLowerCase()
            )) {

                throw new IllegalArgumentException(
                        "Missing required Excel column: "
                                + column
                );
            }
        }
    }

    /*
     * ============================================================
     * STRING VALUE
     * ============================================================
     */

    private String getString(
            Row row,
            Map<String, Integer> columns,
            String column) {

        Integer index =
                columns.get(
                        column.toLowerCase()
                );

        if (index == null) {
            return null;
        }

        Cell cell =
                row.getCell(index);

        if (cell == null) {
            return null;
        }

        DataFormatter formatter =
                new DataFormatter();

        String value =
                formatter.formatCellValue(cell);

        if (value == null
                || value.isBlank()) {

            return null;
        }

        return value.trim();
    }

    /*
     * ============================================================
     * DATE
     * ============================================================
     */

    private LocalDate getDate(
            Row row,
            Map<String, Integer> columns,
            String column) {

        Integer index =
                columns.get(
                        column.toLowerCase()
                );

        if (index == null) {
            return null;
        }

        Cell cell =
                row.getCell(index);

        if (cell == null) {
            return null;
        }

        /*
         * If Excel stores it as an actual date cell.
         */
        if (cell.getCellType() == CellType.NUMERIC
                && DateUtil.isCellDateFormatted(cell)) {

            return cell
                    .getLocalDateTimeCellValue()
                    .toLocalDate();
        }

        String value =
                getString(
                        row,
                        columns,
                        column
                );

        if (value == null) {
            return null;
        }

        try {

            return LocalDate.parse(
                    value,
                    DATE_FORMAT
            );

        } catch (DateTimeParseException e) {

            throw new IllegalArgumentException(
                    "Invalid date in column "
                            + column
                            + ": "
                            + value
                            + ". Use dd-MM-yyyy"
            );
        }
    }

    /*
     * ============================================================
     * EMPTY ROW
     * ============================================================
     */

    private boolean isEmptyRow(Row row) {

        DataFormatter formatter =
                new DataFormatter();

        for (Cell cell : row) {

            if (cell != null
                    && cell.getCellType()
                    != CellType.BLANK
                    && !formatter
                    .formatCellValue(cell)
                    .isBlank()) {

                return false;
            }
        }

        return true;
    }
}