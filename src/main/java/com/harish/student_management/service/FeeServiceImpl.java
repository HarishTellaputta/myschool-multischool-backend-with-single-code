package com.harish.student_management.service;

import com.harish.student_management.dto.*;
import com.harish.student_management.entity.*;
import com.harish.student_management.exception.ResourceNotFoundException;
import com.harish.student_management.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class FeeServiceImpl implements FeeService {
    private final FeeStructureRepository structuresRepo;
    private final StudentFeeRepository feesRepo;
    private final FeePaymentRepository paymentsRepo;
    private final ClassRepository classes;
    private final AcademicYearRepository years;
    private final StudentRepository students;


    @Override
    public Object createStructure(FeeStructureRequestDto r) {

        ClassEntity c = classes.findById(r.getClassId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Class not found: " + r.getClassId()
                        ));

        AcademicYear y = r.getAcademicYearId() == null
                ? null
                : years.findById(r.getAcademicYearId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Academic year not found: "
                                        + r.getAcademicYearId()
                        ));

        // Prevent duplicate fee structures
        if (y != null &&
                structuresRepo.existsByNameAndClassEntityIdAndAcademicYearId(
                        r.getName().trim(),
                        r.getClassId(),
                        y.getId()
                )) {

            throw new IllegalArgumentException(
                    "Fee structure already exists for this class and academic year"
            );
        }

        return structuresRepo.save(
                FeeStructure.builder()
                        .name(r.getName().trim())
                        .classEntity(c)
                        .academicYear(y)
                        .amount(currency(r.getAmount()))
                        .dueDate(r.getDueDate())
                        .status(
                                r.getStatus() == null
                                        ? "ACTIVE"
                                        : r.getStatus()
                        )
                        .applied(false)
                        .build()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeeStructureResponseDto> structures(Long classId) {

        List<FeeStructure> structures =
                classId == null
                        ? structuresRepo.findAll()
                        : structuresRepo.findByClassEntityId(classId);

        return structures.stream()
                .map(f -> FeeStructureResponseDto.builder()
                        .id(f.getId())
                        .name(f.getName())

                        .classId(
                                f.getClassEntity() != null
                                        ? f.getClassEntity().getId()
                                        : null
                        )
                        .className(
                                f.getClassEntity() != null
                                        ? f.getClassEntity().getName()
                                        : null
                        )

                        .academicYearId(
                                f.getAcademicYear() != null
                                        ? f.getAcademicYear().getId()
                                        : null
                        )
                        .academicYearName(
                                f.getAcademicYear() != null
                                        ? f.getAcademicYear().getName()
                                        : null
                        )

                        .amount(f.getAmount())
                        .dueDate(f.getDueDate())
                        .status(f.getStatus())
                        .applied(f.isApplied())

                        .build())
                .toList();
    }
    @Override
    public StudentFeeResponseDto assign(StudentFeeAssignmentRequestDto r) {
        Student s = students.findById(r.getStudentId()).orElseThrow(() -> new ResourceNotFoundException("Student not found: " + r.getStudentId()));
        FeeStructure f = structuresRepo.findById(r.getFeeStructureId()).orElseThrow(() -> new ResourceNotFoundException("Fee structure not found: " + r.getFeeStructureId()));
        if (s.getSection() == null || !s.getSection().getClassEntity().getId().equals(f.getClassEntity().getId()))
            throw new IllegalArgumentException("Fee structure does not apply to student's class");
        if (f.getAcademicYear() != null && s.getAcademicYear() != null && !f.getAcademicYear().getId().equals(s.getAcademicYear().getId()))
            throw new IllegalArgumentException("Fee structure does not apply to student's academic year");
        if (feesRepo.findByStudentIdAndFeeStructureId(s.getId(), f.getId()).isPresent())
            throw new IllegalArgumentException("Fee already assigned to student");
        return map(feesRepo.save(StudentFee.builder().student(s).feeStructure(f).totalAmount(r.getTotalAmount() == null ? f.getAmount() : currency(r.getTotalAmount())).paidAmount(BigDecimal.ZERO).dueDate(r.getDueDate() == null ? f.getDueDate() : r.getDueDate()).status("PENDING").build()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentFeeResponseDto> fees(Long studentId, boolean pending) {
        List<StudentFee> fs = studentId == null ? feesRepo.findAll() : feesRepo.findByStudentId(studentId);
        return fs.stream().filter(f -> !pending || safePaid(f).compareTo(f.getTotalAmount()) < 0).map(this::map).toList();
    }

    @Override
    public FeePaymentResponseDto pay(FeePaymentRequestDto r) {
        StudentFee f = feesRepo.findWithLockById(r.getStudentFeeId()).orElseThrow(() -> new ResourceNotFoundException("Student fee not found: " + r.getStudentFeeId()));
        BigDecimal amount = currency(r.getAmount());
        BigDecimal paid = f.getPaidAmount() == null ? BigDecimal.ZERO : currency(f.getPaidAmount());
        BigDecimal total = currency(f.getTotalAmount());
        BigDecimal pending = total.subtract(paid);
        if (pending.signum() <= 0) throw new IllegalArgumentException("Fee is already fully paid");
        if (amount.compareTo(pending) > 0) throw new IllegalArgumentException("Payment exceeds pending fee");
        f.setPaidAmount(paid.add(amount));
        f.setStatus(f.getPaidAmount().compareTo(total) >= 0 ? "PAID" : "PARTIAL");
        FeePayment p = paymentsRepo.save(FeePayment.builder().studentFee(f).amount(amount).paymentMethod(r.getPaymentMethod() == null ? "CASH" : r.getPaymentMethod()).remarks(r.getRemarks()).receiptNumber("RCT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()).build());
        return paymentMap(p);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeePaymentResponseDto> payments(Long studentId) {
        return (studentId == null ? paymentsRepo.findAll() : paymentsRepo.findByStudentFeeStudentIdOrderByPaymentDateDesc(studentId)).stream().map(this::paymentMap).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] receipt(Long id) {
        FeePayment p = paymentsRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + id));
        return ("FEE PAYMENT RECEIPT\nReceipt: " + p.getReceiptNumber() + "\nStudent: " + p.getStudentFee().getStudent().getName() + "\nAmount: " + p.getAmount() + "\nDate: " + p.getPaymentDate() + "\nMethod: " + p.getPaymentMethod() + "\n").getBytes(StandardCharsets.UTF_8);
    }

    private BigDecimal currency(BigDecimal value) {
        if (value == null) throw new IllegalArgumentException("Amount is required");
        try {
            return value.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException("Amount must have at most two decimal places");
        }
    }

    private BigDecimal safePaid(StudentFee f) {
        return f.getPaidAmount() == null ? BigDecimal.ZERO : f.getPaidAmount();
    }

    private StudentFeeResponseDto map(StudentFee f) {
        BigDecimal total = f.getTotalAmount() == null ? BigDecimal.ZERO : f.getTotalAmount();
        BigDecimal paid = safePaid(f);
        return StudentFeeResponseDto.builder().id(f.getId()).studentId(f.getStudent().getId()).studentName(f.getStudent().getName()).feeStructureId(f.getFeeStructure().getId()).feeName(f.getFeeStructure().getName()).totalAmount(total).paidAmount(paid).pendingAmount(total.subtract(paid)).dueDate(f.getDueDate()).status(f.getStatus()).build();
    }

    private FeePaymentResponseDto paymentMap(FeePayment p) {
        return FeePaymentResponseDto.builder().id(p.getId()).studentFeeId(p.getStudentFee().getId()).studentId(p.getStudentFee().getStudent().getId()).studentName(p.getStudentFee().getStudent().getName()).receiptNumber(p.getReceiptNumber()).amount(p.getAmount()).paymentDate(p.getPaymentDate()).paymentMethod(p.getPaymentMethod()).remarks(p.getRemarks()).build();
    }

 @Override
 @Transactional(readOnly = true)
 public FeeDashboardSummaryDto dashboardSummary() {

  BigDecimal totalFee = feesRepo.totalFeeAmount();
  BigDecimal collected = feesRepo.collectedFeeAmount();
  BigDecimal pending = feesRepo.pendingAmount();

  BigDecimal percentage = BigDecimal.ZERO;

  if (totalFee.compareTo(BigDecimal.ZERO) > 0) {
   percentage = collected
           .multiply(BigDecimal.valueOf(100))
           .divide(totalFee, 2, RoundingMode.HALF_UP);
  }

  return FeeDashboardSummaryDto.builder()
          .totalFee(totalFee)
          .collected(collected)
          .pending(pending)
          .collectionPercentage(percentage)
          .build();
 }

    @Override
    public void applyStructure(Long structureId) {

        FeeStructure f = structuresRepo.findById(structureId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Fee structure not found: " + structureId
                        ));

        List<Student> classStudents =
                students.findBySection_ClassEntity_Id(
                        f.getClassEntity().getId()
                );

        Set<Long> alreadyAssigned =
                feesRepo.findStudentIdsByFeeStructureId(structureId);

        for (Student s : classStudents) {

            // Academic year match
            if (f.getAcademicYear() != null
                    && s.getAcademicYear() != null
                    && !f.getAcademicYear().getId()
                    .equals(s.getAcademicYear().getId())) {
                continue;
            }

            // Already assigned → skip
            if (alreadyAssigned.contains(s.getId())) {
                continue;
            }

            StudentFee studentFee = StudentFee.builder()
                    .student(s)
                    .feeStructure(f)
                    .totalAmount(currency(f.getAmount()))
                    .paidAmount(BigDecimal.ZERO)
                    .dueDate(f.getDueDate())
                    .status("PENDING")
                    .build();

            feesRepo.save(studentFee);
        }

        // Mark fee structure as applied
        f.setApplied(true);
        structuresRepo.save(f);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FeePaymentResponseDto> getPaymentReport(
            LocalDate from,
            LocalDate to,
            Pageable pageable) {

        if (to.isBefore(from)) {
            throw new IllegalArgumentException(
                    "End date must not precede start date"
            );
        }

        return paymentsRepo
                .findByPaymentDateBetween(from, to, pageable)
                .map(this::paymentMap);
    }


    @Override
    @Transactional(readOnly = true)
    public List<FeePaymentResponseDto> paidPayments() {
        return paymentsRepo
                .findFullyPaidPayments()
                .stream()
                .map(this::paymentMap)
                .toList();
    }

    @Override
    public Object updateStructure(
            Long structureId,
            FeeStructureRequestDto r) {

        FeeStructure f = structuresRepo.findById(structureId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Fee structure not found: " + structureId
                        ));

        ClassEntity c = classes.findById(r.getClassId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Class not found: " + r.getClassId()
                        ));

        AcademicYear y = r.getAcademicYearId() == null
                ? null
                : years.findById(r.getAcademicYearId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Academic year not found: "
                                        + r.getAcademicYearId()
                        ));

        f.setName(r.getName().trim());
        f.setClassEntity(c);
        f.setAcademicYear(y);
        f.setAmount(currency(r.getAmount()));
        f.setDueDate(r.getDueDate());
        f.setStatus(
                r.getStatus() == null
                        ? "ACTIVE"
                        : r.getStatus()
        );

        return structuresRepo.save(f);
    }

    @Override
    public void enableStructure(Long structureId) {

        FeeStructure f = structuresRepo.findById(structureId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Fee structure not found: " + structureId
                        ));

        f.setApplied(false);

        structuresRepo.save(f);
    }
}
