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

    //finds a leave balance record for a specific employee, leave type, and calendar year
    Optional<LeaveBalance> findByEmployeeAndLeaveTypeAndCalendarYear(Employee employee, LeaveType leaveType, int calendarYear);

    //deletes all leave balance records associated with a specific employee
    void deleteByEmployee(Employee employee);

    //finds all leave balance records for a specific employee and calendar year
    @EntityGraph(attributePaths = "leaveType")
    List<LeaveBalance> findByEmployeeAndCalendarYear(Employee employee, int calendarYear);

    //finds all leave balance records for a specific leave type and calendar year
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT b FROM LeaveBalance b
            WHERE b.employee = :employee
            AND b.leaveType = :leaveType
            AND b.calendarYear = :year
            """)
    Optional<LeaveBalance> findForUpdate(@Param("employee") Employee employee, @Param("leaveType") LeaveType leaveType, @Param("year") int year);

    //checks if a leave balance record exists for a specific employee, leave type, and calendar year
    boolean existsByEmployeeAndLeaveTypeAndCalendarYear(Employee employee, LeaveType leaveType, int calendarYear);

    //finds all leave balance records for a specific calendar year
    @Query("SELECT b FROM LeaveBalance b WHERE b.calendarYear = :year")
    List<LeaveBalance> findAllByCalendarYear(@Param("year") int year);
}
