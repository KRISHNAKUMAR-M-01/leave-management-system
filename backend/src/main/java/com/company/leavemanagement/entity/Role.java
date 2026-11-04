package com.company.leavemanagement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "roles")
public class Role extends BaseEntity{

    @Column(name ="role_name", nullable = false, unique = true, length = 30)
    private String roleName;

    protected Role() {}

    public Role(String roleName) {
        this.roleName =roleName;
    }

    public String getRoleName() {
        return roleName;
    }
}
