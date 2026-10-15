package com.harish.student_management.repository;

import com.harish.student_management.entity.StudentBusAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.*;
@Repository
public interface StudentBusAllocationRepository
        extends JpaRepository<StudentBusAllocation, Long> {

    List<StudentBusAllocation> findByStudentId(Long studentId);

    @Query("""
        select a.student.id
        from StudentBusAllocation a
        where a.status = 'ACTIVE'
    """)
    Set<Long> findActiveTransportStudentIds();
}
