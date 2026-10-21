
package com.harish.student_management.repository;

import com.harish.student_management.entity.StudentFee;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface StudentFeeRepository extends JpaRepository<StudentFee, Long> {

    // Existing methods retained for compatibility.
    // Avoid using these global/unscoped methods in school-admin operations.
    List<StudentFee> findByStudentId(Long studentId);

    List<StudentFee> findByStatus(String status);

    Optional<StudentFee> findByStudentIdAndFeeStructureId(
            Long studentId, Long structureId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM StudentFee f WHERE f.id = :id")
    Optional<StudentFee> findWithLockById(@Param("id") Long id);

    @Query("""
        SELECT COALESCE(SUM(f.totalAmount - f.paidAmount), 0)
        FROM StudentFee f
        WHERE f.totalAmount > f.paidAmount
        """)
    BigDecimal pendingAmount();

    @Query("""
        SELECT COALESCE(SUM(f.totalAmount), 0)
        FROM StudentFee f
        """)
    BigDecimal totalFeeAmount();

    @Query("""
        SELECT COALESCE(SUM(f.paidAmount), 0)
        FROM StudentFee f
        """)
    BigDecimal collectedFeeAmount();

    @Query("""
        SELECT f.student.id
        FROM StudentFee f
        WHERE f.feeStructure.id = :feeStructureId
        """)
    Set<Long> findStudentIdsByFeeStructureId(
            @Param("feeStructureId") Long feeStructureId);

    // School-scoped fee queries
    List<StudentFee> findAllByStudent_Section_ClassEntity_School_Id(
            Long schoolId);

    List<StudentFee> findByStudent_IdAndStudent_Section_ClassEntity_School_Id(
            Long studentId, Long schoolId);

    Optional<StudentFee> findByIdAndStudent_Section_ClassEntity_School_Id(
            Long id, Long schoolId);

    Optional<StudentFee>
    findByStudent_IdAndFeeStructure_IdAndStudent_Section_ClassEntity_School_Id(
            Long studentId, Long structureId, Long schoolId);

    // Lock a fee only when it belongs to the current school
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT f
        FROM StudentFee f
        WHERE f.id = :id
          AND f.student.section.classEntity.school.id = :schoolId
        """)
    Optional<StudentFee> findWithLockByIdAndSchoolId(
            @Param("id") Long id,
            @Param("schoolId") Long schoolId);

    // Total payable fees after applying discounts, limited to current school.
    @Query("""
    SELECT COALESCE(
        SUM(f.totalAmount - COALESCE(f.discountAmount, 0)), 0)
    FROM StudentFee f
    WHERE f.student.section.classEntity.school.id = :schoolId
    """)
    BigDecimal totalFeeAmountBySchool(
            @Param("schoolId") Long schoolId);

    @Query("""
        SELECT COALESCE(SUM(COALESCE(f.paidAmount, 0)), 0)
        FROM StudentFee f
        WHERE f.student.section.classEntity.school.id = :schoolId
        """)
    BigDecimal collectedFeeAmountBySchool(
            @Param("schoolId") Long schoolId);

    // Outstanding balance is based on discounted payable amount.
    @Query("""
        SELECT COALESCE(
            SUM(
                (f.totalAmount - COALESCE(f.discountAmount, 0))
                - COALESCE(f.paidAmount, 0)
            ), 0)
        FROM StudentFee f
        WHERE f.student.section.classEntity.school.id = :schoolId
          AND (f.totalAmount - COALESCE(f.discountAmount, 0))
              > COALESCE(f.paidAmount, 0)
        """)
    BigDecimal pendingAmountBySchool(
            @Param("schoolId") Long schoolId);

    // Find already-assigned students only within the specified school.
    @Query("""
        SELECT f.student.id
        FROM StudentFee f
        WHERE f.feeStructure.id = :feeStructureId
          AND f.student.section.classEntity.school.id = :schoolId
        """)
    Set<Long> findStudentIdsByFeeStructureIdAndSchoolId(
            @Param("feeStructureId") Long feeStructureId,
            @Param("schoolId") Long schoolId);
}
