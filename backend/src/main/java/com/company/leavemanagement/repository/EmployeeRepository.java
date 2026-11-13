package com.company.leavemanagement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.company.leavemanagement.dto.response.LabelCount;
import com.company.leavemanagement.entity.Employee;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    
    //get employee by email
    Optional<Employee> findByEmail(String email);

    //checks if an employee record already exists by email
    boolean existsByEmail(String email);

    //checks if an email exists for any other employee ID (used during updates to prevent duplicate emails)
    boolean existsByEmailAndIdNot(String email, Long id);

    //checks if an employee is currently assigned as an active manager to any subordinates
    boolean existsByManagerAndActiveTrue(Employee manager);

    //checks if an employee has any assigned subordinates regardless of active status
    boolean existsByManager(Employee manager);

    //Uses @EntityGraph to prevent the N+1 select problem by fetching department, role, and manager in a single join
    @EntityGraph(attributePaths = {"department", "role", "manager"})
    //searches for employees based on optional search criteria (name, email, department, role, active status) and returns a paginated result
    @Query(
            value = """
                    SELECT e FROM Employee e
                    WHERE (:search IS NULL OR LOWER(e.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(e.email) LIKE LOWER(CONCAT('%', :search, '%')))
                    AND (:departmentId IS NULL OR e.department.id = :departmentId)
                    AND (:roleName IS NULL OR e.role.roleName = :roleName)
                    AND (:active IS NULL OR e.active = :active)
                    """,
            countQuery = """
                    SELECT COUNT(e) FROM Employee e
                    WHERE (:search IS NULL OR LOWER(e.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(e.email) LIKE LOWER(CONCAT('%', :search, '%')))
                    AND (:departmentId IS NULL OR e.department.id = :departmentId)
                    AND (:roleName IS NULL OR e.role.roleName = :roleName)
                    AND (:active IS NULL OR e.active = :active)
                    """
    )
    Page<Employee> search(@Param("search") String search,
                          @Param("departmentId") Long departmentId,
                          @Param("roleName") String roleName,
                          @Param("active") Boolean active,
                          Pageable pageable);

    //returns the count of all active employees in the system
    long countByActiveTrue();

    //returns the count of all active employees who report to a specific manager
    long countByManagerAndActiveTrue(Employee manager);

    @EntityGraph(attributePaths = {"department", "role", "manager"})
    List<Employee> findByManagerAndActiveTrueOrderByFullNameAsc(Employee manager);

    //returns a list of LabelCount DTOs representing the count of active employees grouped by their department names
    @Query("""
            SELECT new com.company.leavemanagement.dto.response.LabelCount(d.departmentName, COUNT(e))
            FROM Employee e JOIN e.department d
            WHERE e.active = true
            GROUP BY d.departmentName
            ORDER BY d.departmentName
            """)
    List<LabelCount> countActiveByDepartment();

    //returns a list of LabelCount DTOs representing the count of active employees grouped by their role names
    @Query("SELECT e FROM Employee e WHERE e.active = true AND e.role.roleName <> :roleName")
    List<Employee> findActiveExcludingRole(@Param("roleName") String roleName);
}
