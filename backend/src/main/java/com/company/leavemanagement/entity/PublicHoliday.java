package com.company.leavemanagement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "public_holidays")
public class PublicHoliday extends BaseEntity {

    @Column(name = "holiday_date", nullable = false, unique = true)
    private LocalDate holidayDate;

    @Column(name = "holiday_name", nullable = false, length = 100)
    private String holidayName;

    protected PublicHoliday() {}

    public PublicHoliday(LocalDate holidayDate, String holidayName) {
        this.holidayDate = holidayDate;
        this.holidayName = holidayName;
    }

    public LocalDate getHolidayDate() {
        return holidayDate;
    }

    public String getHolidayName() {
        return holidayName;
    }
}
