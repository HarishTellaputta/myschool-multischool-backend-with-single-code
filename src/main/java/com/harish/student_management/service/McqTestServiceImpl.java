
        package com.harish.student_management.service;

import com.harish.student_management.dto.*;
import com.harish.student_management.entity.*;
import com.harish.student_management.exception.ResourceNotFoundException;
import com.harish.student_management.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class McqTestServiceImpl implements McqTestService {
    private final McqTestRepository tests;
    private final McqQuestionRepository questions;
    private final McqTestAttemptRepository attempts;
    private final McqTestAnswerRepository answers;
    private final ClassRepository classes;
    private final SectionRepository sections;
    private final StudentRepository students;
    private final ParentRepository parents;

    // =========================================================
    // CREATE TEST
    // =========================================================

    @Override
    public McqTestResponseDto create(McqTestRequestDto r) {

        if (r == null) {
            throw new IllegalArgumentException("Test request cannot be null");
        }

        if (r.getClassId() == null) {
            throw new IllegalArgumentException("Class is required");
        }

        if (r.getDate() == null) {
            throw new IllegalArgumentException("Test date is required");
        }

        if (r.getStartTime() == null) {
            throw new IllegalArgumentException("Test start time is required");
        }

        if (r.getDuration() == null || r.getDuration() <= 0) {
            throw new IllegalArgumentException("Duration must be greater than 0 minutes");
        }

        if (r.getNumberOfQuestions() == null || r.getNumberOfQuestions() <= 0) {
            throw new IllegalArgumentException(
                    "Number of questions must be greater than 0"
            );
        }

        if (r.getSubject() == null || r.getSubject().trim().isEmpty()) {
            throw new IllegalArgumentException("Subject is required");
        }

        ClassEntity classEntity = classes.findById(r.getClassId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Class not found: " + r.getClassId()
                        )
                );

        Section section = null;

        if (r.getSectionId() != null) {

            section = sections.findById(r.getSectionId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Section not found: " + r.getSectionId()
                            )
                    );

            if (section.getClassEntity() == null
                    || !section.getClassEntity()
                    .getId()
                    .equals(classEntity.getId())) {

                throw new IllegalArgumentException(
                        "Section does not belong to selected class"
                );
            }
        }

        LocalDate today = LocalDate.now();

        if (r.getDate().isBefore(today)) {
            throw new IllegalArgumentException(
                    "Test date cannot be in the past"
            );
        }

        String subject = r.getSubject().trim();

        // ---------------------------------------------------------
        // Check duplicate test
        // ---------------------------------------------------------

        if (tests.findByClassEntityIdAndSectionIdAndSubjectAndTestDate(
                classEntity.getId(),
                r.getSectionId(),
                subject,
                r.getDate()
        ).isPresent()) {

            throw new IllegalArgumentException(
                    "Only one subject test is allowed per class/section/day"
            );
        }

        // ---------------------------------------------------------
        // Find available question pool
        // ---------------------------------------------------------

        List<McqQuestion> pool =
                questions.findByQuestionDateAndClassEntityId(
                                r.getDate(),
                                classEntity.getId()
                        )
                        .stream()
                        .filter(Objects::nonNull)
                        .filter(q ->
                                q.getSubject() != null
                                        && q.getSubject() != null
                                        && q.getSubject().getName() != null
                                        && q.getSubject().getName().trim().equalsIgnoreCase(subject)
                        )
                        .toList();

        if (pool.size() < r.getNumberOfQuestions()) {

            throw new IllegalArgumentException(
                    "Not enough MCQ questions available for this test. "
                            + "Required: " + r.getNumberOfQuestions()
                            + ", Available: " + pool.size()
            );
        }

        // ---------------------------------------------------------
        // Randomize questions for response
        // ---------------------------------------------------------

        List<McqQuestion> selectedQuestions =
                new ArrayList<>(pool);

        Collections.shuffle(selectedQuestions);

        selectedQuestions =
                selectedQuestions
                        .subList(0, r.getNumberOfQuestions());

        // ---------------------------------------------------------
        // Create test
        // ---------------------------------------------------------

        McqTest test = McqTest.builder()
                .classEntity(classEntity)
                .section(section)
                .subject(subject)
                .testDate(r.getDate())
                .startTime(r.getStartTime())
                .duration(r.getDuration())
                .numberOfQuestions(r.getNumberOfQuestions())
                .status("SCHEDULED")
                .build();

        test = tests.save(test);

        return map(test, selectedQuestions);
    }

    // =========================================================
    // LIST TESTS
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<McqTestResponseDto> list(LocalDate date) {

        List<McqTest> testList =
                date == null
                        ? tests.findAll()
                        : tests.findByTestDate(date);

        return testList
                .stream()
                .map(test -> map(test, pool(test)))
                .toList();
    }

    // =========================================================
    // START TEST
    // =========================================================

    @Override
    public McqAttemptResponseDto start(
            Long testId,
            Long studentId,
            String username,
            boolean isParent
    ) {

        McqTest test = findTest(testId);

        Student student = findStudent(studentId);

        validateStudent(test, student);

        if (isParent) {
            validateParentStudentOwnership(
                    username,
                    student
            );
        }
        // ---------------------------------------------------------
        // Validate test status
        // ---------------------------------------------------------

        if ("CANCELLED".equalsIgnoreCase(test.getStatus())) {
            throw new IllegalStateException(
                    "This test has been cancelled"
            );
        }

        if (!"SCHEDULED".equalsIgnoreCase(test.getStatus())
                && !"ACTIVE".equalsIgnoreCase(test.getStatus())) {

            throw new IllegalStateException(
                    "Test is not available for starting"
            );
        }

// ---------------------------------------------------------
// Check test window
// ---------------------------------------------------------

        LocalDateTime now = LocalDateTime.now();

        LocalDateTime scheduledStart =
                LocalDateTime.of(
                        test.getTestDate(),
                        test.getStartTime()
                );

        LocalDateTime dayEnd =
                test.getTestDate()
                        .plusDays(1)
                        .atStartOfDay();

        if (now.isBefore(scheduledStart)) {

            throw new IllegalStateException(
                    "Test has not started yet"
            );
        }

        if (!now.isBefore(dayEnd)) {

            throw new IllegalStateException(
                    "Test window has expired"
            );
        }

// ---------------------------------------------------------
// Check existing attempt
// ---------------------------------------------------------

        McqTestAttempt existingAttempt =
                attempts.findByTestIdAndStudentId(
                        testId,
                        studentId
                ).orElse(null);

        if (existingAttempt != null) {

            if ("SUBMITTED".equalsIgnoreCase(existingAttempt.getStatus())
                    || "AUTO_SUBMITTED".equalsIgnoreCase(existingAttempt.getStatus())) {

                throw new IllegalStateException(
                        "Student has already submitted this test"
                );
            }

            if ("IN_PROGRESS".equalsIgnoreCase(existingAttempt.getStatus())) {

                expire(existingAttempt);

                if ("SUBMITTED".equalsIgnoreCase(existingAttempt.getStatus())
                        || "AUTO_SUBMITTED".equalsIgnoreCase(existingAttempt.getStatus())) {

                    throw new IllegalStateException(
                            "Student has already submitted this test"
                    );
                }

                return attemptMap(
                        existingAttempt,
                        pool(existingAttempt.getTest())
                );
            }
        }

// ---------------------------------------------------------
// Make sure questions are available
// ---------------------------------------------------------

        List<McqQuestion> questionPool = pool(test);

        if (questionPool.size() < test.getNumberOfQuestions()) {

            throw new IllegalStateException(
                    "Required MCQ questions are no longer available"
            );
        }

// ---------------------------------------------------------
// Create attempt
// ---------------------------------------------------------

        LocalDateTime expiresAt =
                now.plusMinutes(test.getDuration());

        McqTestAttempt attempt =
                McqTestAttempt.builder()
                        .test(test)
                        .student(student)
                        .startedAt(now)
                        .expiresAt(expiresAt)
                        .status("IN_PROGRESS")
                        .build();

        attempt = attempts.save(attempt);

        return attemptMap(
                attempt,
                questionPool
        );
    }

    // =========================================================
    // ANSWER QUESTION
    // =========================================================

    @Override
    public McqAttemptResponseDto answer(
            Long attemptId,
            Long studentId,
            McqAnswerRequestDto r,
            String username,
            boolean isParent
    ) {

        if (r == null) {
            throw new IllegalArgumentException(
                    "Answer request cannot be null"
            );
        }

        if (r.getQuestionId() == null) {
            throw new IllegalArgumentException(
                    "Question ID is required"
            );
        }

        if (r.getAnswer() == null
                || r.getAnswer().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Answer is required"
            );
        }

        McqTestAttempt attempt =
                findAttempt(attemptId);

        checkOwner(
                attempt,
                studentId
        );

        if (isParent) {
            validateParentStudentOwnership(
                    username,
                    attempt.getStudent()
            );
        }

        // ---------------------------------------------------------
        // Automatically expire if time is over
        // ---------------------------------------------------------

        expire(attempt);

        if (!"IN_PROGRESS".equalsIgnoreCase(
                attempt.getStatus())) {

            throw new IllegalStateException(
                    "Test is no longer accepting answers"
            );
        }

        // ---------------------------------------------------------
        // Validate question
        // ---------------------------------------------------------

        McqQuestion question =
                questions.findById(r.getQuestionId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Question not found: "
                                                + r.getQuestionId()
                                )
                        );

        List<McqQuestion> questionPool =
                pool(attempt.getTest());

        boolean belongsToTest =
                questionPool
                        .stream()
                        .anyMatch(q ->
                                q.getId()
                                        .equals(question.getId())
                        );

        if (!belongsToTest) {

            throw new IllegalArgumentException(
                    "Question is not part of this test"
            );
        }

        // ---------------------------------------------------------
        // Validate answer
        // ---------------------------------------------------------

        String selectedAnswer =
                r.getAnswer()
                        .trim()
                        .toUpperCase(Locale.ROOT);

        validateAnswerOption(selectedAnswer);

        // ---------------------------------------------------------
        // Save / update answer
        // ---------------------------------------------------------

        McqTestAnswer answer =
                answers.findByAttemptIdAndQuestionId(
                        attemptId,
                        question.getId()
                ).orElseGet(() ->
                        McqTestAnswer.builder()
                                .attempt(attempt)
                                .question(question)
                                .build()
                );

        answer.setSelectedAnswer(selectedAnswer);

        answers.save(answer);

        return attemptMap(
                attempt,
                questionPool
        );
    }

    // =========================================================
    // SUBMIT TEST
    // =========================================================

    @Override
    public McqAttemptResponseDto submit(
            Long attemptId,
            Long studentId,
            String username,
            boolean isParent
    ) {

        McqTestAttempt attempt =
                findAttempt(attemptId);

        checkOwner(
                attempt,
                studentId
        );

        if (isParent) {
            validateParentStudentOwnership(
                    username,
                    attempt.getStudent()
            );
        }
        expire(attempt);

        // ---------------------------------------------------------
        // If still in progress, manually submit
        // ---------------------------------------------------------

        if ("IN_PROGRESS".equalsIgnoreCase(
                attempt.getStatus())) {

            finalizeResult(attempt);

            attempt.setStatus("SUBMITTED");

            attempt.setSubmittedAt(
                    LocalDateTime.now()
            );
        }

        return attemptMap(
                attempt,
                pool(attempt.getTest())
        );
    }

    // =========================================================
    // STUDENT HISTORY
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<McqAttemptResponseDto> history(
            Long studentId
    ) {

        findStudent(studentId);

        return attempts
                .findByStudentIdOrderByStartedAtDesc(studentId)
                .stream()
                .map(attempt ->
                        attemptMap(
                                attempt,
                                pool(attempt.getTest())
                        )
                )
                .toList();
    }

    // =========================================================
    // CLASS PERFORMANCE
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<McqAttemptResponseDto> classPerformance(
            Long classId
    ) {

        classes.findById(classId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Class not found: " + classId
                        )
                );

        return attempts
                .findByTestClassEntityId(classId)
                .stream()
                .map(attempt ->
                        attemptMap(
                                attempt,
                                pool(attempt.getTest())
                        )
                )
                .toList();
    }

    // =========================================================
    // OVERALL PERFORMANCE
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<McqAttemptResponseDto> overallPerformance() {

        return attempts
                .findAll()
                .stream()
                .map(attempt ->
                        attemptMap(
                                attempt,
                                pool(attempt.getTest())
                        )
                )
                .toList();
    }

    // =========================================================
    // AUTO EXPIRE
    // =========================================================

    private void expire(McqTestAttempt attempt) {

        if (!"IN_PROGRESS".equalsIgnoreCase(
                attempt.getStatus())) {

            return;
        }

        LocalDateTime expiresAt =
                attempt.getExpiresAt();

        if (expiresAt == null) {
            return;
        }

        if (!LocalDateTime.now().isBefore(expiresAt)) {

            finalizeResult(attempt);

            attempt.setStatus("AUTO_SUBMITTED");

            attempt.setSubmittedAt(expiresAt);
        }
    }

    // =========================================================
    // FINALIZE RESULT
    // =========================================================

    private void finalizeResult(
            McqTestAttempt attempt
    ) {

        List<McqQuestion> questionPool =
                pool(attempt.getTest());

        List<McqTestAnswer> submittedAnswers =
                answers.findByAttemptId(
                        attempt.getId()
                );

        int correctAnswers =
                (int) submittedAnswers
                        .stream()
                        .filter(Objects::nonNull)
                        .filter(answer ->
                                answer.getSelectedAnswer() != null
                        )
                        .filter(answer ->
                                answer.getQuestion() != null
                        )
                        .filter(answer ->
                                answer.getQuestion()
                                        .getCorrectAnswer() != null
                        )
                        .filter(answer ->
                                answer.getSelectedAnswer()
                                        .trim()
                                        .equalsIgnoreCase(
                                                answer.getQuestion()
                                                        .getCorrectAnswer()
                                                        .trim()
                                        )
                        )
                        .count();

        int score =
                submittedAnswers
                        .stream()
                        .filter(Objects::nonNull)
                        .filter(answer ->
                                answer.getSelectedAnswer() != null
                        )
                        .filter(answer ->
                                answer.getQuestion() != null
                        )
                        .filter(answer ->
                                answer.getQuestion()
                                        .getCorrectAnswer() != null
                        )
                        .filter(answer ->
                                answer.getSelectedAnswer()
                                        .trim()
                                        .equalsIgnoreCase(
                                                answer.getQuestion()
                                                        .getCorrectAnswer()
                                                        .trim()
                                        )
                        )
                        .mapToInt(answer -> {

                            Integer marks =
                                    answer.getQuestion()
                                            .getMarks();

                            return marks == null
                                    ? 0
                                    : marks;
                        })
                        .sum();

        int totalQuestions =
                questionPool.size();

        int wrongAnswers =
                Math.max(
                        0,
                        totalQuestions - correctAnswers
                );

        int maxMarks =
                questionPool
                        .stream()
                        .mapToInt(question -> {

                            Integer marks =
                                    question.getMarks();

                            return marks == null
                                    ? 0
                                    : marks;
                        })
                        .sum();

        double percentage =
                maxMarks == 0
                        ? 0.0
                        : Math.round(
                        score * 10000.0 / maxMarks
                ) / 100.0;

        attempt.setTotalQuestions(
                totalQuestions
        );

        attempt.setCorrectAnswers(
                correctAnswers
        );

        attempt.setWrongAnswers(
                wrongAnswers
        );

        attempt.setScore(
                score
        );

        attempt.setPercentage(
                percentage
        );
    }

    // =========================================================
    // VALIDATE STUDENT
    // =========================================================

    private void validateStudent(
            McqTest test,
            Student student
    ) {

        if (student.getSection() == null) {

            throw new SecurityException(
                    "Student is not assigned to a section"
            );
        }

        if (student.getSection().getClassEntity() == null) {

            throw new SecurityException(
                    "Student section is not assigned to a class"
            );
        }

        Long studentClassId =
                student.getSection()
                        .getClassEntity()
                        .getId();

        Long testClassId =
                test.getClassEntity()
                        .getId();

        if (!studentClassId.equals(testClassId)) {

            throw new SecurityException(
                    "Student is not assigned to this test class"
            );
        }

        // If test is section-specific,
        // student must belong to the same section.
        if (test.getSection() != null) {

            if (!test.getSection()
                    .getId()
                    .equals(
                            student.getSection().getId()
                    )) {

                throw new SecurityException(
                        "Student is not assigned to this test section"
                );
            }
        }
    }

    // =========================================================
    // CHECK ATTEMPT OWNER
    // =========================================================

    private void checkOwner(
            McqTestAttempt attempt,
            Long studentId
    ) {

        if (studentId == null) {

            throw new SecurityException(
                    "Student ID is required"
            );
        }

        if (attempt.getStudent() == null
                || attempt.getStudent().getId() == null
                || !attempt.getStudent()
                .getId()
                .equals(studentId)) {

            throw new SecurityException(
                    "Attempt belongs to another student"
            );
        }
    }

    private void validateParentStudentOwnership(
            String username,
            Student student
    ) {

        if (username == null || username.trim().isEmpty()) {
            throw new SecurityException(
                    "Logged-in username is required"
            );
        }

        if (student == null
                || student.getParent() == null
                || student.getParent().getId() == null) {

            throw new SecurityException(
                    "Student is not linked to a parent"
            );
        }

        Parent parent = parents.findByUserUsername(username)
                .orElseThrow(() ->
                        new SecurityException(
                                "Parent profile not found"
                        )
                );

        if (!student.getParent()
                .getId()
                .equals(parent.getId())) {

            throw new SecurityException(
                    "Student does not belong to the logged-in parent"
            );
        }
    }

    // =========================================================
    // VALIDATE ANSWER OPTION
    // =========================================================

    private void validateAnswerOption(
            String answer
    ) {

        if (!Set.of("A", "B", "C", "D")
                .contains(answer)) {

            throw new IllegalArgumentException(
                    "Answer must be one of A, B, C or D"
            );
        }
    }

    // =========================================================
    // FIND TEST
    // =========================================================

    private McqTest findTest(Long id) {

        if (id == null) {

            throw new IllegalArgumentException(
                    "Test ID is required"
            );
        }

        return tests.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Test not found: " + id
                        )
                );
    }

    // =========================================================
    // FIND ATTEMPT
    // =========================================================

    private McqTestAttempt findAttempt(Long id) {

        if (id == null) {

            throw new IllegalArgumentException(
                    "Attempt ID is required"
            );
        }

        return attempts.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Attempt not found: " + id
                        )
                );
    }

    // =========================================================
    // FIND STUDENT
    // =========================================================

    private Student findStudent(Long id) {

        if (id == null) {

            throw new IllegalArgumentException(
                    "Student ID is required"
            );
        }

        return students.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Student not found: " + id
                        )
                );
    }

    // =========================================================
    // QUESTION POOL
    // =========================================================

    private List<McqQuestion> pool(
            McqTest test
    ) {

        if (test == null
                || test.getClassEntity() == null
                || test.getTestDate() == null
                || test.getSubject() == null) {

            return Collections.emptyList();
        }

        String subject =
                test.getSubject().trim();

        List<McqQuestion> pool =
                questions.findByQuestionDateAndClassEntityId(
                                test.getTestDate(),
                                test.getClassEntity().getId()
                        )
                        .stream()
                        .filter(Objects::nonNull)
                        .filter(question ->
                                question.getSubject() != null
                                        &&question.getSubject() != null
                                        && question.getSubject().getName() != null
                                        && question.getSubject().getName().trim().equalsIgnoreCase(subject)
                        )
                        .toList();

        /*
         * Existing-table limitation:
         *
         * McqTest does not store the selected question IDs.
         * Therefore we cannot permanently freeze the exact
         * questions selected during test creation.
         *
         * We return at most numberOfQuestions here.
         */
        if (test.getNumberOfQuestions() == null
                || test.getNumberOfQuestions() <= 0) {

            return Collections.emptyList();
        }

        List<McqQuestion> result =
                new ArrayList<>(pool);

        /*
         * Keep question order stable after creation/start
         * instead of reshuffling on every request.
         *
         * Since IDs are database-generated, sorting by ID
         * gives a stable order with the existing schema.
         */
        result.sort(
                Comparator.comparing(
                        McqQuestion::getId,
                        Comparator.nullsLast(
                                Comparator.naturalOrder()
                        )
                )
        );

        if (result.size()
                > test.getNumberOfQuestions()) {

            return result.subList(
                    0,
                    test.getNumberOfQuestions()
            );
        }

        return result;
    }

    // =========================================================
    // MAP TEST RESPONSE
    // =========================================================

    private McqTestResponseDto map(
            McqTest test,
            List<McqQuestion> questionList
    ) {

        return McqTestResponseDto.builder()

                .id(test.getId())

                .classId(
                        test.getClassEntity() != null
                                ? test.getClassEntity().getId()
                                : null
                )

                .sectionId(
                        test.getSection() != null
                                ? test.getSection().getId()
                                : null
                )

                .className(
                        test.getClassEntity() != null
                                ? test.getClassEntity().getName()
                                : null
                )

                .sectionName(
                        test.getSection() != null
                                ? test.getSection().getName()
                                : null
                )

                .subject(test.getSubject())

                .date(test.getTestDate())

                .startTime(test.getStartTime())

                .duration(test.getDuration())

                .numberOfQuestions(
                        test.getNumberOfQuestions()
                )

                .status(test.getStatus())

                .questions(
                        questionList
                                .stream()
                                .map(this::questionMap)
                                .toList()
                )

                .build();
    }

    // =========================================================
    // MAP QUESTION
    // =========================================================

    private McqTestQuestionDto questionMap(
            McqQuestion question
    ) {

        return McqTestQuestionDto.builder()

                .id(question.getId())

                .question(question.getQuestion())

                .optionA(question.getOptionA())

                .optionB(question.getOptionB())

                .optionC(question.getOptionC())

                .optionD(question.getOptionD())

                .marks(question.getMarks())

                .build();
    }

    // =========================================================
    // MAP ATTEMPT RESPONSE
    // =========================================================


