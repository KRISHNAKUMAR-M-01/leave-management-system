package com.company.leavemanagement.controller;

import com.company.leavemanagement.dto.response.AdminDashboardResponse;
import com.company.leavemanagement.dto.response.EmployeeResponse;
import com.company.leavemanagement.dto.response.LeaveRequestResponse;
import com.company.leavemanagement.dto.response.ManagerDashboardResponse;
import com.company.leavemanagement.service.DashboardService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/manager")
    public ManagerDashboardResponse getManagerDashboard(
            @AuthenticationPrincipal UserDetails currentUser) {
        return dashboardService.getManagerDashboard(currentUser.getUsername());
    }

    @GetMapping("/team-calendar")
    public List<LeaveRequestResponse> getTeamCalendar(
            @AuthenticationPrincipal UserDetails currentUser,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return dashboardService.getTeamCalendar(currentUser.getUsername(), from, to);
    }

    @GetMapping("/team-members")
    public List<EmployeeResponse> getTeamMembers(
            @AuthenticationPrincipal UserDetails currentUser) {
        return dashboardService.getTeamMembers(currentUser.getUsername());
    }

    @GetMapping("/admin")
    public AdminDashboardResponse getAdminDashboard() {
        return dashboardService.getAdminDashboard();
    }
}