package com.company.leavemanagement.policy;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.company.leavemanagement.dto.request.LeaveApplyRequest;
import com.company.leavemanagement.entity.Employee;
import com.company.leavemanagement.exception.BusinessRuleException;

@Component
public class MaternityLeavePolicy extends BaseLeavePolicy {

    private static final BigDecimal MAX_MATERNITY_DAYS = BigDecimal.valueOf(180);

    @Override
    protected void validateSpecificRules(
            Employee employee,
            LeaveApplyRequest request,
            BigDecimal requestedDays,
            MultipartFile file) {

        if ("MALE".equalsIgnoreCase(employee.getGender())) {
            throw new BusinessRuleException("Maternity leave is applicable only to female employees");
        }

        if (requestedDays.compareTo(MAX_MATERNITY_DAYS) > 0) {
            throw new BusinessRuleException("Maternity leave cannot exceed 180 days");
        }

        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("Supporting medical documentation is required for maternity leave");
        }
    }
}
