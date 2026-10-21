package com.harish.student_management.repository;

import com.harish.student_management.entity.SchoolNotice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SchoolNoticeRepository
        extends JpaRepository<SchoolNotice, Long> {

    List<SchoolNotice>
    findBySchool_IdAndStatusOrderByPublishedAtDescIdDesc(
            Long schoolId,
            String status
    );

    Optional<SchoolNotice> findByIdAndSchool_Id(
            Long id,
            Long schoolId
    );
}