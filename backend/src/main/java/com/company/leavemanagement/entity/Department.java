package com.company.leavemanagement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "departments")
public class Department extends BaseEntity{

    @Column(name = "department_name", nullable = false, unique = true, length = 50)
    private String departmentName;

    @Column(name = "department_code", nullable = false, unique = true, length = 10)
    private String departmentCode;

    protected Department() {}

    public Department(String departmentName, String departmentCode) {
        this.departmentName = departmentName;
        this.departmentCode = departmentCode;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public String getDepartmentCode() {
        return departmentCode;
    }
}
