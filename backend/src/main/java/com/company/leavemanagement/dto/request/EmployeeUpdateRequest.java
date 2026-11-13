package com.company.leavemanagement.dto.request;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record EmployeeUpdateRequest(
        @NotBlank(message = "Full name is required")
        @Size(min = 3, max = 100, message = "Full name must be 3 to 100 characters")
        String fullName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email format is invalid")
        @Pattern(regexp = "^(?i).*@codeboard\\.com$", message = "Email must belong to @codeboard.com domain")
        @Size(max = 150, message = "Email must be at most 150 characters")
        String email,

        @NotBlank(message = "Phone is required")
        @Pattern(regexp = "^[6-9][0-9]{9}$", message = "Phone must be a valid 10-digit mobile number")
        String phone,

        @NotNull(message = "Department is required")
        Long departmentId,

        @NotBlank(message = "Designation is required")
        @Size(max = 50, message = "Designation must be at most 50 characters")
        String designation,

        @NotNull(message = "Joining date is required")
        LocalDate joiningDate,

        @NotBlank(message = "Role is required")
        @Pattern(regexp = "ADMIN|MANAGER|EMPLOYEE", message = "Role must be ADMIN, MANAGER or EMPLOYEE")
        String roleName,

        Long managerId,

        String gender) {
}
