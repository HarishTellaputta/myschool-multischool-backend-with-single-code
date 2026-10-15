package com.harish.student_management.service;

import com.harish.student_management.dto.TeacherLeaveApprovalDto;
import com.harish.student_management.dto.TeacherLeaveRequestDto;
import com.harish.student_management.dto.TeacherLeaveResponseDto;
import com.harish.student_management.entity.Teacher;
import com.harish.student_management.entity.TeacherLeave;
import com.harish.student_management.exception.ResourceNotFoundException;
import com.harish.student_management.repository.TeacherLeaveRepository;
import com.harish.student_management.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TeacherLeaveServiceImpl implements TeacherLeaveService {

    private static final Logger logger = LoggerFactory.getLogger(TeacherLeaveServiceImpl.class);

    private final TeacherLeaveRepository leaveRepository;
    private final TeacherRepository teacherRepository;

    @Override
    public TeacherLeaveResponseDto applyForLeave(TeacherLeaveRequestDto request) {
        logger.info("Applying for leave for teacher: {}", request.getTeacherId());

        Teacher teacher = teacherRepository.findById(request.getTeacherId())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id: " + request.getTeacherId()));

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        if (leaveRepository.existsByTeacherIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                request.getTeacherId(), request.getEndDate(), request.getStartDate())) {
            throw new IllegalArgumentException("Teacher already has leave during this period");
        }

        TeacherLeave leave = TeacherLeave.builder()
                .teacher(teacher)
                .leaveType(request.getLeaveType())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .reason(request.getReason())
                .status("PENDING")
                .build();

        TeacherLeave savedLeave = leaveRepository.save(leave);
        logger.info("Leave application created successfully with ID: {}", savedLeave.getId());

        return mapToResponseDto(savedLeave);
    }

    @Override
    public TeacherLeaveResponseDto getLeaveById(Long id) {
        logger.info("Fetching leave application with ID: {}", id);
        TeacherLeave leave = leaveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave application not found with id: " + id));
        return mapToResponseDto(leave);
    }

    @Override
    public List<TeacherLeaveResponseDto> getLeavesByTeacherId(Long teacherId) {
        logger.info("Fetching leave applications for teacher ID: {}", teacherId);
        return leaveRepository.findByTeacherId(teacherId).stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    public List<TeacherLeaveResponseDto> getLeavesByTeacherIdAndStatus(Long teacherId, String status) {
        logger.info("Fetching leave applications for teacher ID: {} with status: {}", teacherId, status);
        return leaveRepository.findByTeacherIdAndStatus(teacherId, status).stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    public List<TeacherLeaveResponseDto> getLeavesByStatus(String status) {
        logger.info("Fetching leave applications with status: {}", status);
        return leaveRepository.findByStatus(status).stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    public TeacherLeaveResponseDto approveLeave(Long id, TeacherLeaveApprovalDto approval, String approvedBy) {
        logger.info("Approving/rejecting leave application with ID: {}", id);
        TeacherLeave leave = leaveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave application not found with id: " + id));

        if (!leave.getStatus().equals("PENDING")) {
            throw new IllegalArgumentException("Can only approve or reject pending leave applications");
        }

        if (!approval.getStatus().equals("APPROVED") && !approval.getStatus().equals("REJECTED")) {
            throw new IllegalArgumentException("Status must be either APPROVED or REJECTED");
        }

        leave.setStatus(approval.getStatus());
        leave.setApprovedAt(LocalDateTime.now());
        leave.setApprovedBy(approvedBy);

        if (approval.getStatus().equals("REJECTED")) {
            leave.setRejectionReason(approval.getRejectionReason());
        }

        TeacherLeave updatedLeave = leaveRepository.save(leave);
        logger.info("Leave application {} successfully with ID: {}", approval.getStatus(), updatedLeave.getId());

        return mapToResponseDto(updatedLeave);
    }

    @Override
    public TeacherLeaveResponseDto updateLeave(Long id, TeacherLeaveRequestDto request) {
        logger.info("Updating leave application with ID: {}", id);
        TeacherLeave leave = leaveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave application not found with id: " + id));

        if (!leave.getStatus().equals("PENDING")) {
            throw new IllegalArgumentException("Can only update pending leave applications");
        }

        Teacher teacher = teacherRepository.findById(request.getTeacherId())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id: " + request.getTeacherId()));

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        leave.setTeacher(teacher);
        leave.setLeaveType(request.getLeaveType());
        leave.setStartDate(request.getStartDate());
        leave.setEndDate(request.getEndDate());
        leave.setReason(request.getReason());

        TeacherLeave updatedLeave = leaveRepository.save(leave);
        logger.info("Leave application updated successfully with ID: {}", updatedLeave.getId());

        return mapToResponseDto(updatedLeave);
    }

    @Override
    public void deleteLeave(Long id) {
        logger.info("Deleting leave application with ID: {}", id);
        TeacherLeave leave = leaveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave application not found with id: " + id));

        if (!leave.getStatus().equals("PENDING")) {
            throw new IllegalArgumentException("Can only delete pending leave applications");
        }

        leaveRepository.delete(leave);
        logger.info("Leave application deleted successfully");
    }

    private TeacherLeaveResponseDto mapToResponseDto(TeacherLeave leave) {
        return TeacherLeaveResponseDto.builder()
                .id(leave.getId())
                .teacherId(leave.getTeacher() != null ? leave.getTeacher().getId() : null)
                .teacherName(leave.getTeacher() != null ? leave.getTeacher().getName() : null)
                .leaveType(leave.getLeaveType())
                .startDate(leave.getStartDate())
                .endDate(leave.getEndDate())
                .reason(leave.getReason())
                .status(leave.getStatus())
                .rejectionReason(leave.getRejectionReason())
                .appliedAt(leave.getAppliedAt())
                .approvedAt(leave.getApprovedAt())
                .approvedBy(leave.getApprovedBy())
                .createdAt(leave.getCreatedAt())
                .updatedAt(leave.getUpdatedAt())
                .build();
    }
}
