package com.company.leavemanagement.service;

import com.company.leavemanagement.dto.response.LeaveRequestResponse;
import com.company.leavemanagement.dto.response.PageResponse;
import com.company.leavemanagement.entity.Employee;
import com.company.leavemanagement.enums.LeaveStatus;
import com.company.leavemanagement.exception.BusinessRuleException;
import com.company.leavemanagement.exception.ResourceNotFoundException;
import com.company.leavemanagement.repository.EmployeeRepository;
import com.company.leavemanagement.repository.LeaveRequestRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class LeaveHistoryService {

    private static final int MAX_PAGE_SIZE = 50;

    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;

    public LeaveHistoryService(LeaveRequestRepository leaveRequestRepository,
                               EmployeeRepository employeeRepository) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.employeeRepository = employeeRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<LeaveRequestResponse> getMyLeaves(String email, LeaveStatus status,
                                                          Long leaveTypeId, LocalDate fromDate,
                                                          LocalDate toDate, int page, int size) {
        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        return search(employee.getId(), status, leaveTypeId, fromDate, toDate, page, size);
    }

    @Transactional(readOnly = true)
    public PageResponse<LeaveRequestResponse> getAllLeaves(Long employeeId, LeaveStatus status,
                                                           Long leaveTypeId, LocalDate fromDate,
                                                           LocalDate toDate, int page, int size) {
        return search(employeeId, status, leaveTypeId, fromDate, toDate, page, size);
    }

    private PageResponse<LeaveRequestResponse> search(Long employeeId, LeaveStatus status,
                                                      Long leaveTypeId, LocalDate fromDate,
                                                      LocalDate toDate, int page, int size) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new BusinessRuleException("From date cannot be after to date");
        }

        Sort newestFirst = Sort.by(Sort.Direction.DESC, "startDate")
                .and(Sort.by(Sort.Direction.DESC, "id"));
        Pageable pageable = PageRequest.of(
                Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE), newestFirst);

        Page<LeaveRequestResponse> result = leaveRequestRepository
                .search(employeeId, status, leaveTypeId, fromDate, toDate, pageable)
                .map(LeaveRequestResponse::from);

        return PageResponse.from(result);
    }
}