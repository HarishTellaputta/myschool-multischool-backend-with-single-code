package com.harish.student_management.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileUploadService {

    String uploadMcqImage(MultipartFile file);
}