
package com.harish.student_management.repository;

import com.harish.student_management.entity.FeePayment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FeePaymentRepository
        extends JpaRepository<FeePayment, Long> {

    // --------------------------------------------------
    // Existing methods - backward compatibility
    // --------------------------------------------------

    List<FeePayment> findByStudentFeeStudentIdOrderByPaymentDateDesc(
            Long studentId
    );

    List<FeePayment> findByStudentFeeIdOrderByPaymentDateDesc(
            Long feeId
    );

    Page<FeePayment> findByPaymentDateBetween(
            LocalDate from,
            LocalDate to,
            Pageable pageable
    );

    @Query("""
        SELECT p
        FROM FeePayment p
        WHERE p.studentFee.paidAmount >= p.studentFee.totalAmount
        ORDER BY p.paymentDate DESC
        """)
    List<FeePayment> findFullyPaidPayments();


    // --------------------------------------------------
    // MULTISCHOOL: Fetch all payments for one school
    // --------------------------------------------------

    @Query("""
        SELECT p
        FROM FeePayment p
        WHERE p.studentFee.student.section.classEntity.school.id = :schoolId
        ORDER BY p.paymentDate DESC
        """)
    List<FeePayment> findAllBySchoolId(
            @Param("schoolId") Long schoolId
    );


    // --------------------------------------------------
    // MULTISCHOOL: Fetch payment by ID and school
    // --------------------------------------------------

    @Query("""
        SELECT p
        FROM FeePayment p
        WHERE p.id = :paymentId
          AND p.studentFee.student.section.classEntity.school.id = :schoolId
        """)
    Optional<FeePayment> findByIdAndSchoolId(
            @Param("paymentId") Long paymentId,
            @Param("schoolId") Long schoolId
    );


    // --------------------------------------------------
    // MULTISCHOOL: Fetch payments for a student
    // --------------------------------------------------

    @Query("""
        SELECT p
        FROM FeePayment p
        WHERE p.studentFee.student.id = :studentId
          AND p.studentFee.student.section.classEntity.school.id = :schoolId
        ORDER BY p.paymentDate DESC
        """)
    List<FeePayment> findPaymentsByStudentAndSchool(
            @Param("studentId") Long studentId,
            @Param("schoolId") Long schoolId
    );


    // --------------------------------------------------
    // MULTISCHOOL: Payment report by date range
    // --------------------------------------------------

    @Query("""
        SELECT p
        FROM FeePayment p
        WHERE p.paymentDate BETWEEN :from AND :to
          AND p.studentFee.student.section.classEntity.school.id = :schoolId
        """)
    Page<FeePayment> findPaymentReportBySchool(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("schoolId") Long schoolId,
            Pageable pageable
    );


    // --------------------------------------------------
    // MULTISCHOOL + DISCOUNT: Fully paid fee payments
    // --------------------------------------------------

    @Query("""
        SELECT p
        FROM FeePayment p
        WHERE p.studentFee.student.section.classEntity.school.id = :schoolId
          AND p.studentFee.paidAmount >=
              (
                  p.studentFee.totalAmount
                  - COALESCE(p.studentFee.discountAmount, 0)
              )
        ORDER BY p.paymentDate DESC
        """)
    List<FeePayment> findFullyPaidPaymentsBySchool(
            @Param("schoolId") Long schoolId
    );


}
