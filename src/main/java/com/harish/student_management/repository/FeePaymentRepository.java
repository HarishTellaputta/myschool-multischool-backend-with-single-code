package com.harish.student_management.repository;

import com.harish.student_management.entity.FeePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.Query;

import java.time.*;
import java.util.*;

public interface FeePaymentRepository extends JpaRepository<FeePayment, Long> {
    List<FeePayment> findByStudentFeeStudentIdOrderByPaymentDateDesc(Long studentId);

    List<FeePayment> findByStudentFeeIdOrderByPaymentDateDesc(Long feeId);

    Page<FeePayment> findByPaymentDateBetween(LocalDate from, LocalDate to, Pageable pageable);

    @Query("""
    SELECT p
    FROM FeePayment p
    WHERE p.studentFee.paidAmount >= p.studentFee.totalAmount
    ORDER BY p.paymentDate DESC
    """)
    List<FeePayment> findFullyPaidPayments();
}
