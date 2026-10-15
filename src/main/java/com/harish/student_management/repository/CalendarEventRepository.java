package com.harish.student_management.repository;

import com.harish.student_management.entity.CalendarEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.*;
import java.util.*;

public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {
    List<CalendarEvent> findByStartDateLessThanEqualAndEndDateGreaterThanEqual(LocalDate end, LocalDate start);

    List<CalendarEvent> findByEventType(String eventType);

    List<CalendarEvent> findByClassEntityId(Long classId);

    long countByStartDateLessThanEqualAndEndDateGreaterThanEqualAndStatus(LocalDate end, LocalDate start, String status);
}
