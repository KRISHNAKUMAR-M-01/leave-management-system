package com.company.leavemanagement.repository;

import com.company.leavemanagement.entity.PublicHoliday;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface PublicHolidayRepository extends JpaRepository<PublicHoliday, Long> {
    
    //finds all public holidays that fall within a specified date range
    List<PublicHoliday> findByHolidayDateBetween(LocalDate startDate, LocalDate endDate);

    //checks if a public holiday exists on a specific date
    boolean existsByHolidayDate( LocalDate holidayDate);
}
