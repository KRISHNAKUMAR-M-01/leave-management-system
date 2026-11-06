package com.company.leavemanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PublicHolidayRequest(

        @NotNull(message = "Holiday date is required")
        LocalDate holidayDate,

        @NotBlank(message = "Holiday name is required")
        @Size(max = 100, message = "Holiday name must be at most 100 characters")
        String holidayName) {
}
