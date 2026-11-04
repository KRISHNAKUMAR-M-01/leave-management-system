package com.company.leavemanagement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "leave_types")
public class LeaveType extends BaseEntity{

    @Column(name = "leave_type_name", nullable = false, unique = true, length = 30)
    private String leaveTypeName;

    @Column(name = "days_per_year", nullable = false, precision = 4, scale = 1)
    private BigDecimal daysPerYear;

    protected LeaveType() {}

    public LeaveType(String leaveTypeName, BigDecimal daysPerYear) {
        this.leaveTypeName = leaveTypeName;
        this.daysPerYear = daysPerYear;
    }

    public String getLeaveTypeName() {
        return leaveTypeName;
    }

    public BigDecimal getDaysPerYear() {
        return daysPerYear;
    }
}
