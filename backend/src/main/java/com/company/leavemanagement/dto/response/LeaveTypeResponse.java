package com.company.leavemanagement.dto.response;

import com.company.leavemanagement.entity.LeaveType;

import java.math.BigDecimal;

public record LeaveTypeResponse(Long id, String leaveTypeName, BigDecimal daysPerYear) {
    public static LeaveTypeResponse from(LeaveType leaveType) {
        return new LeaveTypeResponse(
                leaveType.getId(),
                leaveType.getLeaveTypeName(),
                leaveType.getDaysPerYear());
    }
}
