package com.harish.student_management.service;

import com.harish.student_management.dto.*;

import java.util.*;

public interface NotificationService extends NotificationPublisher {

    List<NotificationResponseDto> forUser(String username);

    NotificationResponseDto get(Long id, String username);

    NotificationResponseDto markRead(Long id, String username);

    long unread(String username);

    // Admin
    List<NotificationResponseDto> adminNotices();

    NotificationResponseDto createNotice(NotificationRequestDto request);

    NotificationResponseDto updateNotice(
            Long id,
            NotificationRequestDto request
    );

    void deleteNotice(Long id);

    NotificationResponseDto publishNotice(Long id);
}