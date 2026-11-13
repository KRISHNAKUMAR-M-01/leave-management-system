package com.company.leavemanagement.policy;

import java.math.BigDecimal;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.company.leavemanagement.dto.request.LeaveApplyRequest;
import com.company.leavemanagement.entity.Employee;
import com.company.leavemanagement.exception.BusinessRuleException;

@Component
public class SickLeavePolicy extends BaseLeavePolicy {

    private static final BigDecimal MEDICAL_DOC_THRESHOLD_DAYS = BigDecimal.valueOf(3);
    private static final long MAX_MEDICAL_DOC_BYTES = 5L * 1024 * 1024; // 5 MB
    private static final Set<String> ALLOWED_MEDICAL_DOC_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png"
    );

    @Override
    protected void validateSpecificRules(
            Employee employee,
            LeaveApplyRequest request,
            BigDecimal requestedDays,
            MultipartFile file) {

        // Medical document mandatory for 3+ days
        if (requestedDays.compareTo(MEDICAL_DOC_THRESHOLD_DAYS) >= 0) {
            if (file == null || file.isEmpty()) {
                throw new BusinessRuleException(
                        "A medical document is required for sick leave of 3 or more days");
            }
        }

        // Validate uploaded document format and size if present
        if (file != null && !file.isEmpty()) {
            if (file.getSize() > MAX_MEDICAL_DOC_BYTES) {
                throw new BusinessRuleException("Medical document must be 5 MB or smaller");
            }
            String contentType = file.getContentType();
            if (contentType == null || !ALLOWED_MEDICAL_DOC_TYPES.contains(contentType.toLowerCase())) {
                throw new BusinessRuleException("Only PDF, JPG, or PNG medical documents are allowed");
            }
        }
    }
}
