package com.harish.student_management.service;

import com.harish.student_management.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.*;

public interface FeeService {
    Object createStructure(FeeStructureRequestDto r);

    List<FeeStructureResponseDto> structures(Long classId);

    StudentFeeResponseDto assign(StudentFeeAssignmentRequestDto r);

    List<StudentFeeResponseDto> fees(Long studentId, boolean pending);

    FeePaymentResponseDto pay(FeePaymentRequestDto r);

    List<FeePaymentResponseDto> payments(Long studentId);

    byte[] receipt(Long paymentId);
    FeeDashboardSummaryDto dashboardSummary();
    void applyStructure(Long structureId);

    Page<FeePaymentResponseDto> getPaymentReport(
            LocalDate from,
            LocalDate to,
            Pageable pageable
    );

    List<FeePaymentResponseDto> paidPayments();
    Object updateStructure(Long structureId, FeeStructureRequestDto r);

    void enableStructure(Long structureId);
}
