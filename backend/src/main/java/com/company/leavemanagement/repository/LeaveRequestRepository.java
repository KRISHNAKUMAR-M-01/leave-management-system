package com.company.leavemanagement.repository;

import com.company.leavemanagement.entity.Employee;
import com.company.leavemanagement.entity.LeaveRequest;
import com.company.leavemanagement.entity.LeaveType;
import com.company.leavemanagement.enums.LeaveStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.company.leavemanagement.dto.response.LabelAmount;
import com.company.leavemanagement.dto.response.StatusCount;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    //checks is an employee already has an active or pending leave request that overlaps the new
    @Query("""
            SELECT COUNT(r) > 0
            FROM LeaveRequest r
            WHERE r.employee = :employee
            AND r.leaveStatus IN :statuses
            AND r.startDate <= :endDate
            AND r.endDate >= :startDate
            """)
    boolean existsOverlappingLeave(@Param("employee") Employee employee, @Param("statuses") List<LeaveStatus> statuses, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);


    //checks if an employee has any leave requests in the system
    boolean existsByEmployee(Employee employee);

    //checks if a reviewer has any leave requests that they have reviewed
    boolean existsByReviewedBy(Employee reviewer);


    //finds a leave request by its ID and locks it for update to prevent concurrent modifications
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM LeaveRequest r where r.id = :id")
    Optional<LeaveRequest> findByIdForUpdate(@Param("id") Long id);

    //finds leave requests by their status and the manager of the employee who submitted the request, ordered by start date and ID
    @EntityGraph(attributePaths = {"employee", "leaveType"})
    @Query("""
            SELECT r FROM LeaveRequest r
            WHERE r.leaveStatus = :status
            AND r.employee.manager = :manager
            ORDER BY r.startDate ASC, r.id ASC
            """)
    List<LeaveRequest> findByStatusAndManager(@Param("status") LeaveStatus status, @Param("manager") Employee manager);

    //finds leave requests by their status and the role of the employee who submitted the request, ordered by start date and ID
    @EntityGraph(attributePaths = {"employee", "leaveType"})
    @Query("""
            SELECT r FROM LeaveRequest r
            WHERE r.leaveStatus = :status
            AND r.employee.role.roleName = :roleName
            ORDER BY r.startDate ASC, r.id ASC
            """)
    List<LeaveRequest> findByStatusAndApplicantRole(@Param("status") LeaveStatus status, @Param("roleName") String roleName);

    //searches for leave requests based on optional search criteria (employee ID, status, leave type ID, date range) and returns a paginated result
    @EntityGraph(attributePaths = {"employee", "leaveType", "reviewedBy"})
    @Query(
            value = """
                    SELECT r FROM LeaveRequest r
                    WHERE (:employeeId IS NULL OR r.employee.id = :employeeId)
                      AND (:status IS NULL OR r.leaveStatus = :status)
                      AND (:leaveTypeId IS NULL OR r.leaveType.id = :leaveTypeId)
                      AND (:fromDate IS NULL OR r.endDate >= :fromDate)
                      AND (:toDate IS NULL OR r.startDate <= :toDate)
                    """,
            countQuery = """
                    SELECT COUNT(r) FROM LeaveRequest r
                    WHERE (:employeeId IS NULL OR r.employee.id = :employeeId)
                      AND (:status IS NULL OR r.leaveStatus = :status)
                      AND (:leaveTypeId IS NULL OR r.leaveType.id = :leaveTypeId)
                      AND (:fromDate IS NULL OR r.endDate >= :fromDate)
                      AND (:toDate IS NULL OR r.startDate <= :toDate)
                    """
    )
    Page<LeaveRequest> search(@Param("employeeId") Long employeeId,
                              @Param("status") LeaveStatus status,
                              @Param("leaveTypeId") Long leaveTypeId,
                              @Param("fromDate") LocalDate fromDate,
                              @Param("toDate") LocalDate toDate,
                              Pageable pageable);

    //counts the number of leave requests for a specific status and manager                          
    @Query("""
            SELECT COUNT(r) FROM LeaveRequest r
            WHERE r.leaveStatus = :status AND r.employee.manager = :manager
            """)
    long countForManager(@Param("status") LeaveStatus status, @Param("manager") Employee manager);

    //counts the number of leave requests for a specific status and the role of the employee who submitted the request
    @Query("""
            SELECT COUNT(r) FROM LeaveRequest r
            WHERE r.leaveStatus = :status AND r.employee.role.roleName = :roleName
            """)
    long countByStatusAndApplicantRole(@Param("status") LeaveStatus status,
                                       @Param("roleName") String roleName);

    //finds leave requests for a specific manager, statuses, and date range, ordered by start date and ID                                   
    @EntityGraph(attributePaths = {"employee", "leaveType", "reviewedBy"})
    @Query("""
            SELECT r FROM LeaveRequest r
            WHERE r.employee.manager = :manager
              AND r.leaveStatus IN :statuses
              AND r.endDate >= :fromDate
              AND r.startDate <= :toDate
            ORDER BY r.startDate ASC, r.id ASC
            """)
    List<LeaveRequest> findTeamLeaves(@Param("manager") Employee manager,
                                      @Param("statuses") Collection<LeaveStatus> statuses,
                                      @Param("fromDate") LocalDate fromDate,
                                      @Param("toDate") LocalDate toDate);

    //finds leave requests for a specific applicant role, statuses, and date range, ordered by start date and ID
    @Query("""
            SELECT new com.company.leavemanagement.dto.response.StatusCount(r.leaveStatus, COUNT(r))
            FROM LeaveRequest r
            WHERE r.startDate BETWEEN :fromDate AND :toDate
            GROUP BY r.leaveStatus
            """)
    List<StatusCount> countGroupedByStatus(@Param("fromDate") LocalDate fromDate,
                                           @Param("toDate") LocalDate toDate);

    //finds the total number of leave days grouped by leave type for a specific status and date range                                       
    @Query("""
            SELECT new com.company.leavemanagement.dto.response.LabelAmount(lt.leaveTypeName, SUM(r.totalDays))
            FROM LeaveRequest r JOIN r.leaveType lt
            WHERE r.leaveStatus = :status AND r.startDate BETWEEN :fromDate AND :toDate
            GROUP BY lt.leaveTypeName
            ORDER BY lt.leaveTypeName
            """)
    List<LabelAmount> sumDaysGroupedByLeaveType(@Param("status") LeaveStatus status,
                                                @Param("fromDate") LocalDate fromDate,
                                                @Param("toDate") LocalDate toDate);

    @Query("""
            SELECT COALESCE(SUM(r.totalDays), 0)
            FROM LeaveRequest r
            WHERE r.employee = :employee
              AND r.leaveType = :leaveType
              AND r.leaveStatus = :status
              AND r.startDate BETWEEN :fromDate AND :toDate
            """)
    BigDecimal sumDaysByEmployeeAndLeaveTypeAndStatus(
            @Param("employee") Employee employee,
            @Param("leaveType") LeaveType leaveType,
            @Param("status") LeaveStatus status,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);
}
