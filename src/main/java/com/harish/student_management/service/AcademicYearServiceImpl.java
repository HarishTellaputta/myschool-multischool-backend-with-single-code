package com.harish.student_management.service;

import com.harish.student_management.dto.AcademicYearRequestDto;
import com.harish.student_management.dto.AcademicYearResponseDto;
import com.harish.student_management.entity.AcademicYear;
import com.harish.student_management.exception.ResourceNotFoundException;
import com.harish.student_management.repository.AcademicYearRepository;
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

    @Override
    public AcademicYearResponseDto createAcademicYear(AcademicYearRequestDto request) {
        logger.info("Creating academic year with name: {}", request.getName());

        if (academicYearRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Academic year already exists with name: " + request.getName());
        }

        if (Boolean.TRUE.equals(request.getCurrent())) {
            academicYearRepository.findByCurrentTrue().ifPresent(currentYear -> {
                currentYear.setCurrent(false);
                academicYearRepository.save(currentYear);
            });
        }

        AcademicYear academicYear = AcademicYear.builder()
                .name(request.getName())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .current(request.getCurrent() != null ? request.getCurrent() : false)
                .description(request.getDescription())
                .build();

        AcademicYear savedAcademicYear = academicYearRepository.save(academicYear);
        logger.info("Academic year created successfully with ID: {}", savedAcademicYear.getId());

        return mapToResponseDto(savedAcademicYear);
    }

    @Override
    public AcademicYearResponseDto getAcademicYearById(Long id) {
        logger.info("Fetching academic year with ID: {}", id);
        AcademicYear academicYear = academicYearRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found with id: " + id));
        return mapToResponseDto(academicYear);
    }

    @Override
    public List<AcademicYearResponseDto> getAllAcademicYears() {
        logger.info("Fetching all academic years");
        return academicYearRepository.findAll().stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    public Optional<AcademicYearResponseDto> getCurrentAcademicYear() {
        logger.info("Fetching current academic year");
        return academicYearRepository.findByCurrentTrue()
                .map(this::mapToResponseDto);
    }

    @Override
    public AcademicYearResponseDto updateAcademicYear(Long id, AcademicYearRequestDto request) {
        logger.info("Updating academic year with ID: {}", id);
        AcademicYear academicYear = academicYearRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found with id: " + id));

        if (!academicYear.getName().equals(request.getName()) && academicYearRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Academic year already exists with name: " + request.getName());
        }

        if (Boolean.TRUE.equals(request.getCurrent())) {
            academicYearRepository.findByCurrentTrue().ifPresent(currentYear -> {
                if (!currentYear.getId().equals(id)) {
                    currentYear.setCurrent(false);
                    academicYearRepository.save(currentYear);
                }
            });
        }

        academicYear.setName(request.getName());
        academicYear.setStartDate(request.getStartDate());
        academicYear.setEndDate(request.getEndDate());
        academicYear.setCurrent(request.getCurrent() != null ? request.getCurrent() : false);
        academicYear.setDescription(request.getDescription());

        AcademicYear updatedAcademicYear = academicYearRepository.save(academicYear);
        logger.info("Academic year updated successfully with ID: {}", updatedAcademicYear.getId());

        return mapToResponseDto(updatedAcademicYear);
    }

    @Override
    public void deleteAcademicYear(Long id) {
        logger.info("Deleting academic year with ID: {}", id);
        AcademicYear academicYear = academicYearRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found with id: " + id));
        academicYearRepository.delete(academicYear);
        logger.info("Academic year deleted successfully with ID: {}", id);
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
}
