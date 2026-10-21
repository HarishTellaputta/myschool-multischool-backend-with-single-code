package com.harish.student_management.repository;

import com.harish.student_management.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SectionRepository extends JpaRepository<Section, Long> {

    List<Section> findByClassEntityId(Long classId);

    List<Section> findByClassEntityIdAndName(Long classId, String name);

    Optional<Section> findByIdAndClassEntityId(Long id, Long classId);

    Optional<Section> findByNameIgnoreCaseAndClassEntityId(
            String name,
            Long classId
    );

    @Query("""
    SELECT s
    FROM Section s
    JOIN FETCH s.classEntity c
    JOIN FETCH c.school
    WHERE s.id = :sectionId
""")
    Optional<Section> findByIdWithClassAndSchool(
            @Param("sectionId") Long sectionId
    );

    Optional<Section> findByNameIgnoreCaseAndClassEntityIdAndClassEntity_School_Id(
            String name,
            Long classId,
            Long schoolId
    );

    @Query("""
    SELECT s FROM Section s
    JOIN FETCH s.classEntity c
    WHERE c.school.id = :schoolId
    ORDER BY c.name, s.name
    """)
    List<Section> findAllBySchoolId(
            @Param("schoolId") Long schoolId
    );

    @Query("""
    SELECT s FROM Section s
    JOIN FETCH s.classEntity c
    WHERE s.id = :sectionId
      AND c.school.id = :schoolId
    """)
    Optional<Section> findByIdAndSchoolId(
            @Param("sectionId") Long sectionId,
            @Param("schoolId") Long schoolId
    );

    @Query("""
    SELECT s FROM Section s
    JOIN FETCH s.classEntity c
    WHERE c.id = :classId
      AND c.school.id = :schoolId
    ORDER BY s.name
    """)
    List<Section> findByClassIdAndSchoolId(
            @Param("classId") Long classId,
            @Param("schoolId") Long schoolId
    );
}