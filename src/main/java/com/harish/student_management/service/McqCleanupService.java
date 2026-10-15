
        package com.harish.student_management.service;

import com.harish.student_management.entity.McqTest;
import com.harish.student_management.entity.McqTestAttempt;
import com.harish.student_management.entity.McqQuestion;

import com.harish.student_management.repository.McqTestRepository;
import com.harish.student_management.repository.McqTestAttemptRepository;
import com.harish.student_management.repository.McqTestAnswerRepository;
import com.harish.student_management.repository.McqQuestionRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class McqCleanupService {

    private final McqTestRepository testRepository;
    private final McqTestAttemptRepository attemptRepository;
    private final McqTestAnswerRepository answerRepository;
    private final McqQuestionRepository questionRepository;

    public McqCleanupService(
            McqTestRepository testRepository,
            McqTestAttemptRepository attemptRepository,
            McqTestAnswerRepository answerRepository,
            McqQuestionRepository questionRepository) {

        this.testRepository = testRepository;
        this.attemptRepository = attemptRepository;
        this.answerRepository = answerRepository;
        this.questionRepository = questionRepository;
    }

    @Transactional
    public void deleteYesterdayPostedTests() {

        ZoneId zone = ZoneId.of("Asia/Kolkata");

        LocalDate yesterday = LocalDate.now(zone).minusDays(1);

        LocalDateTime start = yesterday.atStartOfDay();
        LocalDateTime end = yesterday.plusDays(1).atStartOfDay();

        System.out.println(
                "MCQ cleanup started for date: " + yesterday);

        // 1. Find yesterday's tests
        List<McqTest> tests =
                testRepository
                        .findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                                start, end);

        // 2. Delete answers and attempts before deleting tests
        for (McqTest test : tests) {

            List<McqTestAttempt> attempts =
                    attemptRepository.findByTestId(test.getId());

            for (McqTestAttempt attempt : attempts) {

                answerRepository.deleteAll(
                        answerRepository.findByAttemptId(attempt.getId()));
            }

            attemptRepository.deleteAll(attempts);

            testRepository.delete(test);
        }

        // 3. Find questions created yesterday
        List<McqQuestion> questions =
                questionRepository
                        .findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                                start, end);

        // 4. Delete only questions not referenced by remaining answers
        int deletedQuestions = 0;

        for (McqQuestion question : questions) {

            if (answerRepository
                    .findByQuestionId(question.getId())
                    .isEmpty()) {

                questionRepository.delete(question);
                deletedQuestions++;
            }
        }

        System.out.println(
                "Deleted MCQ tests: " + tests.size());

        System.out.println(
                "Deleted MCQ questions: " + deletedQuestions);

        System.out.println(
                "MCQ cleanup completed for date: " + yesterday);
    }
}
