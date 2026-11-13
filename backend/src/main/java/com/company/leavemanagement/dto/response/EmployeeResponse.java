package com.company.leavemanagement.dto.response;

import com.company.leavemanagement.entity.Employee;

import java.time.LocalDate;

public record EmployeeResponse(
        Long id,
        String fullName,
        String email,
        String phone,
        Long departmentId,
        String departmentName,
        String designation,
        LocalDate joiningDate,
        String roleName,
        Long managerId,
        String managerName,
        boolean active,
        String gender) {

    public static EmployeeResponse from(Employee employee) {
        Employee manager = employee.getManager();
        return new EmployeeResponse(
                employee.getId(),
                employee.getFullName(),
                employee.getEmail(),
                employee.getPhone(),
                employee.getDepartment().getId(),
                employee.getDepartment().getDepartmentName(),
                employee.getDesignation(),
                employee.getJoiningDate(),
                employee.getRole().getRoleName(),
                manager != null ? manager.getId() : null,
                manager != null ? manager.getFullName() : null,
                employee.isActive(),
                employee.getGender());
    }
}
