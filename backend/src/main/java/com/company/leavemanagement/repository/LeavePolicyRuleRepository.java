package com.company.leavemanagement.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.company.leavemanagement.entity.LeavePolicyRule;
import com.company.leavemanagement.entity.LeaveType;
import com.company.leavemanagement.entity.Role;

@Repository
public interface LeavePolicyRuleRepository extends JpaRepository<LeavePolicyRule, Long> {

    Optional<LeavePolicyRule> findByRoleAndLeaveType(Role role, LeaveType leaveType);
}
