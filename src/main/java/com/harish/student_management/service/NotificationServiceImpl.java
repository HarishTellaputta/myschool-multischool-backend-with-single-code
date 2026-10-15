package com.harish.student_management.service;

import com.harish.student_management.dto.*;
import com.harish.student_management.entity.*;
import com.harish.student_management.exception.ResourceNotFoundException;
import com.harish.student_management.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository repo;
    private final UserRepository users;
    private final ParentRepository parents;

    @Override
    public void publish(NotificationRequestDto r) {
        if (r.getRecipientUserId() == null && r.getRecipientRole() == null && r.getRecipientId() == null)
            throw new IllegalArgumentException("A notification recipient is required");
        User u = r.getRecipientUserId() == null ? null : users.findById(r.getRecipientUserId()).orElseThrow(() -> new ResourceNotFoundException("Recipient user not found: " + r.getRecipientUserId()));
        repo.save(Notification.builder().type(r.getType().trim().toUpperCase()).title(r.getTitle()).message(r.getMessage()).payload(r.getPayload()).recipientUser(u).recipientRole(r.getRecipientRole() == null ? null : r.getRecipientRole().trim().toUpperCase()).recipientId(r.getRecipientId()).read(false).build());
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponseDto> forUser(String username) {

        User u = users.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Parent p = parents.findByUserId(u.getId()).orElse(null);

        List<Notification> all = new ArrayList<>(
                repo.findByRecipientUserIdOrderByCreatedAtDesc(u.getId())
        );

        if (u.getRole() != null) {
            roleAliases(u.getRole().getName())
                    .forEach(r -> all.addAll(
                            repo.findByRecipientRoleOrderByCreatedAtDesc(r)
                    ));
        }

        return all.stream()
                .filter(n -> isAddressedTo(n, u, p))
                .collect(Collectors.toMap(
                        Notification::getId,
                        n -> n,
                        (a, b) -> a
                ))
                .values()
                .stream()
                .sorted(Comparator.comparing(
                        Notification::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .map(this::map)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationResponseDto get(Long id, String username) {

        Notification n = find(id);
        User u = user(username);

        Parent p = parents.findByUserId(u.getId()).orElse(null);

        if (!isAddressedTo(n, u, p)) {
            throw new SecurityException("Notification is not addressed to this user");
        }

        return map(n);
    }

    @Override
    public NotificationResponseDto markRead(Long id, String username) {

        Notification n = find(id);
        User u = user(username);

        Parent p = parents.findByUserId(u.getId()).orElse(null);

        if (!isAddressedTo(n, u, p)) {
            throw new SecurityException("Notification is not addressed to this user");
        }

        n.setRead(true);
        n.setReadAt(LocalDateTime.now());

        return map(n);
    }
    @Override
    @Transactional(readOnly = true)
    public long unread(String username) {
        return forUser(username).stream().filter(n -> !Boolean.TRUE.equals(n.getRead())).count();
    }

    private User user(String username) {
        return users.findByUsername(username).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private Set<String> roleAliases(String role) {
        String r = role.toUpperCase();
        return Set.of(r, r.endsWith("S") ? r.substring(0, r.length() - 1) : r + "S");
    }

    private Notification find(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + id));
    }

    private boolean isAddressedTo(Notification n, User u, Parent p) {
        if (n.getRecipientUser() != null && n.getRecipientUser().getId().equals(u.getId())) return true;
        if (n.getRecipientRole() == null || u.getRole() == null || !roleAliases(u.getRole().getName()).contains(n.getRecipientRole().toUpperCase()))
            return false;
        return n.getRecipientId() == null || (p != null && n.getRecipientId().equals(p.getId()));
    }

    private NotificationResponseDto map(Notification n) {
        return NotificationResponseDto.builder().id(n.getId()).type(n.getType()).title(n.getTitle()).message(n.getMessage()).payload(n.getPayload()).recipientUserId(n.getRecipientUser() == null ? null : n.getRecipientUser().getId()).recipientRole(n.getRecipientRole()).recipientId(n.getRecipientId()).read(n.getRead()).createdAt(n.getCreatedAt()).readAt(n.getReadAt()).build();
    }

    @Override
    public NotificationResponseDto createNotice(NotificationRequestDto r) {

        Notification notification = Notification.builder()
                .type("SCHOOL_NOTICE")
                .title(r.getTitle().trim())
                .message(r.getMessage())
                .category(r.getCategory())
                .audience(r.getAudience())
                .status(
                        r.getStatus() == null
                                ? "DRAFT"
                                : r.getStatus().trim().toUpperCase()
                )
                .read(false)
                .build();

        if ("PUBLISHED".equals(notification.getStatus())) {
            notification.setPublishedAt(LocalDateTime.now());
        }

        return map(repo.save(notification));
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponseDto> adminNotices() {

        return repo.findByTypeOrderByCreatedAtDesc("SCHOOL_NOTICE")
                .stream()
                .map(this::map)
                .toList();
    }

    @Override
    public NotificationResponseDto updateNotice(
            Long id,
            NotificationRequestDto r
    ) {

        Notification notification = find(id);

        if (!"SCHOOL_NOTICE".equals(notification.getType())) {
            throw new IllegalArgumentException("Not a school notice");
        }

        notification.setTitle(r.getTitle().trim());
        notification.setMessage(r.getMessage());
        notification.setCategory(r.getCategory());
        notification.setAudience(r.getAudience());

        if (r.getStatus() != null) {
            String status = r.getStatus().trim().toUpperCase();

            notification.setStatus(status);

            if ("PUBLISHED".equals(status)
                    && notification.getPublishedAt() == null) {

                notification.setPublishedAt(LocalDateTime.now());
            }
        }

        return map(repo.save(notification));
    }

    @Override
    public void deleteNotice(Long id) {

        Notification notification = find(id);

        if (!"SCHOOL_NOTICE".equals(notification.getType())) {
            throw new IllegalArgumentException("Not a school notice");
        }

        repo.delete(notification);
    }
    @Override
    public NotificationResponseDto publishNotice(Long id) {

        Notification notification = find(id);

        if (!"SCHOOL_NOTICE".equals(notification.getType())) {
            throw new IllegalArgumentException("Not a school notice");
        }

        notification.setStatus("PUBLISHED");
        notification.setPublishedAt(LocalDateTime.now());

        return map(repo.save(notification));
    }
}
