package com.company.leavemanagement.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.company.leavemanagement.dto.request.LeaveApplyRequest;
import com.company.leavemanagement.dto.response.LeaveRequestResponse;
import com.company.leavemanagement.entity.Employee;
import com.company.leavemanagement.entity.LeaveBalance;
import com.company.leavemanagement.entity.LeavePolicyRule;
import com.company.leavemanagement.entity.LeaveRequest;
import com.company.leavemanagement.entity.LeaveType;
import com.company.leavemanagement.enums.LeaveStatus;
import com.company.leavemanagement.exception.BusinessRuleException;
import com.company.leavemanagement.exception.ResourceNotFoundException;
import com.company.leavemanagement.policy.BaseLeavePolicy;
import com.company.leavemanagement.policy.LeavePolicyFactory;
import com.company.leavemanagement.repository.EmployeeRepository;
import com.company.leavemanagement.repository.LeaveBalanceRepository;
import com.company.leavemanagement.repository.LeavePolicyRuleRepository;
import com.company.leavemanagement.repository.LeaveRequestRepository;
import com.company.leavemanagement.repository.LeaveTypeRepository;

@Service
public class LeaveRequestService {

    private static final List<LeaveStatus> BLOCKING_STATUSES = List.of(LeaveStatus.PENDING, LeaveStatus.APPROVED);
    private static final BigDecimal HALF_DAY = new BigDecimal("0.5");

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final EmployeeRepository employeeRepository;
    private final WorkingDayCalculator workingDayCalculator;
    private final LeavePolicyFactory leavePolicyFactory;
    private final LeavePolicyRuleRepository leavePolicyRuleRepository;

