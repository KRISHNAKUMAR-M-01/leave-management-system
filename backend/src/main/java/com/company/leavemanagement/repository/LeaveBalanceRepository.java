package com.company.leavemanagement.repository;

import com.company.leavemanagement.entity.Employee;
import com.company.leavemanagement.entity.LeaveBalance;
import com.company.leavemanagement.entity.LeaveType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {

    Optional<LeaveBalance> findByEmployeeAndLeaveTypeAndCalendarYear(Employee employee, LeaveType leaveType, int calendarYear);

    void deleteByEmployee(Employee employee);

    @EntityGraph(attributePaths = "leaveType")
    List<LeaveBalance> findByEmployeeAndCalendarYear(Employee employee, int calendarYear);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT b FROM LeaveBalance b
            WHERE b.employee = :employee
            AND b.leaveType = :leaveType
            AND b.calendarYear = :year
            """)
    Optional<LeaveBalance> findForUpdate(@Param("employee") Employee employee, @Param("leaveType") LeaveType leaveType, @Param("year") int year);

    boolean existsByEmployeeAndLeaveTypeAndCalendarYear(Employee employee, LeaveType leaveType, int calendarYear);
}
