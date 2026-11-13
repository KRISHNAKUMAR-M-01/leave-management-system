package com.company.leavemanagement.controller;

import com.company.leavemanagement.dto.response.LeaveTypeResponse;
import com.company.leavemanagement.service.LeaveTypeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/leave-types")
public class LeaveTypeController {

    private final LeaveTypeService leaveTypeService;

    public LeaveTypeController(LeaveTypeService leaveTypeService) {
        this.leaveTypeService = leaveTypeService;
    }

    @GetMapping
    public List<LeaveTypeResponse> getAllLeaveTypes() {
        return leaveTypeService.getAllLeaveTypes();
    }
}
