package com.harish.student_management.repository;

import com.harish.student_management.entity.ClassEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface ClassRepository extends JpaRepository<ClassEntity, Long> {

    Optional<ClassEntity> findByCode(String code);

    boolean existsByCode(String code);

    List<ClassEntity> findBySchoolId(Long schoolId);

    Optional<ClassEntity> findByIdAndSchoolId(Long id, Long schoolId);

    Optional<ClassEntity> findByNameIgnoreCase(String name);

    Optional<ClassEntity> findByNameIgnoreCaseAndSchoolId(
            String name,
            Long schoolId
    );
    Optional<ClassEntity> findByNameIgnoreCaseAndSchool_Id(
            String name,
            Long schoolId
    );


    Optional<ClassEntity> findBySchoolIdAndCode(Long schoolId, String code);

    boolean existsBySchoolIdAndCode(Long schoolId, String code);

    Optional<ClassEntity> findByIdAndSchool_Id(
            Long id,
            Long schoolId
    );

    @Query("""
    SELECT c
    FROM ClassEntity c
    JOIN FETCH c.school
    WHERE c.school.id = :schoolId
    """)
    List<ClassEntity> findBySchoolIdWithSchool(
            @Param("schoolId") Long schoolId
    );



}
