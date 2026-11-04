package com.company.leavemanagement.repository;

import com.company.leavemanagement.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee,Long> {

    Optional<Employee> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByManagerAndActiveTrue(Employee manager);

    boolean existsByManager(Employee manager);

    @EntityGraph(attributePaths = {"department", "role", "manager"})

    @Query("""
SELECT e FROM Employee e 
WHERE (:search IS NULL OR LOWER(e.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(e.email) LIKE LOWER(CONCAT('%', :search, '%')))
AND (:departmentId IS NULL OR e.department.id = :departmentId)
AND (:roleName IS NULL OR e.role.roleName = :roleName)
AND (:active IS NULL OR e.active = :active)
""")

    Page<Employee> search(@Param("search") String search,
                          @Param("departmentId") Long departmentId,
                          @Param("roleName") String roleName,
                          @Param("active") Boolean active,
                          Pageable pageable);
}
