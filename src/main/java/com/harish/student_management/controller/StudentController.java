package com.harish.student_management.controller;

import com.harish.student_management.dto.FileUploadResponseDto;
import com.harish.student_management.dto.StudentGenderSummaryDto;
import com.harish.student_management.dto.StudentRequestDto;
import com.harish.student_management.dto.StudentResponseDto;
import com.harish.student_management.entity.Student;
import com.harish.student_management.service.ExcelImportService;
import com.harish.student_management.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/students")
public class StudentController {

    @Autowired
    private StudentService service;

    @Autowired
    private ExcelImportService excelImportService;

    @Operation(summary = "Save a new student")
    @PostMapping
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public StudentResponseDto saveStudent(@Valid @RequestBody StudentRequestDto student){

        return service.saveStudent(student);

    }

    @Operation(summary = "Get all students")
    @GetMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','TEACHER')")
    public Page<StudentResponseDto> getAllStudents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "Id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection

    ){

        return service.getAllStudents(page,size,sortBy,sortDirection);

    }

    @Operation(summary = "Get student by ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','TEACHER')")
    public StudentResponseDto getStudent(@PathVariable Long id){

        return service.getStudentById(id);

    }

    @Operation(summary="Update student by ID")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public StudentResponseDto updateStudent(@Valid @PathVariable Long id,
                                 @RequestBody StudentRequestDto student){

        return service.updateStudent(id,student);

    }

    @Operation(summary = "Delete student by ID")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public String deleteStudent(@PathVariable Long id){

        service.deleteStudent(id);

        return "Student Deleted Successfully";

    }
    @Operation(summary = "Upload Document")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public FileUploadResponseDto uploadFile(@RequestParam("file") MultipartFile file)
            throws IOException {

        return service.uploadFile(file);
    }

    @Operation(summary = "Download Document")
    @GetMapping("/download/{fileName}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable String fileName) throws MalformedURLException {

        Resource resource = service.downloadFile(fileName);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + resource.getFilename() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @Operation(summary = "Delete Uploaded File")
    @DeleteMapping("/delete-file/{fileName}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<String> deleteFile(
            @PathVariable String fileName) throws IOException {

        return ResponseEntity.ok(service.deleteFile(fileName));
    }


    @PostMapping(
            value = "/import-excel",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<String> importExcel(
            @RequestParam("file") MultipartFile file) {

        return ResponseEntity.ok(
                excelImportService.importStudentsAndParents(file)
        );
    }

    @Operation(summary = "Get students by class ID")
    @GetMapping("/class/{classId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','TEACHER')")
    public List<StudentResponseDto> getStudentsByClassId(
            @PathVariable Long classId) {

        return service.getStudentsByClassId(classId);
    }

    @Operation(summary = "Get students by section ID")
    @GetMapping("/section/{sectionId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','TEACHER')")
    public List<StudentResponseDto> getStudentsBySectionId(
            @PathVariable Long sectionId) {

        return service.getStudentsBySectionId(sectionId);
    }

    @Operation(summary = "Get student gender summary")
    @GetMapping("/gender-summary")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public StudentGenderSummaryDto getStudentGenderSummary() {

        return service.getStudentGenderSummary();
    }

}
