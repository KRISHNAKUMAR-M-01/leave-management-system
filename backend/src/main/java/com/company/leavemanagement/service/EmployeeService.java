package com.company.leavemanagement.service;

import com.company.leavemanagement.dto.request.EmployeeCreateRequest;
import com.company.leavemanagement.dto.request.EmployeeUpdateRequest;
import com.company.leavemanagement.dto.response.EmployeeResponse;
import com.company.leavemanagement.entity.Department;
import com.company.leavemanagement.entity.Employee;
import com.company.leavemanagement.entity.Role;
import com.company.leavemanagement.exception.BusinessRuleException;
import com.company.leavemanagement.exception.ResourceNotFoundException;
import com.company.leavemanagement.repository.DepartmentRepository;
import com.company.leavemanagement.repository.EmployeeRepository;
import com.company.leavemanagement.repository.LeaveRequestRepository;
import com.company.leavemanagement.repository.RoleRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.company.leavemanagement.dto.response.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

import java.time.LocalDate;

@Service
public class EmployeeService {
    private static final String ROLE_EMPLOYEE = "EMPLOYEE";
    private static final String ROLE_MANAGER = "MANAGER";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final Set<String> SORTABLE_FIELDS = Set.of("fullName", "email", "joiningDate", "designation");
    private static final int MAX_PAGE_SIZE = 50;

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final LeaveBalanceService leaveBalanceService;
    private final LeaveRequestRepository leaveRequestRepository;

