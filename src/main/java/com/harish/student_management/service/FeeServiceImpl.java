
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

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
    private final CurrentSchoolService currentSchoolService;

    // ---------------------------------------------------------
    // CREATE FEE STRUCTURE
    // ---------------------------------------------------------

    @Override
    public Object createStructure(FeeStructureRequestDto r) {
        Long schoolId = currentSchoolService.getCurrentSchoolId();

        if (r.getName() == null || r.getName().isBlank()) {
            throw new IllegalArgumentException("Fee name is required");
        }

        ClassEntity c = classes.findByIdAndSchoolId(
                        r.getClassId(), schoolId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Class not found in current school"));

        AcademicYear y = r.getAcademicYearId() == null
                ? null
                : years.findByIdAndSchool_Id(
                        r.getAcademicYearId(), schoolId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Academic year not found in current school"));

        validateAmount(r.getAmount());

        boolean duplicate = structuresRepo
                .findAllByClassEntity_School_IdAndClassEntity_Id(
                        schoolId, c.getId())
                .stream()
                .anyMatch(existing ->
                        existing.getName().equalsIgnoreCase(r.getName().trim())
                                && sameId(
                                existing.getAcademicYear() == null
                                        ? null
                                        : existing.getAcademicYear().getId(),
                                y == null ? null : y.getId()));

        if (duplicate) {
            throw new IllegalArgumentException(
                    "Fee structure already exists for this class and academic year");
        }

        FeeStructure f = FeeStructure.builder()
                .name(r.getName().trim())
                .classEntity(c)
                .academicYear(y)
                .amount(currency(r.getAmount()))
                .dueDate(r.getDueDate())
                .status(normalizeStatus(r.getStatus()))
                .applied(false)
                .build();

        return structuresRepo.save(f);
    }

    // ---------------------------------------------------------
    // LIST FEE STRUCTURES
    // ---------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<FeeStructureResponseDto> structures(Long classId) {
        Long schoolId = currentSchoolService.getCurrentSchoolId();

        List<FeeStructure> result = classId == null
                ? structuresRepo.findAllByClassEntity_School_Id(schoolId)
                : structuresRepo
                .findAllByClassEntity_School_IdAndClassEntity_Id(
                        schoolId, classId);

        return result.stream()
                .map(f -> FeeStructureResponseDto.builder()
                        .id(f.getId())
                        .name(f.getName())
                        .classId(f.getClassEntity().getId())
                        .className(f.getClassEntity().getName())
                        .academicYearId(f.getAcademicYear() == null
                                ? null : f.getAcademicYear().getId())
                        .academicYearName(f.getAcademicYear() == null
                                ? null : f.getAcademicYear().getName())
                        .amount(f.getAmount())
                        .dueDate(f.getDueDate())
                        .status(f.getStatus())
                        .applied(f.isApplied())
                        .build())
                .toList();
    }

    // ---------------------------------------------------------
    // ASSIGN FEE TO A STUDENT
    // ---------------------------------------------------------

    @Override
    public StudentFeeResponseDto assign(StudentFeeAssignmentRequestDto r) {
        Long schoolId = currentSchoolService.getCurrentSchoolId();

        Student s = students.findByIdAndSection_ClassEntity_School_Id(
                        r.getStudentId(), schoolId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Student not found in current school"));

        if (s.getSection() == null
                || s.getSection().getClassEntity() == null) {
            throw new IllegalArgumentException(
                    "Student must belong to a class and section");
        }

        FeeStructure f = structuresRepo
                .findByIdAndClassEntity_School_Id(
                        r.getFeeStructureId(), schoolId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Fee structure not found in current school"));

        if (!s.getSection().getClassEntity().getId()
                .equals(f.getClassEntity().getId())) {
            throw new IllegalArgumentException(
                    "Fee structure does not apply to student's class");
        }

        if (f.getAcademicYear() != null
                && (s.getAcademicYear() == null
                || !f.getAcademicYear().getId()
                .equals(s.getAcademicYear().getId()))) {
            throw new IllegalArgumentException(
                    "Student academic year does not match fee structure");
        }

        if (feesRepo
                .findByStudent_IdAndFeeStructure_IdAndStudent_Section_ClassEntity_School_Id(
                        s.getId(), f.getId(), schoolId)
                .isPresent()) {
            throw new IllegalArgumentException(
                    "Fee already assigned to student");
        }

        BigDecimal amount = r.getTotalAmount() == null
                ? currency(f.getAmount())
                : currency(r.getTotalAmount());

        if (amount.signum() < 0) {
            throw new IllegalArgumentException(
                    "Fee amount cannot be negative");
        }

        StudentFee fee = StudentFee.builder()
                .student(s)
                .feeStructure(f)
                .totalAmount(amount)
                .paidAmount(BigDecimal.ZERO)
                .discountType("NONE")
                .discountValue(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .dueDate(r.getDueDate() == null
                        ? f.getDueDate() : r.getDueDate())
                .status(amount.signum() == 0 ? "PAID" : "PENDING")
                .build();

        return map(feesRepo.save(fee));
    }

    // ---------------------------------------------------------
    // STUDENT FEES / PENDING FEES
    // ---------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<StudentFeeResponseDto> fees(
            Long studentId, boolean pending) {

        Long schoolId = currentSchoolService.getCurrentSchoolId();

        List<StudentFee> result = studentId == null
                ? feesRepo.findAllByStudent_Section_ClassEntity_School_Id(
                schoolId)
                : feesRepo
                .findByStudent_IdAndStudent_Section_ClassEntity_School_Id(
                        studentId, schoolId);

        return result.stream()
                .filter(f -> !pending
                        || safePaid(f).compareTo(payableAmount(f)) < 0)
                .map(this::map)
                .toList();
    }

    // ---------------------------------------------------------
    // RECORD PAYMENT
    // ---------------------------------------------------------

    @Override
    public FeePaymentResponseDto pay(FeePaymentRequestDto r) {
        Long schoolId = currentSchoolService.getCurrentSchoolId();

        StudentFee f = feesRepo.findWithLockByIdAndSchoolId(
                        r.getStudentFeeId(), schoolId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Student fee not found in current school"));

        BigDecimal amount = currency(r.getAmount());

        if (amount.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Payment amount must be greater than zero");
        }

        BigDecimal paid = safePaid(f);
        BigDecimal payable = payableAmount(f);
        BigDecimal pending = payable.subtract(paid);

        if (pending.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Fee is already fully paid");
        }

        if (amount.compareTo(pending) > 0) {
            throw new IllegalArgumentException(
                    "Payment exceeds pending balance: " + pending);
        }

        f.setPaidAmount(paid.add(amount));
        f.setStatus(f.getPaidAmount().compareTo(payable) >= 0
                ? "PAID" : "PARTIAL");

        FeePayment payment = FeePayment.builder()
                .studentFee(f)
                .amount(amount)
                .paymentMethod(r.getPaymentMethod() == null
                        || r.getPaymentMethod().isBlank()
                        ? "CASH" : r.getPaymentMethod().trim())
                .remarks(r.getRemarks())
                .receiptNumber("RCT-" + UUID.randomUUID()
                        .toString().substring(0, 8).toUpperCase())
                .build();

        return paymentMap(paymentsRepo.save(payment));
    }

    // ---------------------------------------------------------
    // PAYMENT HISTORY
    // ---------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<FeePaymentResponseDto> payments(Long studentId) {
        Long schoolId = currentSchoolService.getCurrentSchoolId();

        List<FeePayment> result = studentId == null
                ? paymentsRepo.findAllBySchoolId(schoolId)
                : paymentsRepo.findPaymentsByStudentAndSchool(
                studentId, schoolId);

        return result.stream().map(this::paymentMap).toList();
    }

    // ---------------------------------------------------------
    // PAYMENT RECEIPT
    // ---------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public byte[] receipt(Long paymentId) {
        Long schoolId = currentSchoolService.getCurrentSchoolId();

        FeePayment p = paymentsRepo.findByIdAndSchoolId(
                        paymentId, schoolId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Payment not found in current school"));

        StudentFee f = p.getStudentFee();
        BigDecimal payable = payableAmount(f);
        BigDecimal pending = payable.subtract(safePaid(f))
                .max(BigDecimal.ZERO);

        String receipt = """
                FEE PAYMENT RECEIPT
                Receipt: %s
                Student: %s
                Fee: %s
                Original Fee: %s
                Discount: %s
                Payable Amount: %s
                This Payment: %s
                Total Paid: %s
                Pending Balance: %s
                Date: %s
                Method: %s
                """.formatted(
                p.getReceiptNumber(),
                f.getStudent().getName(),
                f.getFeeStructure().getName(),
                f.getTotalAmount(),
                safeDiscount(f),
                payable,
                p.getAmount(),
                safePaid(f),
                pending,
                p.getPaymentDate(),
                p.getPaymentMethod());

        return receipt.getBytes(StandardCharsets.UTF_8);
    }

    // ---------------------------------------------------------
    // DASHBOARD SUMMARY
    // ---------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public FeeDashboardSummaryDto dashboardSummary() {
        Long schoolId = currentSchoolService.getCurrentSchoolId();

        BigDecimal totalFee = zeroIfNull(
                feesRepo.totalFeeAmountBySchool(schoolId));
        BigDecimal collected = zeroIfNull(
                feesRepo.collectedFeeAmountBySchool(schoolId));
        BigDecimal pending = zeroIfNull(
                feesRepo.pendingAmountBySchool(schoolId));

        BigDecimal percentage = totalFee.signum() > 0
                ? collected.multiply(BigDecimal.valueOf(100))
                .divide(totalFee, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return FeeDashboardSummaryDto.builder()
                .totalFee(totalFee)
                .collected(collected)
                .pending(pending)
                .collectionPercentage(percentage)
                .build();
    }

    // ---------------------------------------------------------
    // APPLY FEE STRUCTURE TO CLASS STUDENTS
    // ---------------------------------------------------------

    @Override
    public void applyStructure(Long structureId) {
        Long schoolId = currentSchoolService.getCurrentSchoolId();

        FeeStructure f = structuresRepo
                .findByIdAndClassEntity_School_Id(
                        structureId, schoolId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Fee structure not found in current school"));

        List<Student> classStudents =
                students.findBySection_ClassEntity_IdAndSection_ClassEntity_School_Id(
                        f.getClassEntity().getId(), schoolId);

        Set<Long> alreadyAssigned =
                feesRepo.findStudentIdsByFeeStructureIdAndSchoolId(
                        structureId, schoolId);

        for (Student s : classStudents) {
            if (s.getSection() == null
                    || s.getSection().getClassEntity() == null) {
                continue;
            }

            if (f.getAcademicYear() != null
                    && (s.getAcademicYear() == null
                    || !f.getAcademicYear().getId()
                    .equals(s.getAcademicYear().getId()))) {
                continue;
            }

            if (alreadyAssigned.contains(s.getId())) {
                continue;
            }

            BigDecimal amount = currency(f.getAmount());

            StudentFee studentFee = StudentFee.builder()
                    .student(s)
                    .feeStructure(f)
                    .totalAmount(amount)
                    .paidAmount(BigDecimal.ZERO)
                    .discountType("NONE")
                    .discountValue(BigDecimal.ZERO)
                    .discountAmount(BigDecimal.ZERO)
                    .dueDate(f.getDueDate())
                    .status(amount.signum() == 0 ? "PAID" : "PENDING")
                    .build();

            feesRepo.save(studentFee);
        }

        f.setApplied(true);
        structuresRepo.save(f);
    }

    // ---------------------------------------------------------
    // PAYMENT REPORT
    // ---------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public Page<FeePaymentResponseDto> getPaymentReport(
            LocalDate from, LocalDate to, Pageable pageable) {

        if (from == null || to == null || pageable == null) {
            throw new IllegalArgumentException(
                    "From date, to date and pageable are required");
        }

        if (to.isBefore(from)) {
            throw new IllegalArgumentException(
                    "End date must not precede start date");
        }

        Long schoolId = currentSchoolService.getCurrentSchoolId();

        return paymentsRepo.findPaymentReportBySchool(
                        from, to, schoolId, pageable)
                .map(this::paymentMap);
    }

    // ---------------------------------------------------------
    // FULLY PAID PAYMENTS
    // ---------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<FeePaymentResponseDto> paidPayments() {
        Long schoolId = currentSchoolService.getCurrentSchoolId();

        return paymentsRepo.findFullyPaidPaymentsBySchool(schoolId)
                .stream()
                .map(this::paymentMap)
                .toList();
    }

    // ---------------------------------------------------------
    // APPLY DISCOUNT
    // ---------------------------------------------------------

    @Override
    public StudentFeeResponseDto applyDiscount(
            Long studentFeeId, FeeDiscountRequestDto request) {

        Long schoolId = currentSchoolService.getCurrentSchoolId();

        if (request == null || request.getDiscountType() == null) {
            throw new IllegalArgumentException(
                    "Discount type is required");
        }

        StudentFee f = feesRepo.findWithLockByIdAndSchoolId(
                        studentFeeId, schoolId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Student fee not found in current school"));

        String type = request.getDiscountType()
                .trim().toUpperCase(Locale.ROOT);

        BigDecimal value = currency(request.getDiscountValue());

        if (value.signum() < 0) {
            throw new IllegalArgumentException(
                    "Discount value cannot be negative");
        }

        BigDecimal total = currency(f.getTotalAmount());
        BigDecimal discount;

        switch (type) {
            case "FIXED" -> {
                if (value.compareTo(total) > 0) {
                    throw new IllegalArgumentException(
                            "Fixed discount cannot exceed the original fee");
                }
                discount = value;
            }
            case "PERCENTAGE" -> {
                if (value.compareTo(BigDecimal.valueOf(100)) > 0) {
                    throw new IllegalArgumentException(
                            "Percentage discount cannot exceed 100");
                }

                discount = total.multiply(value)
                        .divide(BigDecimal.valueOf(100),
                                2, RoundingMode.HALF_UP);
            }
            default -> throw new IllegalArgumentException(
                    "Discount type must be FIXED or PERCENTAGE");
        }

        BigDecimal newPayable = total.subtract(discount);
        BigDecimal paid = safePaid(f);

        if (newPayable.compareTo(paid) < 0) {
            throw new IllegalArgumentException(
                    "Discount cannot make payable amount less than "
                            + "the amount already paid");
        }

        f.setDiscountType(type);
        f.setDiscountValue(value);
        f.setDiscountAmount(discount);
        f.setDiscountReason(request.getDiscountReason());

        f.setStatus(paid.compareTo(newPayable) >= 0
                ? "PAID"
                : paid.signum() > 0 ? "PARTIAL" : "PENDING");

        // Store the authenticated username for the audit trail.
        // CurrentSchoolService can be extended to expose this if needed.
        f.setDiscountApprovedBy("SCHOOL_ADMIN");

        return map(feesRepo.save(f));
    }

    // ---------------------------------------------------------
    // UPDATE FEE STRUCTURE
    // ---------------------------------------------------------

    @Override
    public Object updateStructure(
            Long structureId, FeeStructureRequestDto r) {

        Long schoolId = currentSchoolService.getCurrentSchoolId();

        FeeStructure f = structuresRepo
                .findByIdAndClassEntity_School_Id(
                        structureId, schoolId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Fee structure not found in current school"));

        ClassEntity c = classes.findByIdAndSchoolId(
                        r.getClassId(), schoolId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Class not found in current school"));

        AcademicYear y = r.getAcademicYearId() == null
                ? null
                : years.findByIdAndSchool_Id(
                        r.getAcademicYearId(), schoolId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Academic year not found in current school"));

        if (r.getName() == null || r.getName().isBlank()) {
            throw new IllegalArgumentException("Fee name is required");
        }

        validateAmount(r.getAmount());

        boolean duplicate = structuresRepo
                .findAllByClassEntity_School_IdAndClassEntity_Id(
                        schoolId, c.getId())
                .stream()
                .anyMatch(existing ->
                        !existing.getId().equals(f.getId())
                                && existing.getName().equalsIgnoreCase(
                                r.getName().trim())
                                && sameId(
                                existing.getAcademicYear() == null
                                        ? null
                                        : existing.getAcademicYear().getId(),
                                y == null ? null : y.getId()));

        if (duplicate) {
            throw new IllegalArgumentException(
                    "Another fee structure already exists for this class and academic year");
        }

        f.setName(r.getName().trim());
        f.setClassEntity(c);
        f.setAcademicYear(y);
        f.setAmount(currency(r.getAmount()));
        f.setDueDate(r.getDueDate());
        f.setStatus(normalizeStatus(r.getStatus()));

        return structuresRepo.save(f);
    }

    // ---------------------------------------------------------
    // ENABLE / RE-APPLY STRUCTURE
    // ---------------------------------------------------------

    @Override
    public void enableStructure(Long structureId) {
        Long schoolId = currentSchoolService.getCurrentSchoolId();

        FeeStructure f = structuresRepo
                .findByIdAndClassEntity_School_Id(
                        structureId, schoolId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Fee structure not found in current school"));

        f.setApplied(false);
        structuresRepo.save(f);
    }

    // ---------------------------------------------------------
    // HELPERS
    // ---------------------------------------------------------

    private BigDecimal currency(BigDecimal value) {
        if (value == null) {
            throw new IllegalArgumentException("Amount is required");
        }

        try {
            return value.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException(
                    "Amount must have at most two decimal places");
        }
    }

    private void validateAmount(BigDecimal amount) {
        BigDecimal value = currency(amount);

        if (value.signum() < 0) {
            throw new IllegalArgumentException(
                    "Amount cannot be negative");
        }
    }

    private BigDecimal safePaid(StudentFee f) {
        return zeroIfNull(f.getPaidAmount());
    }

    private BigDecimal safeDiscount(StudentFee f) {
        return zeroIfNull(f.getDiscountAmount());
    }

    private BigDecimal payableAmount(StudentFee f) {
        return currency(f.getTotalAmount())
                .subtract(safeDiscount(f))
                .max(BigDecimal.ZERO);
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private boolean sameId(Long first, Long second) {
        return first == null ? second == null : first.equals(second);
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "ACTIVE";
        }

        String normalized = status.trim().toUpperCase(Locale.ROOT);

        if (!normalized.equals("ACTIVE")
                && !normalized.equals("INACTIVE")) {
            throw new IllegalArgumentException(
                    "Status must be ACTIVE or INACTIVE");
        }

        return normalized;
    }

    private StudentFeeResponseDto map(StudentFee f) {
        BigDecimal total = currency(f.getTotalAmount());
        BigDecimal discount = safeDiscount(f);
        BigDecimal paid = safePaid(f);
        BigDecimal payable = total.subtract(discount)
                .max(BigDecimal.ZERO);

        return StudentFeeResponseDto.builder()
                .id(f.getId())
                .studentId(f.getStudent().getId())
                .studentName(f.getStudent().getName())
                .feeStructureId(f.getFeeStructure().getId())
                .feeName(f.getFeeStructure().getName())
                .totalAmount(total)
                .paidAmount(paid)
                .pendingAmount(payable.subtract(paid)
                        .max(BigDecimal.ZERO))
                .discountType(f.getDiscountType())
                .discountValue(f.getDiscountValue())
                .discountAmount(discount)
                .payableAmount(payable)
                .discountReason(f.getDiscountReason())
                .dueDate(f.getDueDate())
                .status(f.getStatus())
                .build();
    }

    private FeePaymentResponseDto paymentMap(FeePayment p) {
        return FeePaymentResponseDto.builder()
                .id(p.getId())
                .studentFeeId(p.getStudentFee().getId())
                .studentId(p.getStudentFee().getStudent().getId())
                .studentName(p.getStudentFee().getStudent().getName())
                .receiptNumber(p.getReceiptNumber())
                .amount(p.getAmount())
                .paymentDate(p.getPaymentDate())
                .paymentMethod(p.getPaymentMethod())
                .remarks(p.getRemarks())
                .build();
    }
}
