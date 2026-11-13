package com.company.leavemanagement.policy;

import org.springframework.stereotype.Component;

@Component
public class LeavePolicyFactory {

    private final SickLeavePolicy sickLeavePolicy;
    private final CasualLeavePolicy casualLeavePolicy;
    private final AnnualLeavePolicy annualLeavePolicy;
    private final MaternityLeavePolicy maternityLeavePolicy;
    private final PaternityLeavePolicy paternityLeavePolicy;

    public LeavePolicyFactory(
            SickLeavePolicy sickLeavePolicy,
            CasualLeavePolicy casualLeavePolicy,
            AnnualLeavePolicy annualLeavePolicy,
            MaternityLeavePolicy maternityLeavePolicy,
            PaternityLeavePolicy paternityLeavePolicy) {
        this.sickLeavePolicy = sickLeavePolicy;
        this.casualLeavePolicy = casualLeavePolicy;
        this.annualLeavePolicy = annualLeavePolicy;
        this.maternityLeavePolicy = maternityLeavePolicy;
        this.paternityLeavePolicy = paternityLeavePolicy;
    }

    public BaseLeavePolicy getPolicy(String leaveTypeName) {
        if (leaveTypeName == null) {
            return annualLeavePolicy;
        }

        String normalized = leaveTypeName.trim().toUpperCase();
        return switch (normalized) {
            case "SICK" -> sickLeavePolicy;
            case "CASUAL" -> casualLeavePolicy;
            case "ANNUAL" -> annualLeavePolicy;
            case "MATERNITY" -> maternityLeavePolicy;
            case "PATERNITY" -> paternityLeavePolicy;
            default -> annualLeavePolicy;
        };
    }
}