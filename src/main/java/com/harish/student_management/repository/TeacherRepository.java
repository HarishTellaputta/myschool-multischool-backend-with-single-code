
package com.harish.student_management.repository;

import com.harish.student_management.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    // School-scoped methods

    List<Teacher> findAllBySchool_Id(Long schoolId);

    List<Teacher> findAllBySchool_IdAndStatusIgnoreCase(
            Long schoolId,
            String status
    );

    Optional<Teacher> findByUser_IdAndSchool_Id(
            Long userId,
            Long schoolId
    );

    boolean existsByUser_IdAndSchool_Id(
            Long userId,
            Long schoolId
    );

    boolean existsByIdAndSchool_Id(Long id, Long schoolId);

    @Query("""
        SELECT t
        FROM Teacher t
        JOIN FETCH t.user u
        WHERE t.id = :teacherId
          AND t.school.id = :schoolId
        """)
    Optional<Teacher> findByIdAndSchoolIdWithUser(
            @Param("teacherId") Long teacherId,
            @Param("schoolId") Long schoolId
    );

    @Query("""
        SELECT t
        FROM Teacher t
        WHERE t.user.username = :username
          AND t.school.id = :schoolId
        """)
    Optional<Teacher> findByUsernameAndSchoolId(
            @Param("username") String username,
            @Param("schoolId") Long schoolId
    );
    List<Teacher> findAllByOrderByIdAsc();


    Optional<Teacher> findByIdAndSchool_Id(
            Long teacherId,
            Long schoolId
    );

    Optional<Teacher> findByUser_UsernameAndSchool_Id(
            String username,
            Long schoolId
    );


}
