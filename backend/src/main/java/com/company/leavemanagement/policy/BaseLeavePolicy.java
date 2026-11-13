package com.company.leavemanagement.policy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import org.springframework.web.multipart.MultipartFile;

import com.company.leavemanagement.dto.request.LeaveApplyRequest;
import com.company.leavemanagement.entity.Employee;
import com.company.leavemanagement.entity.LeavePolicyRule;
import com.company.leavemanagement.exception.BusinessRuleException;

public abstract class BaseLeavePolicy {

    public final void validate(
            Employee employee,
            LeaveApplyRequest request,
            BigDecimal requestedDays,
            BigDecimal effectiveAvailableBalance,
            LeavePolicyRule rule,
            MultipartFile file) {

        validateCommonRules(employee, request, requestedDays, effectiveAvailableBalance, rule);
        validateSpecificRules(employee, request, requestedDays, file);
    }

    protected void validateCommonRules(
            Employee employee,
            LeaveApplyRequest request,
            BigDecimal requestedDays,
            BigDecimal effectiveAvailableBalance,
            LeavePolicyRule rule) {

        LocalDate startDate = request.startDate();
        LocalDate endDate = request.endDate();

        if (endDate.isBefore(startDate)) {
            throw new BusinessRuleException("End date cannot be before start date");
        }
        if (startDate.isBefore(LocalDate.now())) {
            throw new BusinessRuleException("Start date cannot be in the past");
        }
        if (startDate.getYear() != endDate.getYear()) {
            throw new BusinessRuleException(
                    "Leave must start and end in the same calendar year. Apply separately for each year");
        }
        if (request.halfDaySession() != null && !startDate.equals(endDate)) {
            throw new BusinessRuleException("A half-day leave must be for a single date");
        }

        if (rule != null && rule.getMinNoticeDays() > 0) {
            long noticeGivenDays = ChronoUnit.DAYS.between(LocalDate.now(), startDate);
            if (noticeGivenDays < rule.getMinNoticeDays()) {
                throw new BusinessRuleException(String.format(
                        "Your role (%s) requires at least %d day(s) advance notice for this leave type. Notice given: %d day(s).",
                        employee.getRole().getRoleName(),
                        rule.getMinNoticeDays(),
                        noticeGivenDays
                ));
            }
        }
        if (effectiveAvailableBalance != null && requestedDays.compareTo(effectiveAvailableBalance) > 0) {
            throw new BusinessRuleException(String.format(
                    "Requested %s day(s) exceed your available balance of %s day(s)",
                    requestedDays,
                    effectiveAvailableBalance
            ));
        }
    }

    protected abstract void validateSpecificRules(
            Employee employee,
            LeaveApplyRequest request,
            BigDecimal requestedDays,
            MultipartFile file);
}
