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
    private final CurrentSchoolService currentSchoolService;

    @Override
    public SchoolNoticeResponseDto create(SchoolNoticeRequestDto r) {
        SchoolNotice notice = new SchoolNotice();

        notice.setSchool(currentSchoolService.getCurrentSchool());
        notice.setStatus(
                r.getStatus() == null || r.getStatus().isBlank()
                        ? "DRAFT"
                        : r.getStatus().trim().toUpperCase()
        );

        return map(repo.save(apply(notice, r)));
    }

    @Override
    public SchoolNoticeResponseDto update(
            Long id,
            SchoolNoticeRequestDto r) {

        SchoolNotice n = find(id);

        boolean wasPublished =
                "PUBLISHED".equalsIgnoreCase(n.getStatus());

        LocalDateTime publishedAt = n.getPublishedAt();

        apply(n, r);

        // Keep an already-published notice published after editing.
        if (wasPublished) {
            n.setStatus("PUBLISHED");

            if (publishedAt == null) {
                n.setPublishedAt(LocalDateTime.now());
            } else {
                n.setPublishedAt(publishedAt);
            }
        }

        return map(repo.save(n));
    }

    @Override
    public void delete(Long id) {
        SchoolNotice notice = find(id);
        repo.delete(notice);
    }

    @Override
    public SchoolNoticeResponseDto publish(Long id, boolean publish) {
        SchoolNotice n = find(id);

        if (publish) {
            n.setStatus("PUBLISHED");

            if (n.getPublishedAt() == null) {
                n.setPublishedAt(LocalDateTime.now());
            }
        } else {
            n.setStatus("DRAFT");
            n.setPublishedAt(null);
        }

        return map(repo.save(n));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SchoolNoticeResponseDto> relevant(
            String role,
            Long classId,
            Long sectionId) {

        String userRole = role == null
                ? ""
                : role.trim().toUpperCase(Locale.ROOT);

        Set<String> allowedRoles = Set.of(
                "SCHOOL_ADMIN", "PARENT", "TEACHER", "STUDENT"
        );

        if (!allowedRoles.contains(userRole)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Invalid role for notice access"
            );
        }

        Long schoolId = currentSchoolService.getCurrentSchoolId();

        List<SchoolNotice> notices =
                repo.findBySchool_IdAndStatusOrderByPublishedAtDescIdDesc(
                        schoolId,
                        "PUBLISHED"
                );

        if ("SCHOOL_ADMIN".equals(userRole)) {
            return notices.stream()
                    .map(this::map)
                    .toList();
        }

        return notices.stream()
                .filter(n -> isAudienceMatch(n, userRole))
                .filter(n -> isClassMatch(n, classId))
                .filter(n -> isSectionMatch(n, sectionId))
                .map(this::map)
                .toList();
    }

    private SchoolNotice apply(
            SchoolNotice n,
            SchoolNoticeRequestDto r) {

        Long schoolId = currentSchoolService.getCurrentSchoolId();

        if (r.getAudience() == null || r.getAudience().isBlank()) {
            throw new IllegalArgumentException("Audience is required");
        }

        String audience = r.getAudience().trim().toUpperCase(Locale.ROOT);

        Set<String> allowedAudiences = Set.of(
                "ALL_PARENTS",
                "CLASS",
                "SECTION",
                "TEACHERS",
                "STUDENTS",
                "STUDENTS_AND_PARENTS",
                "ALL"
        );

        if (!allowedAudiences.contains(audience)) {
            throw new IllegalArgumentException("Unsupported notice audience");
        }

        if (("CLASS".equals(audience) || "SECTION".equals(audience))
                && r.getClassId() == null) {
            throw new IllegalArgumentException(
                    "Class is required for targeted notice"
            );
        }

        if ("SECTION".equals(audience) && r.getSectionId() == null) {
            throw new IllegalArgumentException(
                    "Section is required for section-targeted notice"
            );
        }

        if (r.getStatus() != null && !r.getStatus().isBlank()) {
            String status = r.getStatus().trim().toUpperCase(Locale.ROOT);

            if (!Set.of("DRAFT", "PUBLISHED").contains(status)) {
                throw new IllegalArgumentException("Invalid notice status");
            }

            n.setStatus(status);
        }

        ClassEntity classEntity = null;

        if (r.getClassId() != null) {
            classEntity = classes.findById(r.getClassId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException("Class not found")
                    );

            if (classEntity.getSchool() == null
                    || !schoolId.equals(classEntity.getSchool().getId())) {
                throw new ResourceNotFoundException("Class not found");
            }
        }

        Section section = null;

        if (r.getSectionId() != null) {
            section = sections.findById(r.getSectionId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException("Section not found")
                    );

            if (section.getClassEntity() == null
                    || section.getClassEntity().getSchool() == null
                    || !schoolId.equals(
                    section.getClassEntity().getSchool().getId())) {
                throw new ResourceNotFoundException("Section not found");
            }

            if (classEntity == null
                    || !section.getClassEntity().getId()
                    .equals(classEntity.getId())) {
                throw new IllegalArgumentException(
                        "Section does not belong to class"
                );
            }
        }

        n.setTitle(r.getTitle());
        n.setContent(r.getContent());
        n.setCategory(
                r.getCategory() == null || r.getCategory().isBlank()
                        ? "General"
                        : r.getCategory().trim()
        );
        n.setAudience(audience);
        n.setClassEntity(classEntity);
        n.setSection(section);

        if (n.getStatus() == null || n.getStatus().isBlank()) {
            n.setStatus("DRAFT");
        }

        if ("PUBLISHED".equals(n.getStatus())) {
            if (n.getPublishedAt() == null) {
                n.setPublishedAt(LocalDateTime.now());
            }
        } else {
            n.setPublishedAt(null);
        }

        return n;
    }

    private SchoolNotice find(Long id) {
        Long schoolId = currentSchoolService.getCurrentSchoolId();

        return repo.findByIdAndSchool_Id(id, schoolId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Notice not found")
                );
    }

    private SchoolNoticeResponseDto map(SchoolNotice n) {
        return SchoolNoticeResponseDto.builder()
                .id(n.getId())
                .title(n.getTitle())
                .content(n.getContent())
                .audience(n.getAudience())
                .category(n.getCategory())
                .status(n.getStatus())
                .classId(
                        n.getClassEntity() == null
                                ? null
                                : n.getClassEntity().getId()
                )
                .className(
                        n.getClassEntity() == null
                                ? null
                                : n.getClassEntity().getName()
                )
                .sectionId(
                        n.getSection() == null
                                ? null
                                : n.getSection().getId()
                )
                .sectionName(
                        n.getSection() == null
                                ? null
                                : n.getSection().getName()
                )
                .publishedAt(n.getPublishedAt())
                .createdAt(n.getCreatedAt())
                .build();
    }

    private boolean isAudienceMatch(
            SchoolNotice notice,
            String role) {

        if (notice.getAudience() == null || role == null) {
            return false;
        }

        String audience = notice.getAudience()
                .trim()
                .toUpperCase(Locale.ROOT);

        String userRole = role.trim()
                .toUpperCase(Locale.ROOT);

        return switch (audience) {
            case "ALL_PARENTS" -> "PARENT".equals(userRole);

            case "STUDENTS" -> "STUDENT".equals(userRole);

            case "STUDENTS_AND_PARENTS" -> "STUDENT".equals(userRole)
                    || "PARENT".equals(userRole);

            case "TEACHERS" -> "TEACHER".equals(userRole);

            case "ALL" -> "STUDENT".equals(userRole)
                    || "PARENT".equals(userRole)
                    || "TEACHER".equals(userRole);

            case "CLASS", "SECTION" -> "STUDENT".equals(userRole)
                    || "PARENT".equals(userRole);

            default -> false;
        };
    }

    private boolean isClassMatch(
            SchoolNotice notice,
            Long classId) {

        String audience = notice.getAudience();

        if (!"CLASS".equalsIgnoreCase(audience)
                && !"SECTION".equalsIgnoreCase(audience)) {
            return true;
        }

        return classId != null
                && notice.getClassEntity() != null
                && classId.equals(notice.getClassEntity().getId());
    }

    private boolean isSectionMatch(
            SchoolNotice notice,
            Long sectionId) {

        if (!"SECTION".equalsIgnoreCase(notice.getAudience())) {
            return true;
        }

        return sectionId != null
                && notice.getSection() != null
                && sectionId.equals(notice.getSection().getId());
    }
}
