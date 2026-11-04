package com.company.leavemanagement.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Year;

@Entity
@Table(name = "leave_balances")
public class LeaveBalance extends BaseEntity {

    @Column(name = "calendar_year", nullable = false)
    private int calendarYear;

    @Column(name = "total_days", nullable = false, precision = 4, scale = 1)
    private BigDecimal totalDays;

    @Column(name = "used_days", nullable = false, precision = 4, scale = 1)
    private BigDecimal usedDays = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveType leaveType;

    protected LeaveBalance() {}

    public LeaveBalance(int calendarYear, BigDecimal totalDays, Employee employee, LeaveType leaveType) {
        this.calendarYear = calendarYear;
        this.totalDays = totalDays;
        this.usedDays = BigDecimal.ZERO;
        this.employee = employee;
        this.leaveType = leaveType;
    }

    public int getCalendarYear() {
        return calendarYear;
    }

    public BigDecimal getTotalDays() {
        return totalDays;
    }

    public BigDecimal getUsedDays() {
        return usedDays;
    }

    public Employee getEmployee() {
        return employee;
    }

    public LeaveType getLeaveType() {
        return leaveType;
    }

    public BigDecimal getRemainingDays() {
        return totalDays.subtract(usedDays);
    }
}
