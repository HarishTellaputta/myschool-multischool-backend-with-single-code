package com.harish.student_management.service;

import com.harish.student_management.dto.*;

import java.util.*;

public interface SchoolNoticeService {
    SchoolNoticeResponseDto create(SchoolNoticeRequestDto r);

    SchoolNoticeResponseDto update(Long id, SchoolNoticeRequestDto r);

    SchoolNoticeResponseDto publish(Long id, boolean publish);

    void delete(Long id);

    List<SchoolNoticeResponseDto> relevant(String role, Long classId, Long sectionId);
}
