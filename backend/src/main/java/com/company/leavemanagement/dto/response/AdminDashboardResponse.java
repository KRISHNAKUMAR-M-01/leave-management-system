package com.company.leavemanagement.dto.response;

import com.company.leavemanagement.enums.LeaveStatus;

import java.util.List;
import java.util.Map;

public record AdminDashboardResponse(
        long totalEmployees,
        long activeEmployees,
        long pendingManagerApprovals,
        int year,
        Map<LeaveStatus, Long> leavesByStatus,
        List<LabelAmount> approvedDaysByLeaveType,
        List<LabelCount> employeesByDepartment) {
}
