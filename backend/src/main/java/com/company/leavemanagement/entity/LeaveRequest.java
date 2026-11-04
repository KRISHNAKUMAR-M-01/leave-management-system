package com.company.leavemanagement.entity;

import com.company.leavemanagement.enums.HalfDaySession;
import com.company.leavemanagement.enums.LeaveStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "leave_requests")
public class LeaveRequest extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveType leaveType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "total_days", nullable = false, precision = 4, scale = 1)
    private BigDecimal totalDays;

    @Column(name = "is_half_day", nullable = false)
    private boolean halfDay;

    @Enumerated(EnumType.STRING)
    @Column(name = "half_day_session", length = 15)
    private HalfDaySession halfDaySession;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "leave_status", nullable = false, length = 20)
    private LeaveStatus leaveStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private Employee reviewedBy;

    @Column(name = "review_comment", length = 500)
    private String reviewComment;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    protected LeaveRequest() {}

    public LeaveRequest(Employee employee, LeaveType leaveType, LocalDate startDate, LocalDate endDate, BigDecimal totalDays, HalfDaySession halfDaySession, String reason) {
        this.employee = employee;
        this.leaveType = leaveType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.halfDay = halfDaySession != null;
        this.totalDays = totalDays;
        this.halfDaySession = halfDaySession;
        this.reason = reason;
        this.leaveStatus = LeaveStatus.PENDING;
    }

    public Employee getEmployee() {
        return employee;
    }

    public LeaveType getLeaveType() {
        return leaveType;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public boolean isHalfDay() {
        return halfDay;
    }

    public BigDecimal getTotalDays() {
        return totalDays;
    }

    public HalfDaySession getHalfDaySession() {
        return halfDaySession;
    }

    public String getReason() {
        return reason;
    }

    public LeaveStatus getLeaveStatus() {
        return leaveStatus;
    }

    public Employee getReviewedBy() {
        return reviewedBy;
    }

    public String getReviewComment() {
        return reviewComment;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }
}
