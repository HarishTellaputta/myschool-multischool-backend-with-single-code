package com.harish.student_management.service;

import com.harish.student_management.dto.AcademicYearRequestDto;
import com.harish.student_management.dto.AcademicYearResponseDto;
import com.harish.student_management.entity.AcademicYear;
import com.harish.student_management.exception.ResourceNotFoundException;
import com.harish.student_management.repository.AcademicYearRepository;
import com.harish.student_management.repository.SchoolRepository;

import com.harish.student_management.entity.School;
import com.harish.student_management.repository.SchoolRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AcademicYearServiceImpl implements AcademicYearService {

    private static final Logger logger = LoggerFactory.getLogger(AcademicYearServiceImpl.class);

    private final AcademicYearRepository academicYearRepository;
    private final CurrentSchoolService currentSchoolService;
    private final SchoolRepository schoolRepository;

    @Override
    public AcademicYearResponseDto getAcademicYearById(Long id) {
        Long schoolId = currentSchoolService.getCurrentSchoolId();

        AcademicYear year = academicYearRepository
                .findByIdAndSchool_Id(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Academic year not found"));

        return mapToResponseDto(year);
    }

    @Override
    public List<AcademicYearResponseDto> getAllAcademicYears() {
        Long schoolId = currentSchoolService.getCurrentSchoolId();

        return academicYearRepository.findAllBySchool_Id(schoolId)
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    public Optional<AcademicYearResponseDto> getCurrentAcademicYear() {
        Long schoolId = currentSchoolService.getCurrentSchoolId();

        return academicYearRepository
                .findBySchool_IdAndCurrentTrue(schoolId)
                .map(this::mapToResponseDto);
    }



    @Override
    @Transactional
    public AcademicYearResponseDto updateAcademicYear(
            Long id, AcademicYearRequestDto request) {

        Long schoolId = currentSchoolService.getCurrentSchoolId();

        AcademicYear academicYear = academicYearRepository
                .findByIdAndSchool_Id(id, schoolId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Academic year not found"));

        String name = request.getName().trim();

        if (!academicYear.getName().equalsIgnoreCase(name)
                && academicYearRepository
                .existsBySchool_IdAndNameIgnoreCase(
                        schoolId, name)) {
            throw new IllegalArgumentException(
                    "Academic year already exists in this school");
        }

        boolean makeCurrent =
                Boolean.TRUE.equals(request.getCurrent());

        if (makeCurrent) {
            academicYearRepository
                    .findBySchool_IdAndCurrentTrue(schoolId)
                    .filter(existing ->
                            !existing.getId().equals(id))
                    .ifPresent(existing -> {
                        existing.setCurrent(false);
                        academicYearRepository.save(existing);
                    });
        }

        academicYear.setName(name);
        academicYear.setStartDate(request.getStartDate());
        academicYear.setEndDate(request.getEndDate());
        academicYear.setCurrent(makeCurrent);
        academicYear.setDescription(request.getDescription());

        return mapToResponseDto(
                academicYearRepository.save(academicYear));
    }


    @Override
    @Transactional
    public void deleteAcademicYear(Long id) {

        Long schoolId = currentSchoolService.getCurrentSchoolId();

        AcademicYear academicYear = academicYearRepository
                .findByIdAndSchool_Id(id, schoolId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Academic year not found"));

        academicYearRepository.delete(academicYear);
    }

    private AcademicYearResponseDto mapToResponseDto(AcademicYear academicYear) {
        return AcademicYearResponseDto.builder()
                .id(academicYear.getId())
                .name(academicYear.getName())
                .startDate(academicYear.getStartDate())
                .endDate(academicYear.getEndDate())
                .current(academicYear.getCurrent())
                .description(academicYear.getDescription())
                .createdAt(academicYear.getCreatedAt())
                .updatedAt(academicYear.getUpdatedAt())
                .build();
    }


    @Override
    public AcademicYearResponseDto createAcademicYear(
            AcademicYearRequestDto request) {

        Long schoolId = currentSchoolService.getCurrentSchoolId();

        if (academicYearRepository
                .existsBySchool_IdAndNameIgnoreCase(
                        schoolId, request.getName().trim())) {
            throw new IllegalArgumentException(
                    "Academic year already exists in this school"
            );
        }

        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("School not found"));

        boolean makeCurrent = Boolean.TRUE.equals(request.getCurrent());

        if (makeCurrent) {
            academicYearRepository
                    .findBySchool_IdAndCurrentTrue(schoolId)
                    .ifPresent(existing -> {
                        existing.setCurrent(false);
                        academicYearRepository.save(existing);
                    });
        }

        AcademicYear academicYear = AcademicYear.builder()
                .school(school)
                .name(request.getName().trim())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .current(makeCurrent)
                .description(request.getDescription())
                .build();

        return mapToResponseDto(
                academicYearRepository.save(academicYear));
    }


    @Override
    @Transactional
    public AcademicYearResponseDto setAcademicYearCurrent(
            Long id, boolean current) {

        Long schoolId = currentSchoolService.getCurrentSchoolId();

        AcademicYear target = academicYearRepository
                .findByIdAndSchool_Id(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Academic year not found"));

        if (current) {
            academicYearRepository
                    .findBySchool_IdAndCurrentTrue(schoolId)
                    .filter(existing ->
                            !existing.getId().equals(target.getId()))
                    .ifPresent(existing -> {
                        existing.setCurrent(false);
                        academicYearRepository.save(existing);
                    });
        }

        target.setCurrent(current);

        return mapToResponseDto(academicYearRepository.save(target));
    }


}
