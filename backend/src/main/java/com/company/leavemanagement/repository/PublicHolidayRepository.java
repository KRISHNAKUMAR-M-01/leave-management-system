package com.company.leavemanagement.repository;

import com.company.leavemanagement.entity.PublicHoliday;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface PublicHolidayRepository extends JpaRepository<PublicHoliday, Long> {
    List<PublicHoliday> findByHolidayDateBetween(LocalDate startDate, LocalDate endDate);

    boolean existsByHolidayDate( LocalDate holidayDate);
}
