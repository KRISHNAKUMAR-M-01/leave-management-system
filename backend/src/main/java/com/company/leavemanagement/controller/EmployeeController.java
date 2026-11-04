package com.company.leavemanagement.controller;

import com.company.leavemanagement.dto.request.EmployeeCreateRequest;
import com.company.leavemanagement.dto.request.EmployeeUpdateRequest;
import com.company.leavemanagement.dto.response.EmployeeResponse;
import com.company.leavemanagement.dto.response.PageResponse;
import com.company.leavemanagement.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeResponse createEmployee(@Valid @RequestBody EmployeeCreateRequest request) {
        return employeeService.createEmployee(request);
    }

    @GetMapping
    public PageResponse<EmployeeResponse> searchEmployees(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String roleName,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "fullName") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        return employeeService.searchEmployees(search, departmentId, roleName, active, page, size, sortBy, direction);
    }

    @GetMapping("/{id}")
    public EmployeeResponse getEmployeeById(@PathVariable Long id) {
        return employeeService.getEmployeeById(id);
    }

    @PutMapping("/{id}")
    public EmployeeResponse updateEmployee(@PathVariable Long id, @Valid @RequestBody EmployeeUpdateRequest request) {
        return employeeService.updateEmployee(id, request);
    }

    @PatchMapping("/{id}/deactivate")
    public EmployeeResponse deactivateEmployee(@PathVariable Long id, @AuthenticationPrincipal UserDetails currentUser) {
        return employeeService.deactivateEmployee(id, currentUser.getUsername());
    }

    @PatchMapping("/{id}/activate")
    public EmployeeResponse activateEmployee(@PathVariable Long id) {
        return employeeService.activateEmployee(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEmployee(@PathVariable Long id, @AuthenticationPrincipal UserDetails currentUser) {
        employeeService.deleteEmployee(id, currentUser.getUsername());
    }
}
