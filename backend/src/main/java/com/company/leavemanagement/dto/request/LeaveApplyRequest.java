package com.company.leavemanagement.dto.request;

import com.company.leavemanagement.enums.HalfDaySession;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record LeaveApplyRequest(
        @NotNull(message = "Leave type is required")
        Long leaveTypeId,

        @NotNull(message = "Start date is required")
        LocalDate startDate,

        @NotNull(message = "End date is required")
        LocalDate endDate,

        @NotBlank(message = "Reason is required")
        @Size(max = 500, message = "Reason must be at most 500 character length")
        String reason ,

        HalfDaySession halfDaySession) {
}
