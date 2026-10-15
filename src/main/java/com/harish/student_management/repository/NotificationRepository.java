package com.harish.student_management.repository;

import com.harish.student_management.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRecipientUserIdOrderByCreatedAtDesc(Long userId);

    List<Notification> findByRecipientRoleOrderByCreatedAtDesc(String role);

    List<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);

    long countByRecipientUserIdAndReadFalse(Long userId);
    List<Notification> findByTypeOrderByCreatedAtDesc(String type);
}
