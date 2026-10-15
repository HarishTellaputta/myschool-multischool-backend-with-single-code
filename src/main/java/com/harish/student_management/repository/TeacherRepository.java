package com.harish.student_management.repository;

import com.harish.student_management.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    Optional<Teacher> findByEmployeeId(String employeeId);

    Optional<Teacher> findByUserId(Long userId);

    boolean existsByEmployeeId(String employeeId);

    boolean existsByUserId(Long userId);

    List<Teacher> findBySchoolId(Long schoolId);

    List<Teacher> findByStatus(String status);

    Optional<Teacher> findByIdAndSchoolId(Long id, Long schoolId);

    Optional<Teacher> findByNameIgnoreCase(String name);

    Optional<Teacher> findByUser_Username(String username);
}