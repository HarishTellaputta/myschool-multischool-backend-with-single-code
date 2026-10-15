package com.harish.student_management.repository;

import com.harish.student_management.entity.StudentFee;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.*;
import java.math.*;

public interface StudentFeeRepository extends JpaRepository<StudentFee, Long> {
    List<StudentFee> findByStudentId(Long studentId);

    List<StudentFee> findByStatus(String status);

    Optional<StudentFee> findByStudentIdAndFeeStructureId(Long studentId, Long structureId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from StudentFee f where f.id = :id")
    Optional<StudentFee> findWithLockById(@Param("id") Long id);

    @Query("select coalesce(sum(f.totalAmount-f.paidAmount),0) from StudentFee f where f.totalAmount>f.paidAmount")
    BigDecimal pendingAmount();

    @Query("""
        select coalesce(sum(f.totalAmount), 0)
        from StudentFee f
    """)
    BigDecimal totalFeeAmount();

    @Query("""
        select coalesce(sum(f.paidAmount), 0)
        from StudentFee f
    """)
    BigDecimal collectedFeeAmount();

    @Query("""
    select f.student.id
    from StudentFee f
    where f.feeStructure.id = :feeStructureId
""")
    Set<Long> findStudentIdsByFeeStructureId(@Param("feeStructureId") Long feeStructureId);
}
