
        package com.harish.student_management.repository;

import com.harish.student_management.entity.SchoolNotice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SchoolNoticeRepository
        extends JpaRepository<SchoolNotice, Long> {

    List<SchoolNotice> findByStatusOrderByPublishedAtDescIdDesc(
            String status
    );
}
