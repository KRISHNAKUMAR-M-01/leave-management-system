package com.company.leavemanagement.service;

import com.company.leavemanagement.dto.response.LeaveRequestResponse;
import com.company.leavemanagement.entity.Employee;
import com.company.leavemanagement.entity.LeaveBalance;
import com.company.leavemanagement.entity.LeaveRequest;
import com.company.leavemanagement.enums.LeaveStatus;
import com.company.leavemanagement.exception.BusinessRuleException;
import com.company.leavemanagement.exception.ResourceNotFoundException;
import com.company.leavemanagement.repository.EmployeeRepository;
import com.company.leavemanagement.repository.LeaveBalanceRepository;
import com.company.leavemanagement.repository.LeaveRequestRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LeaveApprovalService {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_MANAGER = "MANAGER";

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final EmployeeRepository employeeRepository;

    public LeaveApprovalService(LeaveRequestRepository leaveRequestRepository,
                                LeaveBalanceRepository leaveBalanceRepository,
                                EmployeeRepository employeeRepository) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.employeeRepository = employeeRepository;
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> getPendingRequests(String approverEmail) {
        Employee approver = findEmployeeByEmail(approverEmail);

        List<LeaveRequest> pending = isAdmin(approver)
                ? leaveRequestRepository.findByStatusAndApplicantRole(
                LeaveStatus.PENDING, ROLE_MANAGER)
                : leaveRequestRepository.findByStatusAndManager(
                LeaveStatus.PENDING, approver);

        return pending.stream().map(LeaveRequestResponse::from).toList();
    }

    @Transactional
    public LeaveRequestResponse approve(Long requestId, String approverEmail) {
        Employee approver = findEmployeeByEmail(approverEmail);
        LeaveRequest request = findPendingRequestForUpdate(requestId);
        validateApproverIsAllowed(approver, request);

        LeaveBalance balance = leaveBalanceRepository
                .findForUpdate(request.getEmployee(), request.getLeaveType(),
                        request.getStartDate().getYear())
                .orElseThrow(() -> new BusinessRuleException(
                        "The employee has no leave balance for this leave type and year"));

        if (request.getTotalDays().compareTo(balance.getRemainingDays()) > 0) {
            throw new BusinessRuleException(
                    "Cannot approve: the employee's remaining balance is only "
                            + balance.getRemainingDays() + " days");
        }

        balance.deduct(request.getTotalDays());
        request.approve(approver);
        return LeaveRequestResponse.from(request);
    }

    @Transactional
    public LeaveRequestResponse reject(Long requestId, String approverEmail, String comment) {
        Employee approver = findEmployeeByEmail(approverEmail);
        LeaveRequest request = findPendingRequestForUpdate(requestId);
        validateApproverIsAllowed(approver, request);

        request.reject(approver, comment.trim());
        return LeaveRequestResponse.from(request);
    }

    private Employee findEmployeeByEmail(String email) {
        return employeeRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
    }

    private LeaveRequest findPendingRequestForUpdate(Long requestId) {
        LeaveRequest request = leaveRequestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Leave request not found with id " + requestId));

        if (request.getLeaveStatus() != LeaveStatus.PENDING) {
            throw new BusinessRuleException("Only pending requests can be approved or rejected");
        }
        return request;
    }

    private void validateApproverIsAllowed(Employee approver, LeaveRequest request) {
        Employee applicant = request.getEmployee();

        if (approver.getId().equals(applicant.getId())) {
            throw new BusinessRuleException("You cannot review your own leave request");
        }

        boolean applicantIsManager = ROLE_MANAGER.equals(applicant.getRole().getRoleName());
        boolean allowed = applicantIsManager
                ? isAdmin(approver)
                : isManagerOf(approver, applicant);

        if (!allowed) {
            throw new AccessDeniedException("You are not allowed to review this request");
        }
    }

    private boolean isManagerOf(Employee approver, Employee applicant) {
        Employee manager = applicant.getManager();
        return manager != null && manager.getId().equals(approver.getId());
    }

    private boolean isAdmin(Employee employee) {
        return ROLE_ADMIN.equals(employee.getRole().getRoleName());
    }
}