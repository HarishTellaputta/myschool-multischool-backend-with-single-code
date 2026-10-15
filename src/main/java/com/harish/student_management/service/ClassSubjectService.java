package com.harish.student_management.service;


import com.harish.student_management.dto.ClassSubjectResponse;

import java.util.List;

public interface ClassSubjectService {

    ClassSubjectResponse assignSubjectToClass(
            Long classId,
            Long subjectId
    );

    List<ClassSubjectResponse> getSubjectsByClass(
            Long classId
    );

    void removeSubjectFromClass(
            Long classId,
            Long subjectId
    );
}