package com.harish.student_management.controller; import com.harish.student_management.dto.AchievementDtos.*; import com.harish.student_management.service.AchievementService; import jakarta.validation.Valid; import lombok.RequiredArgsConstructor; import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.web.bind.annotation.*; import java.util.*;

@RestController
@RequestMapping("/api/v1/achievements")
@RequiredArgsConstructor
public class AchievementController {
    private final AchievementService service;

    @PostMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','TEACHER')")
    public Response create(@Valid @RequestBody Request r, @RequestParam(required = false) Long teacherId) {
        return service.create(r, teacherId);
    }

    @GetMapping("/student/{id}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','TEACHER','PARENT','STUDENT')")
    public List<Response> student(@PathVariable Long id) {
        return service.student(id);
    }
}
