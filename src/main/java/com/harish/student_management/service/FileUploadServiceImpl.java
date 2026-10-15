package com.harish.student_management.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class FileUploadServiceImpl implements FileUploadService {

    private final Path uploadPath =
            Paths.get("uploads", "mcq");

    @Override
    public String uploadMcqImage(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file is required");
        }

        String contentType = file.getContentType();

        if (contentType == null ||
                !(contentType.equals("image/jpeg")
                        || contentType.equals("image/png")
                        || contentType.equals("image/webp"))) {

            throw new IllegalArgumentException(
                    "Only JPG, PNG and WEBP images are allowed"
            );
        }

        try {
            Files.createDirectories(uploadPath);

            String originalName = file.getOriginalFilename();

            String extension = "";

            if (originalName != null && originalName.contains(".")) {
                extension = originalName.substring(
                        originalName.lastIndexOf(".")
                );
            }

            String fileName =
                    UUID.randomUUID() + extension;

            Path target =
                    uploadPath.resolve(fileName);

            Files.copy(
                    file.getInputStream(),
                    target,
                    StandardCopyOption.REPLACE_EXISTING
            );

            return "/uploads/mcq/" + fileName;

        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to upload image",
                    e
            );
        }
    }
}