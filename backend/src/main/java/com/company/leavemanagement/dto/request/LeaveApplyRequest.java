package com.company.leavemanagement.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record LeaveApplyRequest(
        @NotNull(message = "Leave type ID is required")
        Long leaveTypeId,

        @NotNull(message = "Start date is required")
        @FutureOrPresent(message = "Start date must be today or in the future")
        LocalDate startDate,

        @NotNull(message = "End date is required")
        @FutureOrPresent(message = "End date must be today or in the future")
        LocalDate endDate,

        @NotBlank(message = "Reason is required")
        @Size(min = 4, max = 450, message = "Reason must be 5 to 250 characters")
        String reason ) {
}
