package com.company.leavemanagement.service;

import com.company.leavemanagement.dto.response.AdminDashboardResponse;
import com.company.leavemanagement.dto.response.LeaveRequestResponse;
import com.company.leavemanagement.dto.response.ManagerDashboardResponse;
import com.company.leavemanagement.entity.Employee;
import com.company.leavemanagement.enums.LeaveStatus;
import com.company.leavemanagement.exception.BusinessRuleException;
import com.company.leavemanagement.exception.ResourceNotFoundException;
import com.company.leavemanagement.repository.EmployeeRepository;
import com.company.leavemanagement.repository.LeaveRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private static final String ROLE_MANAGER = "MANAGER";
    private static final int MAX_CALENDAR_DAYS = 92;
    private static final List<LeaveStatus> CALENDAR_STATUSES =
            List.of(LeaveStatus.PENDING, LeaveStatus.APPROVED);

    private final EmployeeRepository employeeRepository;
    private final LeaveRequestRepository leaveRequestRepository;

    public DashboardService(EmployeeRepository employeeRepository,
                            LeaveRequestRepository leaveRequestRepository) {
        this.employeeRepository = employeeRepository;
        this.leaveRequestRepository = leaveRequestRepository;
    }

    @Transactional(readOnly = true)
    public ManagerDashboardResponse getManagerDashboard(String email) {
        Employee manager = findEmployeeByEmail(email);

        return new ManagerDashboardResponse(
                leaveRequestRepository.countForManager(LeaveStatus.PENDING, manager),
                employeeRepository.countByManagerAndActiveTrue(manager));
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> getTeamCalendar(String email, LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new BusinessRuleException("From date cannot be after to date");
        }
        if (ChronoUnit.DAYS.between(from, to) > MAX_CALENDAR_DAYS) {
            throw new BusinessRuleException(
                    "Date range cannot exceed " + MAX_CALENDAR_DAYS + " days");
        }

        Employee manager = findEmployeeByEmail(email);
        return leaveRequestRepository.findTeamLeaves(manager, CALENDAR_STATUSES, from, to)
                .stream()
                .map(LeaveRequestResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse getAdminDashboard() {
        int year = LocalDate.now().getYear();
        LocalDate firstDay = LocalDate.of(year, 1, 1);
        LocalDate lastDay = LocalDate.of(year, 12, 31);

        Map<LeaveStatus, Long> leavesByStatus = new EnumMap<>(LeaveStatus.class);
        for (LeaveStatus status : LeaveStatus.values()) {
            leavesByStatus.put(status, 0L);
        }
        leaveRequestRepository.countGroupedByStatus(firstDay, lastDay)
                .forEach(item -> leavesByStatus.put(item.status(), item.count()));

        return new AdminDashboardResponse(
                employeeRepository.count(),
                employeeRepository.countByActiveTrue(),
                leaveRequestRepository.countByStatusAndApplicantRole(
                        LeaveStatus.PENDING, ROLE_MANAGER),
                year,
                leavesByStatus,
                leaveRequestRepository.sumDaysGroupedByLeaveType(
                        LeaveStatus.APPROVED, firstDay, lastDay),
                employeeRepository.countActiveByDepartment());
    }

    private Employee findEmployeeByEmail(String email) {
        return employeeRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
    }
}