// =========================================================
// MAP ATTEMPT RESPONSE
// =========================================================

    private McqAttemptResponseDto attemptMap(
            McqTestAttempt attempt,
            List<McqQuestion> questionList
    ) {

        return McqAttemptResponseDto.builder()

                .attemptId(attempt.getId())

                .testId(
                        attempt.getTest() != null
                                ? attempt.getTest().getId()
                                : null
                )

                // -------------------------------------------------
                // STUDENT DETAILS
                // -------------------------------------------------

                .studentId(
                        attempt.getStudent() != null
                                ? attempt.getStudent().getId()
                                : null
                )

                .studentName(
                        attempt.getStudent() != null
                                ? attempt.getStudent().getName()
                                : null
                )

                .admissionNo(
                        attempt.getStudent() != null
                                ? attempt.getStudent().getAdmissionNo()
                                : null
                )

                // -------------------------------------------------
                // STUDENT SECTION
                // IMPORTANT:
                // MCQ Test is Class + Subject wise.
                // Section comes from the STUDENT.
                // -------------------------------------------------

                .sectionId(
                        attempt.getStudent() != null
                                && attempt.getStudent().getSection() != null
                                ? attempt.getStudent().getSection().getId()
                                : null
                )

                .sectionName(
                        attempt.getStudent() != null
                                && attempt.getStudent().getSection() != null
                                ? attempt.getStudent().getSection().getName()
                                : null
                )

                // -------------------------------------------------
                // ATTEMPT DETAILS
                // -------------------------------------------------

                .status(attempt.getStatus())

                .startedAt(attempt.getStartedAt())

                .expiresAt(attempt.getExpiresAt())

                .submittedAt(attempt.getSubmittedAt())

                .score(attempt.getScore())

                .totalQuestions(
                        attempt.getTotalQuestions()
                )

                .correctAnswers(
                        attempt.getCorrectAnswers()
                )

                .wrongAnswers(
                        attempt.getWrongAnswers()
                )

                .percentage(
                        attempt.getPercentage()
                )

                // -------------------------------------------------
                // TEST DETAILS
                // -------------------------------------------------

                .test(
                        map(
                                attempt.getTest(),
                                questionList
                        )
                )

                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<McqAttemptResponseDto> reportPerformance(
            LocalDate date,
            Long classId,
            Long sectionId,
            String subject,
            Pageable pageable
    ) {

        String normalizedSubject =
                subject == null || subject.trim().isEmpty()
                        ? null
                        : subject.trim();

        Page<McqTestAttempt> attemptPage =
                attempts.findPerformanceReport(
                        date,
                        classId,
                        sectionId,
                        normalizedSubject,
                        null,
                        null,
                        pageable
                );

        return attemptPage.map(attempt ->
                attemptMap(
                        attempt,
                        pool(attempt.getTest())
                )
        );
    }

}