    public EmployeeService(EmployeeRepository employeeRepository, DepartmentRepository departmentRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder, LeaveBalanceService leaveBalanceService, LeaveRequestRepository leaveRequestRepository) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.roleRepository =roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.leaveBalanceService = leaveBalanceService;
        this.leaveRequestRepository = leaveRequestRepository;
    }

    @Transactional
    public EmployeeResponse createEmployee(EmployeeCreateRequest request) {
        String email = request.email().trim().toLowerCase();
        validateEmailIsNotTaken(email);

        Department department = findDepartment(request.departmentId());
        Role role = findRole(request.roleName());
        Employee manager = resolveManager(request.roleName(), request.managerId());

        Employee employee = new Employee(
                request.fullName().trim(),
                email,
                passwordEncoder.encode(request.password()),
                request.phone(),
                request.designation().trim(),
                request.joiningDate(),
                department,
                role
                );
        if (request.gender() != null && !request.gender().isBlank()) {
            employee.setGender(request.gender());
        }
        employee.assignManager(manager);

        Employee savedEmployee = employeeRepository.save(employee);
        if (!ROLE_ADMIN.equals(request.roleName())) {
            leaveBalanceService.createYearlyBalances(savedEmployee, LocalDate.now().getYear());
        }
        return EmployeeResponse.from(savedEmployee);
    }

    @Transactional
    public EmployeeResponse updateEmployee(Long id, EmployeeUpdateRequest request) {
        Employee employee = findEmployee(id);

        String email = request.email().trim().toLowerCase();
        validateEmailIsNotTakenByAnother(email, id);

        Department department = findDepartment(request.departmentId());
        Role role = findRole(request.roleName());
        validateRoleChange(employee, request.roleName());

        Employee manager = resolveManager(request.roleName(), request.managerId());
        validateNotOwnManager(employee, manager);

        employee.updateDetails(
                request.fullName().trim(),
                email,
                request.phone(),
                request.designation().trim(),
                request.joiningDate(),
                department,
                role);
        if (request.gender() != null && !request.gender().isBlank()) {
            employee.setGender(request.gender());
        }
        employee.assignManager(manager);

        return EmployeeResponse.from(employee);
    }

    @Transactional
    public EmployeeResponse deactivateEmployee(Long id, String currentUserEmail) {
        Employee employee = findEmployee(id);
        validateNotCurrentUser(employee, currentUserEmail, "deactivate");

        if (!employee.isActive()) {
            throw new BusinessRuleException("Employee is already deactivated");
        }
        validateManagerHasNoActiveTeam(employee);

        employee.deactivate();
        return EmployeeResponse.from(employee);
    }

    @Transactional
    public EmployeeResponse activateEmployee(Long id) {
        Employee employee = findEmployee(id);

        if (employee.isActive()) {
            throw new BusinessRuleException("Employee is already active");
        }

        Employee manager = employee.getManager();
        if (manager != null && !manager.isActive()){
            throw new BusinessRuleException("The reporting manager is deactivated. Assign an active manager first");
        }

        employee.activate();
        return EmployeeResponse.from(employee);
    }

    @Transactional
    public void deleteEmployee(Long id, String currentUserEmail) {
        Employee employee = findEmployee(id);
        validateNotCurrentUser(employee, currentUserEmail, "delete");
        validateHasNoLeaveRecords(employee);

        if (employeeRepository.existsByManager(employee)) {
            throw new BusinessRuleException("This employee is the reporting manager of others. Reassign them first");
        }

        leaveBalanceService.deleteBalancesOf(employee);
        employeeRepository.delete(employee);
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getEmployeeById(Long id) {
        return EmployeeResponse.from(findEmployee(id));
    }
    private Employee findEmployee(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id " + id));
    }

    @Transactional(readOnly = true)
    public PageResponse<EmployeeResponse>
    searchEmployees(String search,
                    Long departmentId,
                    String roleName,
                    Boolean active,
                    int page,
                    int size,
                    String sortBy,
                    String direction) {
        Pageable  pageable = buildPageable(page, size, sortBy, direction);

        Page<EmployeeResponse> result = employeeRepository
                .search(blankToNull(search), departmentId, blankToNull(roleName), active, pageable)
                .map(EmployeeResponse::from);
        return PageResponse.from(result);
    }

    private Pageable buildPageable(int page, int size, String sortBy, String direction) {
        if (!SORTABLE_FIELDS.contains(sortBy)) {
            throw new BusinessRuleException("Cannot sort by '" + sortBy + "'");
        }
        String jpaField = switch (sortBy) {
            case "departmentName" -> "department.departmentName";
            case "roleName" -> "role.roleName";
            default -> sortBy;
        };
        Sort.Direction sortDirection = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sort = Sort.by(sortDirection, jpaField).and(Sort.by(Sort.Direction.ASC, "id"));

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        return PageRequest.of(safePage, safeSize, sort);

    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null :value.trim();
    }

    private void validateEmailIsNotTaken(String email) {
        if(employeeRepository.existsByEmail(email)) {
            throw new BusinessRuleException("An employee with this email is already exists");
        }
    }

    private Department findDepartment(Long departmentId) {
        return departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id " + departmentId));
    }

    private Role findRole(String roleName) {
        return roleRepository.findByRoleName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));
    }

    private Employee resolveManager(String roleName, Long managerId) {
        if (!ROLE_EMPLOYEE.equals(roleName)) {
            if (managerId != null) {
                throw new BusinessRuleException("Only an employee with the EMPLOYEE role can have a reporting manager");
            }
            return null;
        }
        if (managerId == null) {
            throw new BusinessRuleException("A reporting manager is required for an employee");
        }
        Employee manager = employeeRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager not found with id " + managerId));

        if(!manager.isActive() || !ROLE_MANAGER.equals((manager.getRole().getRoleName()))) {
            throw new BusinessRuleException("The reporting manager must be an active employee with the MANAGER role");
        }
        return manager;
    }

    private void validateEmailIsNotTakenByAnother(String email, Long id) {
        if (employeeRepository.existsByEmailAndIdNot(email, id)) {
            throw new BusinessRuleException("Another employee already uses this email");
        }
    }

    private void validateRoleChange(Employee employee, String newRoleName) {
        String currentRoleName = employee.getRole().getRoleName();

        if (currentRoleName.equals(newRoleName)) {
            return;
        }
        if (ROLE_ADMIN.equals(currentRoleName) || ROLE_ADMIN.equals(newRoleName)) {
            throw new BusinessRuleException("The ADMIN role cannot be assigned or removed here");
        }
        if (ROLE_MANAGER.equals(currentRoleName)) {
            validateManagerHasNoActiveTeam(employee);
        }
    }

    private void validateNotOwnManager(Employee employee, Employee manager) {
        if (manager !=null && manager.getId().equals(employee.getId())) {
            throw new BusinessRuleException("An employee cannot be their own manager");
        }
    }

    private void validateManagerHasNoActiveTeam(Employee employee) {
        if (employeeRepository.existsByManagerAndActiveTrue(employee)) {
            throw new BusinessRuleException("This manager still has active team members. Reassign them first");
        }
    }

    private void validateNotCurrentUser(Employee employee, String currentUserEmail, String action) {
        if (employee.getEmail().equals(currentUserEmail)) {
            throw new BusinessRuleException("You cannot " + action + " Your own account");
        }
    }

    private void validateHasNoLeaveRecords(Employee employee) {
        if (leaveRequestRepository.existsByEmployee(employee) || leaveRequestRepository.existsByReviewedBy(employee)) {
            throw new BusinessRuleException("This employee has leave records and cannot be permanently deleted. Deactivate the employee instead");
        }
    }
}
