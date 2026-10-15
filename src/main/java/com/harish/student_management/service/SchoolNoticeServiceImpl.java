package com.harish.student_management.service;

import com.harish.student_management.dto.*;
import com.harish.student_management.entity.*;
import com.harish.student_management.exception.ResourceNotFoundException;
import com.harish.student_management.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class SchoolNoticeServiceImpl implements SchoolNoticeService {
    private final SchoolNoticeRepository repo;
    private final ClassRepository classes;
    private final SectionRepository sections;

    public SchoolNoticeResponseDto create(SchoolNoticeRequestDto r) {
        SchoolNotice notice = new SchoolNotice();

        if (r.getStatus() == null || r.getStatus().isBlank()) {
            notice.setStatus("DRAFT");
        }

        return map(repo.save(apply(notice, r)));
    }

    public SchoolNoticeResponseDto update(Long id, SchoolNoticeRequestDto r) {
        SchoolNotice n = find(id);

        boolean wasPublished = "PUBLISHED".equalsIgnoreCase(n.getStatus());
        LocalDateTime publishedAt = n.getPublishedAt();

        apply(n, r);

        // Published notice can also be edited.
        // Keep it published after editing.
        if (wasPublished) {
            n.setStatus("PUBLISHED");

            if (publishedAt != null) {
                n.setPublishedAt(publishedAt);
            } else {
                n.setPublishedAt(LocalDateTime.now());
            }
        }

        return map(n);
    }

    public void delete(Long id) {
        repo.delete(find(id));
    }

    public SchoolNoticeResponseDto publish(Long id, boolean publish) {
        SchoolNotice n = find(id);
        n.setStatus(publish ? "PUBLISHED" : "DRAFT");
        n.setPublishedAt(publish ? LocalDateTime.now() : null);
        return map(n);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SchoolNoticeResponseDto> relevant(
            String role,
            Long classId,
            Long sectionId
    ) {
        String userRole = role == null
                ? ""
                : role.trim().toUpperCase();

        List<SchoolNotice> notices =
                repo.findByStatusOrderByPublishedAtDescIdDesc("PUBLISHED");

        // School Admin → all published notices
        if ("SCHOOL_ADMIN".equals(userRole)) {
            return notices.stream()
                    .map(this::map)
                    .toList();
        }

        // Parent / Teacher / Student → school-wide published notices
        return notices.stream()
                .filter(n -> isAudienceMatch(n, userRole))
                .filter(n -> isClassMatch(n, classId))
                .filter(n -> isSectionMatch(n, sectionId))
                .map(this::map)
                .toList();
    }

    private SchoolNotice apply(SchoolNotice n, SchoolNoticeRequestDto r) {
        Set<String> a = Set.of(
                "ALL_PARENTS",
                "CLASS",
                "SECTION",
                "TEACHERS",
                "STUDENTS",
                "STUDENTS_AND_PARENTS",
                "ALL"
        );
        String audience = r.getAudience().toUpperCase();
        if (!a.contains(audience)) throw new IllegalArgumentException("Unsupported notice audience");
        if (("CLASS".equals(audience) || "SECTION".equals(audience)) && r.getClassId() == null)
            throw new IllegalArgumentException("Class is required for targeted notice");
        n.setTitle(r.getTitle());
        n.setContent(r.getContent());
        n.setCategory(
                r.getCategory() == null || r.getCategory().isBlank()
                        ? "General"
                        : r.getCategory().trim()
        );
        n.setAudience(audience);
        if (r.getStatus() != null && !r.getStatus().isBlank()) {
            n.setStatus(r.getStatus().toUpperCase());
        }
        n.setClassEntity(r.getClassId() == null ? null : classes.findById(r.getClassId()).orElseThrow(() -> new ResourceNotFoundException("Class not found")));
        n.setSection(r.getSectionId() == null ? null : sections.findById(r.getSectionId()).orElseThrow(() -> new ResourceNotFoundException("Section not found")));
        if (n.getSection() != null && (n.getClassEntity() == null || !n.getSection().getClassEntity().getId().equals(n.getClassEntity().getId())))
            throw new IllegalArgumentException("Section does not belong to class");
        if ("PUBLISHED".equals(n.getStatus()) && n.getPublishedAt() == null) n.setPublishedAt(LocalDateTime.now());
        return n;
    }

    private SchoolNotice find(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Notice not found: " + id));
    }

    private SchoolNoticeResponseDto map(SchoolNotice n) {
        return SchoolNoticeResponseDto.builder()
                .id(n.getId())
                .title(n.getTitle())
                .content(n.getContent())
                .audience(n.getAudience())
                .category(n.getCategory())
                .status(n.getStatus()).classId(n.getClassEntity() == null ? null : n.getClassEntity().getId()).className(n.getClassEntity() == null ? null : n.getClassEntity().getName()).sectionId(n.getSection() == null ? null : n.getSection().getId()).sectionName(n.getSection() == null ? null : n.getSection().getName()).publishedAt(n.getPublishedAt()).createdAt(n.getCreatedAt()).build();

    }

    private boolean isAudienceMatch(SchoolNotice notice, String role) {
        String audience = notice.getAudience();

        if (audience == null) {
            return false;
        }

        return switch (audience.toUpperCase()) {

            case "ALL_PARENTS" ->
                    "PARENT".equals(role);

            case "STUDENTS" ->
                    "STUDENT".equals(role);

            case "STUDENTS_AND_PARENTS" ->
                    "STUDENT".equals(role)
                            || "PARENT".equals(role);

            case "TEACHERS" ->
                    "TEACHER".equals(role);

            case "ALL" ->
                    "STUDENT".equals(role)
                            || "PARENT".equals(role)
                            || "TEACHER".equals(role);

            case "CLASS", "SECTION" ->
                    "STUDENT".equals(role)
                            || "PARENT".equals(role);

            default ->
                    false;
        };
    }
 private boolean isClassMatch(
         SchoolNotice notice,
         Long classId
 ) {
  if (!"CLASS".equalsIgnoreCase(notice.getAudience())
          && !"SECTION".equalsIgnoreCase(notice.getAudience())) {
   return true;
  }

  return classId != null
          && notice.getClassEntity() != null
          && notice.getClassEntity().getId().equals(classId);
 }
 private boolean isSectionMatch(
         SchoolNotice notice,
         Long sectionId
 ) {
  if (!"SECTION".equalsIgnoreCase(notice.getAudience())) {
   return true;
  }

  return sectionId != null
          && notice.getSection() != null
          && notice.getSection().getId().equals(sectionId);
 }
}
