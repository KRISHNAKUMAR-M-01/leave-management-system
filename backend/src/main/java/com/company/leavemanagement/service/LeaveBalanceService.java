package com.company.leavemanagement.service;

import com.company.leavemanagement.dto.response.LeaveBalanceResponse;
import com.company.leavemanagement.entity.Employee;
import com.company.leavemanagement.entity.LeaveBalance;
import com.company.leavemanagement.entity.LeaveType;
import com.company.leavemanagement.exception.BusinessRuleException;
import com.company.leavemanagement.exception.ResourceNotFoundException;
import com.company.leavemanagement.repository.EmployeeRepository;
import com.company.leavemanagement.repository.LeaveBalanceRepository;
import com.company.leavemanagement.repository.LeaveTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class LeaveBalanceService {

    private static final String ROLE_ADMIN = "ADMIN";

    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final EmployeeRepository employeeRepository;

    public LeaveBalanceService(LeaveBalanceRepository leaveBalanceRepository, LeaveTypeRepository leaveTypeRepository, EmployeeRepository employeeRepository) {
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.leaveTypeRepository = leaveTypeRepository;
        this.employeeRepository = employeeRepository;
    }

    @Transactional
    public void createYearlyBalances(Employee employee, int year) {
        List<LeaveBalance> balances = leaveTypeRepository.findAll()
                .stream()
                .map(leaveType -> new LeaveBalance(
                        year, leaveType.getDaysPerYear(), employee, leaveType))
                .toList();
        leaveBalanceRepository.saveAll(balances);
    }

    @Transactional
    public void deleteBalancesOf(Employee employee) {

        leaveBalanceRepository.deleteByEmployee(employee);
    }

    @Transactional(readOnly = true)
    public List<LeaveBalanceResponse> getMyBalances(String email) {
        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Employe not found"));

        return leaveBalanceRepository
                .findByEmployeeAndCalendarYear(employee, LocalDate.now().getYear())
                .stream()
                .map(LeaveBalanceResponse::from)
                .sorted(Comparator.comparing(LeaveBalanceResponse::leaveTypeName))
                .toList();
    }

    @Transactional
    public int createBalancesForYear(int year) {
        int currentYear = LocalDate.now().getYear();
        if(year < currentYear || year > currentYear + 1) {
            throw new BusinessRuleException("Balances can be generated only for " + currentYear + " or " + (currentYear + 1));
        }

        List<LeaveType> leaveTypes = leaveTypeRepository.findAll();
        List<LeaveBalance> newBalances = new ArrayList<>();

        for (Employee employee : employeeRepository.findActiveExcludingRole(ROLE_ADMIN)) {
            for (LeaveType leaveType : leaveTypes) {
                boolean exists = leaveBalanceRepository.existsByEmployeeAndLeaveTypeAndCalendarYear(employee, leaveType, year);
                if (!exists) {
                    newBalances.add(new LeaveBalance(year, leaveType.getDaysPerYear(), employee, leaveType));
                }
            }
        }
        leaveBalanceRepository.saveAll(newBalances);
        return newBalances.size();
    }
}
