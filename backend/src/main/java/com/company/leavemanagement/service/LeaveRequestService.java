package com.company.leavemanagement.service;

import com.company.leavemanagement.dto.request.LeaveApplyRequest;
import com.company.leavemanagement.dto.response.LeaveRequestResponse;
import com.company.leavemanagement.entity.Employee;
import com.company.leavemanagement.entity.LeaveBalance;
import com.company.leavemanagement.entity.LeaveRequest;
import com.company.leavemanagement.entity.LeaveType;
import com.company.leavemanagement.enums.LeaveStatus;
import com.company.leavemanagement.exception.BusinessRuleException;
import com.company.leavemanagement.exception.ResourceNotFoundException;
import com.company.leavemanagement.repository.EmployeeRepository;
import com.company.leavemanagement.repository.LeaveBalanceRepository;
import com.company.leavemanagement.repository.LeaveRequestRepository;
import com.company.leavemanagement.repository.LeaveTypeRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
public class LeaveRequestService {
    private static final List<LeaveStatus> BLOCKING_STATUSES = List.of(LeaveStatus.PENDING, LeaveStatus.APPROVED);
    private static final BigDecimal HALF_DAY = new BigDecimal("0.5");
    private static final BigDecimal MEDICAL_DOC_THRESHOLD_DAYS = new BigDecimal("3");
    private static final long MAX_MEDICAL_DOC_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_MEDICAL_DOC_TYPES = Set.of(
            "application/pdf", "image/jpeg", "image/jpg", "image/png");

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final EmployeeRepository employeeRepository;
    private final WorkingDayCalculator workingDayCalculator;

    public LeaveRequestService(LeaveRequestRepository leaveRequestRepository, LeaveBalanceRepository leaveBalanceRepository, LeaveTypeRepository leaveTypeRepository, EmployeeRepository employeeRepository, WorkingDayCalculator workingDayCalculator) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.leaveTypeRepository = leaveTypeRepository;
        this.employeeRepository = employeeRepository;
        this.workingDayCalculator = workingDayCalculator;
    }

    @Transactional
    public LeaveRequestResponse applyForLeave(String email, LeaveApplyRequest request) {
        return applyForLeave(email, request, null);
    }

    @Transactional
    public LeaveRequestResponse applyForLeave(String email, LeaveApplyRequest request, MultipartFile medicalDocument) {
        Employee employee = findEmployeeByEmail(email);
        LeaveType leaveType = findLeaveType(request.leaveTypeId());

        validateDates(request);
        BigDecimal totalDays = calculateTotalDays(request);
        int year = request.startDate().getYear();
        LeaveBalance balance = leaveBalanceRepository
                .findForUpdate(employee, leaveType, year)
                .orElseThrow(() -> new BusinessRuleException(
                        "No " + leaveType.getLeaveTypeName() + " leave balance found for " + year));

        validateNoOverlap(employee, request);
        validateBalance(balance, employee, leaveType, year, totalDays);
        validateMedicalDocumentRequirement(leaveType, totalDays, medicalDocument);

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

    public record MedicalDocumentData(String fileName, String contentType, byte[] data) {}

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

    private Employee findEmployeeByEmail(String email) {
        return employeeRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
    }

    private LeaveType findLeaveType(Long leaveTypeId) {
        return leaveTypeRepository.findById(leaveTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave type not found with id " + leaveTypeId));
    }

    private void validateDates(LeaveApplyRequest request) {
        LocalDate startDate = request.startDate();
        LocalDate endDate = request.endDate();

        if (endDate.isBefore(startDate)) {
            throw new BusinessRuleException("End date cannot be before start date");
        }
        if (startDate.isBefore(LocalDate.now())) {
            throw new BusinessRuleException("Start date cannot be in the past");
        }
        if (startDate.getYear() != endDate.getYear()) {
            throw new BusinessRuleException(
                    "Leave must start and end in the same calendar year. Apply separately for each year");
        }
        if (request.halfDaySession() != null && !startDate.equals(endDate)) {
            throw new BusinessRuleException("A half-day leave must be for a single date");
        }
    }

    private BigDecimal calculateTotalDays(LeaveApplyRequest request) {
        int workingDays = workingDayCalculator.countWorkingDays(
                request.startDate(), request.endDate());

        if(workingDays == 0) {
            throw new BusinessRuleException("The selected dates contain only weekends or public holidays");
        }
        if(request.halfDaySession() != null) {
            return HALF_DAY;
        }
        return BigDecimal.valueOf(workingDays);
    }

    private void validateNoOverlap(Employee employee, LeaveApplyRequest request) {
        boolean overlaps = leaveRequestRepository.existsOverlappingLeave(employee, BLOCKING_STATUSES, request.startDate(), request.endDate());

        if (overlaps) {
            throw new BusinessRuleException("These dates overlap with an existing pending or approved leave");
        }
    }

    private void validateBalance(LeaveBalance balance, Employee employee, LeaveType leaveType,
                                 int year, BigDecimal requestedDays) {
        LocalDate fromDate = LocalDate.of(year, 1, 1);
        LocalDate toDate = LocalDate.of(year, 12, 31);

        BigDecimal pendingDays = leaveRequestRepository.sumDaysByEmployeeAndLeaveTypeAndStatus(
                employee, leaveType, LeaveStatus.PENDING, fromDate, toDate);

        BigDecimal effectiveAvailable = balance.getRemainingDays().subtract(pendingDays);

        if (requestedDays.compareTo(effectiveAvailable) > 0) {
            if (pendingDays.compareTo(BigDecimal.ZERO) > 0) {
                throw new BusinessRuleException(
                        "Requested " + requestedDays + " day(s) exceed your available balance. You have "
                                + balance.getRemainingDays() + " day(s) remaining, but "
                                + pendingDays + " day(s) are already pending approval. Available: "
                                + effectiveAvailable + " day(s).");
            } else {
                throw new BusinessRuleException(
                        "Requested " + requestedDays + " day(s) exceed your available balance of "
                                + balance.getRemainingDays() + " day(s)");
            }
        }
    }

    private void validateMedicalDocumentRequirement(LeaveType leaveType, BigDecimal totalDays, MultipartFile file) {
        boolean isSick = "Sick".equalsIgnoreCase(leaveType.getLeaveTypeName().trim());
        if (isSick && totalDays.compareTo(MEDICAL_DOC_THRESHOLD_DAYS) >= 0) {
            if (file == null || file.isEmpty()) {
                throw new BusinessRuleException(
                        "A medical document is required for sick leave of 3 or more days");
            }
        }
        if (file != null && !file.isEmpty()) {
            if (file.getSize() > MAX_MEDICAL_DOC_BYTES) {
                throw new BusinessRuleException("Medical document must be 5 MB or smaller");
            }
            String contentType = file.getContentType();
            if (contentType == null || !ALLOWED_MEDICAL_DOC_TYPES.contains(contentType.toLowerCase())) {
                throw new BusinessRuleException("Only PDF, JPG, or PNG medical documents are allowed");
            }
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
}

