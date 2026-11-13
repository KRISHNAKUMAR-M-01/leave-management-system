package com.company.leavemanagement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "leave_policy_rules",
        uniqueConstraints = @UniqueConstraint(name = "uq_role_leave_type", columnNames = {"role_id", "leave_type_id"})
)
public class LeavePolicyRule extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false, foreignKey = @ForeignKey(name = "fk_policy_role"))
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leave_type_id", nullable = false, foreignKey = @ForeignKey(name = "fk_policy_leave_type"))
    private LeaveType leaveType;

    @Column(name = "max_annual_quota", nullable = false)
    private Integer maxAnnualQuota = 12;

    @Column(name = "max_carry_forward", nullable = false)
    private Integer maxCarryForward = 0;

    @Column(name = "min_notice_days", nullable = false)
    private Integer minNoticeDays = 0;

    public LeavePolicyRule() {
    }

    public LeavePolicyRule(Role role, LeaveType leaveType, Integer maxAnnualQuota, Integer maxCarryForward, Integer minNoticeDays) {
        this.role = role;
        this.leaveType = leaveType;
        this.maxAnnualQuota = maxAnnualQuota;
        this.maxCarryForward = maxCarryForward;
        this.minNoticeDays = minNoticeDays;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public LeaveType getLeaveType() {
        return leaveType;
    }

    public void setLeaveType(LeaveType leaveType) {
        this.leaveType = leaveType;
    }

    public Integer getMaxAnnualQuota() {
        return maxAnnualQuota;
    }

    public void setMaxAnnualQuota(Integer maxAnnualQuota) {
        this.maxAnnualQuota = maxAnnualQuota;
    }

    public Integer getMaxCarryForward() {
        return maxCarryForward;
    }

    public void setMaxCarryForward(Integer maxCarryForward) {
        this.maxCarryForward = maxCarryForward;
    }

    public Integer getMinNoticeDays() {
        return minNoticeDays != null ? minNoticeDays : 0;
    }

    public void setMinNoticeDays(Integer minNoticeDays) {
        this.minNoticeDays = minNoticeDays;
    }
}
