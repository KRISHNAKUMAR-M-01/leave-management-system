package com.company.leavemanagement.controller;

import com.company.leavemanagement.dto.response.LeaveBalanceResponse;
import com.company.leavemanagement.service.LeaveBalanceService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/leave-balances")
public class LeaveBalanceController {

    private final LeaveBalanceService leaveBalanceService;

    public LeaveBalanceController(LeaveBalanceService leaveBalanceService) {
        this.leaveBalanceService = leaveBalanceService;
    }

    @GetMapping("/me")
    public List<LeaveBalanceResponse> getMyBalances(
            @AuthenticationPrincipal UserDetails currentUser) {
        return leaveBalanceService.getMyBalances(currentUser.getUsername());
    }

    @GetMapping("/employee/{id}")
    public List<LeaveBalanceResponse> getEmployeeBalances(@PathVariable Long id) {
        return leaveBalanceService.getBalancesByEmployeeId(id);
    }

    @PostMapping("/generate/{year}")
    public Map<String, Integer> generateBalances(@PathVariable int year) {
        return Map.of("created", leaveBalanceService.createBalancesForYear(year));
    }
}
