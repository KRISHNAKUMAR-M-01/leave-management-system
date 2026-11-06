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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class LeaveRequestService {
    private static final List<LeaveStatus> BLOCKING_STATUSES = List.of(LeaveStatus.PENDING, LeaveStatus.APPROVED);
    private static final BigDecimal HALF_DAY = new BigDecimal("0.5");

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
        Employee employee = findEmployeeByEmail(email);
        LeaveType leaveType = findLeaveType(request.leaveTypeId());

        validateDates(request);
        BigDecimal totalDays = calculateTotalDays(request);
        validateNoOverlap(employee, request);
        validateBalance(employee, leaveType, request.startDate().getYear(), totalDays);

        LeaveRequest leaveRequest = new LeaveRequest(
                employee,
                leaveType,
                request.startDate(),
                request.endDate(),
                totalDays,
                request.halfDaySession(),
                request.reason().trim());

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

    private void validateBalance(Employee employee, LeaveType leaveType,
                                 int year, BigDecimal requestedDays) {
        LeaveBalance balance = leaveBalanceRepository
                .findByEmployeeAndLeaveTypeAndCalendarYear(employee, leaveType, year)
                .orElseThrow(() -> new BusinessRuleException(
                        "No " + leaveType.getLeaveTypeName() + " leave balance found for " + year));

        if (requestedDays.compareTo(balance.getRemainingDays()) > 0) {
            throw new BusinessRuleException(
                    "Requested days exceed your available balance of "
                            + balance.getRemainingDays() + " days");
        }
    }
}

