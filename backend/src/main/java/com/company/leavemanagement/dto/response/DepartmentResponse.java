package com.company.leavemanagement.dto.response;

import com.company.leavemanagement.entity.Department;

public record DepartmentResponse(Long id, String departmentName, String departmentCode) {

    public static DepartmentResponse from(Department department) {
        return new DepartmentResponse(
                department.getId(),
                department.getDepartmentName(),
                department.getDepartmentCode());
    }
}
