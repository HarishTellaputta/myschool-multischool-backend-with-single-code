package com.harish.student_management.service;

import com.harish.student_management.dto.ClassSubjectResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/classes")
@RequiredArgsConstructor
public class ClassSubjectController {

    private final ClassSubjectService classSubjectService;

    @PostMapping("/{classId}/subjects/{subjectId}")
    public ResponseEntity<ClassSubjectResponse> assignSubject(
            @PathVariable Long classId,
            @PathVariable Long subjectId) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        classSubjectService.assignSubjectToClass(
                                classId,
                                subjectId
                        )
                );
    }

    @GetMapping("/{classId}/subjects")
    public ResponseEntity<List<ClassSubjectResponse>> getSubjects(
            @PathVariable Long classId) {

        return ResponseEntity.ok(
                classSubjectService.getSubjectsByClass(classId)
        );
    }

    @DeleteMapping("/{classId}/subjects/{subjectId}")
    public ResponseEntity<Void> removeSubject(
            @PathVariable Long classId,
            @PathVariable Long subjectId) {

        classSubjectService.removeSubjectFromClass(
                classId,
                subjectId
        );

        return ResponseEntity.noContent().build();
    }
}