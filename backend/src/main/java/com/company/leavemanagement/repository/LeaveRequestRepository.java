package com.company.leavemanagement.repository;

import com.company.leavemanagement.entity.Employee;
import com.company.leavemanagement.entity.LeaveRequest;
import com.company.leavemanagement.enums.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    @Query("""
            SELECT COUNT(r) > 0
            FROM LeaveRequest r
            WHERE r.employee = :employee
            AND r.leaveStatus IN :statuses
            AND r.startDate <= :endDate
            AND r.endDate >= :startDate
            """)
    boolean existsOverlappingLeave(@Param("employee") Employee employee, @Param("statuses") Collection<LeaveStatus> statuses, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    boolean existsByEmployee(Employee employee);

    boolean existsByReviewedBy(Employee reviewer);
}
