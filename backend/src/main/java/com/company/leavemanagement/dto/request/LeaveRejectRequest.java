package com.company.leavemanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LeaveRejectRequest(

        @NotBlank(message = "A comment is required when rejecting a leave request")
        @Size(max = 500, message = "Comment must be at most 500 character")
        String comment) {
}
