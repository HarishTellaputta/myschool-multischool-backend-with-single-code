package com.harish.student_management.service;


import com.harish.student_management.dto.SubjectImportResponseDto;
import com.harish.student_management.dto.SubjectRequest;
import com.harish.student_management.dto.SubjectResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface SubjectService {

    SubjectResponse createSubject(SubjectRequest request);

    List<SubjectResponse> getAllSubjects();

    List<SubjectResponse> getSubjectsBySchool(Long schoolId);

    List<SubjectResponse> getActiveSubjectsBySchool(Long schoolId);

    SubjectResponse getSubjectById(Long id);

    SubjectResponse updateSubject(Long id, SubjectRequest request);

    void deleteSubject(Long id);
    List<SubjectResponse> getSubjectsByClass(Long classId);

    SubjectImportResponseDto importSubjects(
            MultipartFile file,
            Long schoolId
    ) throws IOException;
}