package com.company.leavemanagement.service;

import com.company.leavemanagement.dto.response.LoginResponse;
import com.company.leavemanagement.dto.request.LoginRequest;
import com.company.leavemanagement.entity.Employee;
import com.company.leavemanagement.repository.EmployeeRepository;
import com.company.leavemanagement.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final EmployeeRepository employeeRepository;
    private final JwtService jwtService;

    public AuthService(AuthenticationManager authenticationManager, EmployeeRepository employeeRepository, JwtService jwtService) {

        this.authenticationManager = authenticationManager;
        this.employeeRepository = employeeRepository;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(), request.password()));

        Employee employee = employeeRepository.findByEmail(
                request.email()).orElseThrow(() -> new IllegalStateException("Authenticated employee not found"));

        String role = employee.getRole().getRoleName();
        String token = jwtService.generateToken(employee.getEmail(),role);

        return new LoginResponse(token, employee.getEmail(), employee.getFullName(), role, employee.getGender());

    }
}
