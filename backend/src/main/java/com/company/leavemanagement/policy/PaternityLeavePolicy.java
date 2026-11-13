package com.company.leavemanagement.policy;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.company.leavemanagement.dto.request.LeaveApplyRequest;
import com.company.leavemanagement.entity.Employee;
import com.company.leavemanagement.exception.BusinessRuleException;

@Component
public class PaternityLeavePolicy extends BaseLeavePolicy {

    private static final BigDecimal MAX_PATERNITY_DAYS = BigDecimal.valueOf(15);

    @Override
    protected void validateSpecificRules(
            Employee employee,
            LeaveApplyRequest request,
            BigDecimal requestedDays,
            MultipartFile file) {

        if ("FEMALE".equalsIgnoreCase(employee.getGender())) {
            throw new BusinessRuleException("Paternity leave is applicable only to male employees");
        }

        if (requestedDays.compareTo(MAX_PATERNITY_DAYS) > 0) {
            throw new BusinessRuleException("Paternity leave cannot exceed 15 days");
        }
    }
}
