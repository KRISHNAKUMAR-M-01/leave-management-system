package com.company.leavemanagement.policy;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.company.leavemanagement.dto.request.LeaveApplyRequest;
import com.company.leavemanagement.entity.Employee;

@Component
public class AnnualLeavePolicy extends BaseLeavePolicy {

    @Override
    protected void validateSpecificRules(
            Employee employee,
            LeaveApplyRequest request,
            BigDecimal requestedDays,
            MultipartFile file) {
        
    }
}
