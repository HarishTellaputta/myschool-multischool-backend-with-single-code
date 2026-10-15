package com.harish.student_management.service;

import com.harish.student_management.dto.ReportCardDto;

public interface ReportCardService {
    ReportCardDto generate(Long studentId, Long examinationId, boolean includeUnpublished);
    byte[] download(Long studentId, Long examinationId, boolean includeUnpublished);
}
