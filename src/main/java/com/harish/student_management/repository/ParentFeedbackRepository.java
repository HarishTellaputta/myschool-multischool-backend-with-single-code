
package com.harish.student_management.repository;

import com.harish.student_management.entity.ParentFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParentFeedbackRepository
        extends JpaRepository<ParentFeedback, Long> {

    List<ParentFeedback> findAllByOrderByCreatedAtDesc();
}
