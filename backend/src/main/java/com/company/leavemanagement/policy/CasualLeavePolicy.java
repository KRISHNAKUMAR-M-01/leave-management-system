package com.company.leavemanagement.policy;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.company.leavemanagement.dto.request.LeaveApplyRequest;
import com.company.leavemanagement.entity.Employee;
import com.company.leavemanagement.exception.BusinessRuleException;

@Component
public class CasualLeavePolicy extends BaseLeavePolicy {

    private static final BigDecimal MAX_CASUAL_DAYS = BigDecimal.valueOf(3);

    @Override
    protected void validateSpecificRules(
            Employee employee,
            LeaveApplyRequest request,
            BigDecimal requestedDays,
            MultipartFile file) {

        if (requestedDays.compareTo(MAX_CASUAL_DAYS) > 0) {
            throw new BusinessRuleException(
                    "Casual leave cannot be applied for more than 3 consecutive days at a time");
        }
    }
}