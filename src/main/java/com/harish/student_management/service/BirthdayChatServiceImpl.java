package com.harish.student_management.service;

import com.harish.student_management.dto.*;
import com.harish.student_management.entity.*;
import com.harish.student_management.exception.ResourceNotFoundException;
import com.harish.student_management.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class BirthdayChatServiceImpl
        implements BirthdayChatService {

    private final BirthdayChatMessageRepository messageRepository;

    private final BirthdayChatReactionRepository reactionRepository;

    private final StudentRepository studentRepository;

    private final ParentRepository parentRepository;

    private final TeacherRepository teacherRepository;

    private final UserRepository userRepository;

    // =========================================================
    // SHOW MESSAGES
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<BirthdayChatMessageResponse> getMessages(
            String username,
            Long studentId
    ) {

        User user =
                userRepository
                        .findByUsername(username)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found"
                                )
                        );

        Student student =
                studentRepository.findById(studentId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Student not found"
                                )
                        );

        return messageRepository
                .findMessagesByStudentId(student.getId())
                .stream()
                .map(message ->
                        toResponse(
                                message,
                                user.getId()
                        )
                )
                .toList();
    }

    // =========================================================
    // SEND MESSAGE
    // =========================================================

    @Override
    public BirthdayChatMessageResponse sendMessage(
            String username,
            Long studentId,
            BirthdayChatMessageRequest request
    ) {

        User sender =
                userRepository
                        .findByUsername(username)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found"
                                )
                        );

        Student student =
                studentRepository.findById(studentId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Student not found"
                                )
                        );

        BirthdayChatMessage replyTo = null;

        if (request.getReplyToMessageId() != null) {

            replyTo =
                    messageRepository
                            .findById(
                                    request.getReplyToMessageId()
                            )
                            .orElseThrow(() ->
                                    new ResourceNotFoundException(
                                            "Reply message not found"
                                    )
                            );

            if (!replyTo.getStudent()
                    .getId()
                    .equals(student.getId())) {

                throw new IllegalArgumentException(
                        "Reply message does not belong to this student"
                );
            }
        }

        BirthdayChatMessage message =
                BirthdayChatMessage.builder()
                        .student(student)
                        .sender(sender)
                        .message(request.getMessage().trim())
                        .replyToMessage(replyTo)
                        .edited(false)
                        .deleted(false)
                        .build();

        BirthdayChatMessage saved =
                messageRepository.save(message);

        return toResponse(
                saved,
                sender.getId()
        );
    }

    // =========================================================
    // EDIT
    // =========================================================

    @Override
    public BirthdayChatMessageResponse editMessage(
            String username,
            Long messageId,
            BirthdayChatMessageUpdateRequest request
    ) {

        User user =
                userRepository
                        .findByUsername(username)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found"
                                )
                        );

        BirthdayChatMessage message =
                messageRepository
                        .findById(messageId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Message not found"
                                )
                        );

        if (!message.getSender()
                .getId()
                .equals(user.getId())) {

            throw new SecurityException(
                    "You can edit only your own message"
            );
        }

        if (Boolean.TRUE.equals(message.getDeleted())) {
            throw new IllegalArgumentException(
                    "Deleted message cannot be edited"
            );
        }

        message.setMessage(
                request.getMessage().trim()
        );

        message.setEdited(true);

        BirthdayChatMessage updated =
                messageRepository.save(message);

        return toResponse(
                updated,
                user.getId()
        );
    }

    // =========================================================
    // DELETE
    // =========================================================

    @Override
    public void deleteMessage(
            String username,
            Long messageId
    ) {

        User user =
                userRepository
                        .findByUsername(username)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found"
                                )
                        );

        BirthdayChatMessage message =
                messageRepository
                        .findById(messageId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Message not found"
                                )
                        );

        if (!message.getSender()
                .getId()
                .equals(user.getId())) {

            throw new SecurityException(
                    "You can delete only your own message"
            );
        }

        message.setDeleted(true);
        message.setMessage(
                "This message was deleted"
        );

        messageRepository.save(message);
    }

    // =========================================================
    // REPLY
    // =========================================================

    @Override
    public BirthdayChatMessageResponse replyToMessage(
            String username,
            Long messageId,
            BirthdayChatMessageRequest request
    ) {

        User user =
                userRepository
                        .findByUsername(username)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found"
                                )
                        );

        BirthdayChatMessage original =
                messageRepository
                        .findById(messageId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Message not found"
                                )
                        );

        if (Boolean.TRUE.equals(original.getDeleted())) {
            throw new IllegalArgumentException(
                    "Cannot reply to a deleted message"
            );
        }

        BirthdayChatMessage reply =
                BirthdayChatMessage.builder()
                        .student(original.getStudent())
                        .sender(user)
                        .message(request.getMessage().trim())
                        .replyToMessage(original)
                        .edited(false)
                        .deleted(false)
                        .build();

        BirthdayChatMessage saved =
                messageRepository.save(reply);

        return toResponse(
                saved,
                user.getId()
        );
    }

    // =========================================================
    // ADD REACTION
    // =========================================================

    @Override
    public BirthdayChatMessageResponse addReaction(
            String username,
            Long messageId,
            BirthdayChatReactionRequest request
    ) {

        User user =
                userRepository
                        .findByUsername(username)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found"
                                )
                        );

        BirthdayChatMessage message =
                messageRepository
                        .findById(messageId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Message not found"
                                )
                        );

        if (Boolean.TRUE.equals(
                message.getDeleted()
        )) {

            throw new IllegalArgumentException(
                    "Cannot react to a deleted message"
            );
        }

        if (!isAllowedReaction(
                request.getReaction()
        )) {

            throw new IllegalArgumentException(
                    "Invalid reaction"
            );
        }

        Optional<BirthdayChatReaction> existing =
                reactionRepository
                        .findByMessageIdAndUserIdAndReaction(
                                messageId,
                                user.getId(),
                                request.getReaction()
                        );

        if (existing.isEmpty()) {

            BirthdayChatReaction reaction =
                    BirthdayChatReaction.builder()
                            .message(message)
                            .user(user)
                            .reaction(
                                    request.getReaction()
                            )
                            .build();

            try {
                reactionRepository.save(reaction);
            } catch (DataIntegrityViolationException ignored) {
                // Already reacted
            }
        }

        return toResponse(
                message,
                user.getId()
        );
    }

    // =========================================================
    // REMOVE REACTION
    // =========================================================

    @Override
    public void removeReaction(
            String username,
            Long messageId,
            BirthdayChatReactionRequest request
    ) {

        User user =
                userRepository
                        .findByUsername(username)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found"
                                )
                        );

        BirthdayChatMessage message =
                messageRepository
                        .findById(messageId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Message not found"
                                )
                        );

        if (!isAllowedReaction(
                request.getReaction()
        )) {

            throw new IllegalArgumentException(
                    "Invalid reaction"
            );
        }

        Optional<BirthdayChatReaction> existing =
                reactionRepository
                        .findByMessageIdAndUserIdAndReaction(
                                messageId,
                                user.getId(),
                                request.getReaction()
                        );

        existing.ifPresent(
                reactionRepository::delete
        );
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private Parent getParentFromUsername(
            String username
    ) {

        User user =
                userRepository
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
                                "Parent profile not found"
                        )
                );
    }

    private Student getStudentForParent(
            Long studentId,
            Parent parent
    ) {

        Student student =
                studentRepository.findById(studentId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Student not found"
                                )
                        );

        if (student.getParent() == null ||
                !student.getParent()
                        .getId()
                        .equals(parent.getId())) {

            throw new SecurityException(
                    "You are not authorized to access this student"
            );
        }

        return student;
    }

    private BirthdayChatMessage getMessageForParent(
            Long messageId,
            Parent parent
    ) {

        return messageRepository
                .findByIdAndParentId(
                        messageId,
                        parent.getId()
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Message not found"
                        )
                );
    }

    private boolean isAllowedReaction(
            String reaction
    ) {

        return switch (reaction) {
            case "❤️",
                 "👍",
                 "😂",
                 "😮",
                 "😢",
                 "👏",
                 "🎉",
                 "🎂" -> true;

            default -> false;
        };
    }

    // =========================================================
    // RESOLVE SENDER NAME
    // =========================================================

    private String resolveSenderName(
            User sender
    ) {

        if (sender == null) {
            return "Unknown User";
        }

        /*
         * Role IDs from your roles table:
         *
         * 1 = SCHOOL_ADMIN
         * 2 = TEACHER
         * 3 = PARENT
         * 4 = STUDENT
         * 5 = EMPLOYEE
         * 6 = DRIVER
         */

        if (sender.getRole() == null) {
            return "User";
        }

        Long roleId =
                sender.getRole().getId();

        // =====================================================
        // SCHOOL ADMIN
        // =====================================================

        if (Long.valueOf(1L).equals(roleId)) {

            return "School Admin";
        }

        // =====================================================
        // TEACHER
        // =====================================================

        if (Long.valueOf(2L).equals(roleId)) {

            return teacherRepository
                    .findByUserId(sender.getId())
                    .map(Teacher::getName)
                    .filter(name ->
                            name != null &&
                                    !name.trim().isEmpty()
                    )
                    .orElse("Teacher");
        }

        // =====================================================
        // PARENT
        // =====================================================

        if (Long.valueOf(3L).equals(roleId)) {

            Optional<Parent> parent =
                    parentRepository.findByUserId(
                            sender.getId()
                    );

            if (parent.isPresent()) {

                Parent profile =
                        parent.get();

                String relationship =
                        profile.getRelationship();

                if (relationship != null) {

                    relationship =
                            relationship
                                    .trim()
                                    .toUpperCase();

                    // -----------------------------------------
                    // MOTHER
                    // -----------------------------------------

                    if ("MOTHER".equals(relationship)
                            && profile.getMotherName() != null
                            && !profile.getMotherName()
                            .trim()
                            .isEmpty()) {

                        return profile.getMotherName()
                                .trim();
                    }

                    // -----------------------------------------
                    // GUARDIAN
                    // -----------------------------------------

                    if ("GUARDIAN".equals(relationship)
                            && profile.getGuardianName() != null
                            && !profile.getGuardianName()
                            .trim()
                            .isEmpty()) {

                        return profile.getGuardianName()
                                .trim();
                    }
                }

                // ---------------------------------------------
                // FATHER / DEFAULT PARENT
                // ---------------------------------------------

                if (profile.getFatherName() != null
                        && !profile.getFatherName()
                        .trim()
                        .isEmpty()) {

                    return profile.getFatherName()
                            .trim();
                }
            }

            return "Parent";
        }

        // =====================================================
        // OTHER ROLES
        // =====================================================

        if (Long.valueOf(4L).equals(roleId)) {
            return "Student";
        }

        if (Long.valueOf(5L).equals(roleId)) {
            return "Employee";
        }

        if (Long.valueOf(6L).equals(roleId)) {
            return "Driver";
        }

        return "User";
    }

    // =========================================================
    // RESPONSE
    // =========================================================

    private BirthdayChatMessageResponse toResponse(
            BirthdayChatMessage message,
            Long currentUserId
    ) {

        List<BirthdayChatReactionResponse> reactions =
                reactionRepository
                        .findByMessageId(message.getId())
                        .stream()
                        .collect(
                                java.util.stream.Collectors
                                        .groupingBy(
                                                BirthdayChatReaction::getReaction
                                        )
                        )
                        .entrySet()
                        .stream()
                        .map(entry ->
                                BirthdayChatReactionResponse
                                        .builder()
                                        .reaction(
                                                entry.getKey()
                                        )
                                        .count(
                                                entry.getValue()
                                                        .size()
                                        )
                                        .reactedByCurrentUser(
                                                entry.getValue()
                                                        .stream()
                                                        .anyMatch(
                                                                r ->
                                                                        r.getUser()
                                                                                .getId()
                                                                                .equals(
                                                                                        currentUserId
                                                                                )
                                                        )
                                        )
                                        .build()
                        )
                        .toList();

        String replyMessage = null;

        Long replyId = null;

        if (message.getReplyToMessage() != null) {

            replyId =
                    message.getReplyToMessage()
                            .getId();

            replyMessage =
                    message.getReplyToMessage()
                            .getMessage();
        }

        User sender =
                message.getSender();

        String senderName =
                resolveSenderName(sender);

        return BirthdayChatMessageResponse
                .builder()
                .id(message.getId())
                .studentId(
                        message.getStudent().getId()
                )
                .senderId(
                        sender.getId()
                )
                .senderName(
                        senderName
                )
                .message(
                        message.getMessage()
                )
                .edited(
                        Boolean.TRUE.equals(
                                message.getEdited()
                        )
                )
                .deleted(
                        Boolean.TRUE.equals(
                                message.getDeleted()
                        )
                )
                .createdAt(
                        message.getCreatedAt()
                )
                .updatedAt(
                        message.getUpdatedAt()
                )
                .replyToMessageId(
                        replyId
                )
                .replyToMessage(
                        replyMessage
                )
                .reactions(
                        reactions
                )
                .build();
    }
}