    public LeaveRequestService(
            LeaveRequestRepository leaveRequestRepository,
            LeaveBalanceRepository leaveBalanceRepository,
            LeaveTypeRepository leaveTypeRepository,
            EmployeeRepository employeeRepository,
            WorkingDayCalculator workingDayCalculator,
            LeavePolicyFactory leavePolicyFactory,
            LeavePolicyRuleRepository leavePolicyRuleRepository) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.leaveTypeRepository = leaveTypeRepository;
        this.employeeRepository = employeeRepository;
        this.workingDayCalculator = workingDayCalculator;
        this.leavePolicyFactory = leavePolicyFactory;
        this.leavePolicyRuleRepository = leavePolicyRuleRepository;
    }

    // Public API Methods
    @Transactional
    public LeaveRequestResponse applyForLeave(String email, LeaveApplyRequest request) {
        return applyForLeave(email, request, null);
    }

    @Transactional
    public LeaveRequestResponse applyForLeave(String email, LeaveApplyRequest request, MultipartFile medicalDocument) {
        Employee employee = findEmployeeByEmail(email);
        LeaveType leaveType = findLeaveType(request.leaveTypeId());

        int year = request.startDate().getYear();
        LeaveBalance balance = leaveBalanceRepository
                .findForUpdate(employee, leaveType, year)
                .orElseThrow(() -> new BusinessRuleException(
                        "No " + leaveType.getLeaveTypeName() + " leave balance found for " + year));

        BigDecimal totalDays = calculateTotalDays(request);
        validateNoOverlap(employee, request);

        // Calculate available balance taking pending requests into account
        LocalDate fromDate = LocalDate.of(year, 1, 1);
        LocalDate toDate = LocalDate.of(year, 12, 31);
        BigDecimal pendingDays = leaveRequestRepository.sumDaysByEmployeeAndLeaveTypeAndStatus(
                employee, leaveType, LeaveStatus.PENDING, fromDate, toDate);
        BigDecimal effectiveAvailable = balance.getRemainingDays().subtract(pendingDays);

        // Fetch policy rule for this role and leave type (if configured in leave_policy_rules)
        LeavePolicyRule rule = leavePolicyRuleRepository
                .findByRoleAndLeaveType(employee.getRole(), leaveType)
                .orElse(null);

        // OOP Polymorphic validation using Abstract Class & Factory
        BaseLeavePolicy policy = leavePolicyFactory.getPolicy(leaveType.getLeaveTypeName());
        policy.validate(employee, request, totalDays, effectiveAvailable, rule, medicalDocument);

        LeaveRequest leaveRequest = new LeaveRequest(
                employee,
                leaveType,
                request.startDate(),
                request.endDate(),
                totalDays,
                request.halfDaySession(),
                request.reason().trim());

        if (medicalDocument != null && !medicalDocument.isEmpty()) {
            attachMedicalDocument(leaveRequest, medicalDocument);
        }

        return LeaveRequestResponse.from(leaveRequestRepository.save(leaveRequest));
    }

    @Transactional
    public LeaveRequestResponse cancel(Long requestId, String email) {
        Employee employee = findEmployeeByEmail(email);
        LeaveRequest request = leaveRequestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Leave request not found with id " + requestId));

        if (!request.getEmployee().getId().equals(employee.getId())) {
            throw new AccessDeniedException("You can cancel only your own leave requests");
        }

        LeaveStatus status = request.getLeaveStatus();
        if (status == LeaveStatus.APPROVED) {
            validateStartDateIsInFuture(request);
            restoreBalance(request);
        } else if (status != LeaveStatus.PENDING) {
            throw new BusinessRuleException(
                    "A " + status.name().toLowerCase() + " request cannot be cancelled");
        }

        request.cancel();
        return LeaveRequestResponse.from(request);
    }

    @Transactional(readOnly = true)
    public MedicalDocumentData getMedicalDocument(Long requestId, String viewerEmail) {
        Employee viewer = findEmployeeByEmail(viewerEmail);
        LeaveRequest request = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Leave request not found with id " + requestId));

        Employee applicant = request.getEmployee();
        boolean isOwner = applicant.getId().equals(viewer.getId());
        boolean isManager = applicant.getManager() != null
                && applicant.getManager().getId().equals(viewer.getId());
        boolean isAdmin = "ADMIN".equals(viewer.getRole().getRoleName());

        if (!isOwner && !isManager && !isAdmin) {
            throw new AccessDeniedException("You are not allowed to view this medical document");
        }
        if (!request.hasMedicalDocument()) {
            throw new ResourceNotFoundException("Medical document not found for this leave request");
        }

        return new MedicalDocumentData(
                request.getMedicalDocumentName(),
                request.getMedicalDocumentType(),
                request.getMedicalDocument());
    }

    // Leave Application Helpers
    private BigDecimal calculateTotalDays(LeaveApplyRequest request) {
        int workingDays = workingDayCalculator.countWorkingDays(
                request.startDate(), request.endDate());

        if (workingDays == 0) {
            throw new BusinessRuleException("The selected dates contain only weekends or public holidays");
        }
        if (request.halfDaySession() != null) {
            return HALF_DAY;
        }
        return BigDecimal.valueOf(workingDays);
    }

    private void validateNoOverlap(Employee employee, LeaveApplyRequest request) {
        boolean overlaps = leaveRequestRepository.existsOverlappingLeave(
                employee, BLOCKING_STATUSES, request.startDate(), request.endDate());

        if (overlaps) {
            throw new BusinessRuleException("These dates overlap with an existing pending or approved leave");
        }
    }

    private void attachMedicalDocument(LeaveRequest leaveRequest, MultipartFile file) {
        String original = file.getOriginalFilename();
        String safeName = (original == null || original.isBlank())
                ? "medical-document"
                : Paths.get(original).getFileName().toString();
        String contentType = "image/jpg".equalsIgnoreCase(file.getContentType())
                ? "image/jpeg"
                : file.getContentType();
        try {
            leaveRequest.attachMedicalDocument(safeName, contentType, file.getBytes());
        } catch (IOException ex) {
            throw new BusinessRuleException("Failed to read uploaded medical document");
        }
    }

    // Leave Cancellation Helpers
    private void validateStartDateIsInFuture(LeaveRequest request) {
        if (!request.getStartDate().isAfter(LocalDate.now())) {
            throw new BusinessRuleException(
                    "Approved leave can be cancelled only before its start date");
        }
    }

    private void restoreBalance(LeaveRequest request) {
        LeaveBalance balance = leaveBalanceRepository
                .findForUpdate(request.getEmployee(), request.getLeaveType(),
                        request.getStartDate().getYear())
                .orElseThrow(() -> new BusinessRuleException(
                        "Leave balance not found for this request"));

        balance.restore(request.getTotalDays());
    }

    // Entity Lookup Helpers
    private Employee findEmployeeByEmail(String email) {
        return employeeRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
    }

    private LeaveType findLeaveType(Long leaveTypeId) {
        return leaveTypeRepository.findById(leaveTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave type not found with id " + leaveTypeId));
    }

    // Inner Records
    public record MedicalDocumentData(String fileName, String contentType, byte[] data) {}
}