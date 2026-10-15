package com.harish.student_management.controller;

import com.harish.student_management.dto.*;
import com.harish.student_management.service.FeeService;
import com.harish.student_management.service.FeeStructureExcelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/api/v1/fees")
@RequiredArgsConstructor
public class FeeController {
    private final FeeService service;
    private final FeeStructureExcelService feeStructureExcelService;

    @PostMapping("/structures")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<?> structure(@Valid @RequestBody FeeStructureRequestDto r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createStructure(r));
    }

    @GetMapping("/structures")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PARENT')")
    public List<FeeStructureResponseDto> structures(
            @RequestParam(required = false) Long classId) {

        return service.structures(classId);
    }

    @PostMapping("/assign")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public StudentFeeResponseDto assign(@Valid @RequestBody StudentFeeAssignmentRequestDto r) {
        return service.assign(r);
    }

    @GetMapping("/students/{studentId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PARENT')")
    public List<StudentFeeResponseDto> fees(@PathVariable Long studentId, @RequestParam(defaultValue = "false") boolean pending) {
        return service.fees(studentId, pending);
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public List<StudentFeeResponseDto> pending() {
        return service.fees(null, true);
    }

    @PostMapping("/payments")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<FeePaymentResponseDto> payment(@Valid @RequestBody FeePaymentRequestDto r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.pay(r));
    }

    @GetMapping("/payments")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PARENT')")
    public List<FeePaymentResponseDto> payments(@RequestParam(required = false) Long studentId) {
        return service.payments(studentId);
    }

    @GetMapping("/payments/{id}/receipt")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PARENT')")
    public ResponseEntity<ByteArrayResource> receipt(@PathVariable Long id) {
        byte[] b = service.receipt(id);
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=receipt-" + id + ".txt").contentLength(b.length).body(new ByteArrayResource(b));
    }

    @GetMapping("/dashboard-summary")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public FeeDashboardSummaryDto dashboardSummary() {
        return service.dashboardSummary();
    }

    @PostMapping("/structures/{structureId}/apply")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<?> applyStructure(
            @PathVariable Long structureId) {

        service.applyStructure(structureId);

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Fee structure applied to eligible students successfully"
                )
        );
    }

    @GetMapping("/payments/paid")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PARENT')")
    public List<FeePaymentResponseDto> paidPayments() {
        return service.paidPayments();
    }

    @PostMapping(
            value = "/structures/import",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<FeeStructureImportResultDto> importFeeStructures(
            @RequestParam("file") MultipartFile file
    ) {

        FeeStructureImportResultDto result =
                feeStructureExcelService.importExcel(file);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/structures/template")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ByteArrayResource> downloadFeeStructureTemplate() {

        byte[] data = feeStructureExcelService.generateTemplate();

        ByteArrayResource resource =
                new ByteArrayResource(data);

        return ResponseEntity.ok()
                .contentType(
                        MediaType.parseMediaType(
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                        )
                )
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=fee-structure-template.xlsx"
                )
                .contentLength(data.length)
                .body(resource);
    }

    @PutMapping("/structures/{structureId}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<?> updateStructure(
            @PathVariable Long structureId,
            @Valid @RequestBody FeeStructureRequestDto r) {

        return ResponseEntity.ok(
                service.updateStructure(structureId, r)
        );
    }

    @PutMapping("/structures/{structureId}/enable")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<?> enableStructure(
            @PathVariable Long structureId) {

        service.enableStructure(structureId);

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Fee structure enabled successfully"
                )
        );
    }
}
