
package com.harish.student_management.service;

import com.harish.student_management.dto.GroupChatMessageRequest;
import com.harish.student_management.dto.GroupChatMessageResponse;
import com.harish.student_management.dto.GroupChatReactionRequest;
import com.harish.student_management.dto.GroupChatReactionResponse;
import com.harish.student_management.entity.GroupChatMessage;
import com.harish.student_management.entity.GroupChatReaction;
import com.harish.student_management.entity.User;
import com.harish.student_management.repository.GroupChatMessageRepository;
import com.harish.student_management.repository.GroupChatReactionRepository;
import com.harish.student_management.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class GroupChatServiceImpl implements GroupChatService {

    private final GroupChatMessageRepository messageRepository;
    private final GroupChatReactionRepository reactionRepository;
    private final UserRepository userRepository;

    private static final Set<String> ALLOWED_ROLES =
            Set.of("SCHOOL_ADMIN", "TEACHER", "PARENT");

    private static final Set<String> ALLOWED_REACTIONS =
            Set.of("❤️", "👍", "😂", "😮", "😢", "👏", "🎉", "🎂");

    public GroupChatServiceImpl(
            GroupChatMessageRepository messageRepository,
            GroupChatReactionRepository reactionRepository,
            UserRepository userRepository
    ) {
        this.messageRepository = messageRepository;
        this.reactionRepository = reactionRepository;
        this.userRepository = userRepository;
    }

    private User currentUser() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Please log in"
            );
        }

        User user = userRepository
                .findByUsernameWithRole(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "User not found"
                ));

        String role = user.getRole() == null
                ? ""
                : user.getRole().getName();

        if (!ALLOWED_ROLES.contains(role)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not allowed to use the group chat"
            );
        }

        return user;
    }

    private GroupChatMessage findMessage(Long messageId) {
        return messageRepository.findByIdWithDetails(messageId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Message not found"
                ));
    }

    @Override
    public List<GroupChatMessageResponse> getMessages() {
        currentUser();

        List<GroupChatMessage> messages =
                messageRepository.findAllForGroupChat();

        List<GroupChatMessageResponse> responses = new ArrayList<>();

        for (GroupChatMessage message : messages) {
            responses.add(toResponse(message));
        }

        return responses;
    }

    @Override
    public GroupChatMessageResponse sendMessage(
            GroupChatMessageRequest request
    ) {
        User sender = currentUser();

        if (request.getMessage() == null
                || request.getMessage().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Message cannot be empty"
            );
        }

        GroupChatMessage message = new GroupChatMessage();
        message.setSender(sender);
        message.setMessage(request.getMessage().trim());

        if (request.getReplyToMessageId() != null) {
            GroupChatMessage replyTo =
                    findMessage(request.getReplyToMessageId());

            if (replyTo.isDeleted()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Cannot reply to a deleted message"
                );
            }

            message.setReplyToMessage(replyTo);
        }

        return toResponse(messageRepository.save(message));
    }

    @Override
    public GroupChatMessageResponse editMessage(
            Long messageId,
            GroupChatMessageRequest request
    ) {
        User user = currentUser();
        GroupChatMessage message = findMessage(messageId);

        if (message.isDeleted()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Deleted messages cannot be edited"
            );
        }

        if (!message.getSender().getId().equals(user.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You can edit only your own messages"
            );
        }

        if (request.getMessage() == null
                || request.getMessage().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Message cannot be empty"
            );
        }

        message.setMessage(request.getMessage().trim());
        message.setEdited(true);

        return toResponse(messageRepository.save(message));
    }

    @Override
    public void deleteMessage(Long messageId) {
        User user = currentUser();
        GroupChatMessage message = findMessage(messageId);

        String role = user.getRole().getName();
        boolean isOwner =
                message.getSender().getId().equals(user.getId());
        boolean isAdmin = "SCHOOL_ADMIN".equals(role);

        if (!isOwner && !isAdmin) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You can delete only your own messages"
            );
        }

        if (!message.isDeleted()) {
            message.setMessage("This message was deleted");
            message.setDeleted(true);
            messageRepository.save(message);
        }
    }

    @Override
    public GroupChatMessageResponse addReaction(
            Long messageId,
            GroupChatReactionRequest request
    ) {
        User user = currentUser();
        GroupChatMessage message = findMessage(messageId);

        if (message.isDeleted()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot react to a deleted message"
            );
        }

        String reaction = request.getReaction();

        if (reaction == null || !ALLOWED_REACTIONS.contains(reaction)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Unsupported reaction"
            );
        }

        boolean exists =
                reactionRepository
                        .existsByMessage_IdAndUser_IdAndReaction(
                                messageId, user.getId(), reaction
                        );

        if (!exists) {
            GroupChatReaction newReaction = new GroupChatReaction();
            newReaction.setMessage(message);
            newReaction.setUser(user);
            newReaction.setReaction(reaction);
            reactionRepository.save(newReaction);
        }

        return toResponse(findMessage(messageId));
    }

    @Override
    public void removeReaction(
            Long messageId,
            GroupChatReactionRequest request
    ) {
        User user = currentUser();
        findMessage(messageId);

        String reaction = request.getReaction();

        if (reaction == null || !ALLOWED_REACTIONS.contains(reaction)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Unsupported reaction"
            );
        }

        reactionRepository.deleteByMessage_IdAndUser_IdAndReaction(
                messageId, user.getId(), reaction
        );
    }

    private GroupChatMessageResponse toResponse(
            GroupChatMessage message
    ) {
        GroupChatMessageResponse response =
                new GroupChatMessageResponse();

        response.setId(message.getId());
        response.setSenderId(message.getSender().getId());
        response.setSenderName(message.getSender().getUsername());
        response.setSenderRole(
                message.getSender().getRole() == null
                        ? null
                        : message.getSender().getRole().getName()
        );

        response.setMessage(message.getMessage());
        response.setEdited(message.isEdited());
        response.setDeleted(message.isDeleted());
        response.setCreatedAt(message.getCreatedAt());
        response.setUpdatedAt(message.getUpdatedAt());

        GroupChatMessage reply = message.getReplyToMessage();

        if (reply != null) {
            response.setReplyToMessageId(reply.getId());
            response.setReplyToMessage(reply.getMessage());
            response.setReplyToSenderName(
                    reply.getSender().getUsername()
            );
        }

        List<GroupChatReactionResponse> reactionResponses =
                new ArrayList<>();

        List<GroupChatReaction> reactions =
                reactionRepository.findByMessage_IdIn(
                        List.of(message.getId())
                );

        for (GroupChatReaction reaction : reactions) {
            GroupChatReactionResponse reactionResponse =
                    new GroupChatReactionResponse(
                            reaction.getId(),
                            reaction.getUser().getId(),
                            reaction.getUser().getUsername(),
                            reaction.getReaction(),
                            reaction.getCreatedAt()
                    );

            reactionResponses.add(reactionResponse);
        }

        response.setReactions(reactionResponses);

        return response;
    }
}
