
package com.harish.student_management.repository;

import com.harish.student_management.entity.ParentFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ParentFeedbackRepository
        extends JpaRepository<ParentFeedback, Long> {

    List<ParentFeedback> findAllByOrderByCreatedAtDesc();
    List<ParentFeedback> findAllByParent_User_School_IdOrderByCreatedAtDesc(
            Long schoolId
    );

    Optional<ParentFeedback> findByIdAndParent_User_School_Id(
            Long feedbackId,
            Long schoolId
    );
}
