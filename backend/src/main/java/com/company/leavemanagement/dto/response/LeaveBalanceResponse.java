package com.company.leavemanagement.dto.response;

import com.company.leavemanagement.entity.LeaveBalance;

import java.math.BigDecimal;

public record LeaveBalanceResponse(
        Long leaveTypeId,
        String leaveTypeName,
        int calendarYear,
        BigDecimal totalDays,
        BigDecimal usedDays,
        BigDecimal remainingDays) {

    public static LeaveBalanceResponse from(LeaveBalance balance) {
        return new LeaveBalanceResponse(
                balance.getLeaveType().getId(),
                balance.getLeaveType().getLeaveTypeName(),
                balance.getCalendarYear(),
                balance.getTotalDays(),
                balance.getUsedDays(),
                balance.getRemainingDays());
    }
}
