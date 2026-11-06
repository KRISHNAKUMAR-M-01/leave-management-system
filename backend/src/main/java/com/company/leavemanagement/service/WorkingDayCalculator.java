package com.company.leavemanagement.service;

import com.company.leavemanagement.entity.PublicHoliday;
import com.company.leavemanagement.repository.PublicHolidayRepository;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class WorkingDayCalculator {

    private final PublicHolidayRepository publicHolidayRepository;

    public WorkingDayCalculator(PublicHolidayRepository publicHolidayRepository) {
        this.publicHolidayRepository = publicHolidayRepository;
    }

    public int countWorkingDays(LocalDate startDate, LocalDate endDate) {
        Set<LocalDate> holidays = publicHolidayRepository.findByHolidayDateBetween(startDate, endDate)
                .stream()
                .map(PublicHoliday::getHolidayDate)
                .collect(Collectors.toSet());

        return (int) startDate.datesUntil(endDate.plusDays(1))
                .filter(date -> isWorkingDay(date, holidays))
                .count();
    }

    private boolean isWorkingDay(LocalDate date, Set<LocalDate> holidays) {
        DayOfWeek day = date.getDayOfWeek();
        boolean isWeekend = day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
        return !isWeekend && !holidays.contains(date);
    }


}
