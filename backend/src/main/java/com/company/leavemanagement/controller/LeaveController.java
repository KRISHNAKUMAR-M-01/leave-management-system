package com.company.leavemanagement.controller;

import com.company.leavemanagement.dto.request.LeaveApplyRequest;
import com.company.leavemanagement.dto.request.LeaveRejectRequest;
import com.company.leavemanagement.dto.response.LeaveRequestResponse;
import com.company.leavemanagement.dto.response.PageResponse;
import com.company.leavemanagement.enums.LeaveStatus;
import com.company.leavemanagement.service.LeaveApprovalService;
import com.company.leavemanagement.service.LeaveHistoryService;
import com.company.leavemanagement.service.LeaveRequestService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/leaves")
public class LeaveController {

    private final LeaveRequestService leaveRequestService;
    private final LeaveApprovalService leaveApprovalService;
    private final LeaveHistoryService leaveHistoryService;

    public LeaveController(LeaveRequestService leaveRequestService,
                           LeaveApprovalService leaveApprovalService,
                           LeaveHistoryService leaveHistoryService) {
        this.leaveRequestService = leaveRequestService;
        this.leaveApprovalService = leaveApprovalService;
        this.leaveHistoryService = leaveHistoryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LeaveRequestResponse applyForLeave(
            @AuthenticationPrincipal UserDetails currentUser,
            @Valid @RequestBody LeaveApplyRequest request) {
        return leaveRequestService.applyForLeave(currentUser.getUsername(), request);
    }

    @GetMapping("/my")
    public PageResponse<LeaveRequestResponse> getMyLeaves(
            @AuthenticationPrincipal UserDetails currentUser,
            @RequestParam(required = false) LeaveStatus status,
            @RequestParam(required = false) Long leaveTypeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return leaveHistoryService.getMyLeaves(
                currentUser.getUsername(),
                status,
                leaveTypeId,
                fromDate,
                toDate,
                page,
                size);
    }

    @GetMapping
    public PageResponse<LeaveRequestResponse> getAllLeaves(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) LeaveStatus status,
            @RequestParam(required = false) Long leaveTypeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return leaveHistoryService.getAllLeaves(
                employeeId,
                status,
                leaveTypeId,
                fromDate,
                toDate,
                page,
                size);
    }

    @GetMapping("/pending")
    public List<LeaveRequestResponse> getPendingRequests(@AuthenticationPrincipal UserDetails currentUser) {
        return leaveApprovalService.getPendingRequests(currentUser.getUsername());
    }

    @PatchMapping("/{id}/approve")
    public LeaveRequestResponse approve(@PathVariable Long id, @AuthenticationPrincipal UserDetails currentUser) {
        return leaveApprovalService.approve(id, currentUser.getUsername());
    }

    @PatchMapping("/{id}/reject")
    public LeaveRequestResponse reject(@PathVariable Long id, @AuthenticationPrincipal UserDetails currentUser, @Valid @RequestBody LeaveRejectRequest request) {
        return leaveApprovalService.reject(id, currentUser.getUsername(), request.comment());
    }

    @PatchMapping("/{id}/cancel")
    public LeaveRequestResponse cancel(@PathVariable Long id, @AuthenticationPrincipal UserDetails currentUser) {
        return leaveRequestService.cancel(id, currentUser.getUsername());
    }
}