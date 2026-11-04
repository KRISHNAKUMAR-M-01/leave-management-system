package com.company.leavemanagement.repository;

import com.company.leavemanagement.entity.Employee;
import com.company.leavemanagement.entity.LeaveBalance;
import com.company.leavemanagement.entity.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {
    Optional<LeaveBalance> findByEmployeeAndLeaveTypeAndCalendarYear(Employee employee, LeaveType leaveType, int calendarYear);

    void deleteByEmployee(Employee employee);
}
