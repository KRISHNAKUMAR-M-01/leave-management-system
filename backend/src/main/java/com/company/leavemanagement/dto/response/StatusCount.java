package com.company.leavemanagement.dto.response;

import com.company.leavemanagement.enums.LeaveStatus;

public record StatusCount(LeaveStatus status, Long count) {
}
