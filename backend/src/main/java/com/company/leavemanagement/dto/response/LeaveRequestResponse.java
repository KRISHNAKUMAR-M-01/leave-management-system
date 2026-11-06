package com.company.leavemanagement.dto.response;

import com.company.leavemanagement.entity.Employee;
import com.company.leavemanagement.entity.LeaveRequest;
import com.company.leavemanagement.enums.HalfDaySession;
import com.company.leavemanagement.enums.LeaveStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record LeaveRequestResponse(
        Long id,
        Long employeeId,
        String employeeName,
        Long leaveTypeId,
        String leaveTypeName,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal totalDays,
        boolean halfDay,
        HalfDaySession halfDaySession,
        String reason,
        LeaveStatus leaveStatus,
        String reviewedByname,
        String reviewComment,
        LocalDateTime reviewedAt,
        LocalDateTime createdAt) {

    public static LeaveRequestResponse from(LeaveRequest request) {
        Employee reviewer = request.getReviewedBy();

        return new LeaveRequestResponse(
                request.getId(),
                request.getEmployee().getId(),
                request.getEmployee().getFullName(),
                request.getLeaveType().getId(),
                request.getLeaveType().getLeaveTypeName(),
                request.getStartDate(),
                request.getEndDate(),
                request.getTotalDays(),
                request.isHalfDay(),
                request.getHalfDaySession(),
                request.getReason(),
                request.getLeaveStatus(),
                reviewer != null ? reviewer.getFullName() : null,
                request.getReviewComment(),
                request.getReviewedAt(),
                request.getCreatedAt());
    }
}
