package com.company.leavemanagement.dto.response;

import com.company.leavemanagement.entity.PublicHoliday;

import java.time.LocalDate;

public record PublicHolidayResponse(Long id, LocalDate holidayDate, String holidayName) {
    public static PublicHolidayResponse from(PublicHoliday holiday) {
        return new PublicHolidayResponse(
                holiday.getId(),
                holiday.getHolidayDate(),
                holiday.getHolidayName());
    }
}
