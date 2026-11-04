package com.company.leavemanagement.service;

import com.company.leavemanagement.entity.Employee;
import com.company.leavemanagement.entity.LeaveBalance;
import com.company.leavemanagement.repository.LeaveBalanceRepository;
import com.company.leavemanagement.repository.LeaveTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LeaveBalanceService {

    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveTypeRepository leaveTypeRepository;

    public LeaveBalanceService(LeaveBalanceRepository leaveBalanceRepository, LeaveTypeRepository leaveTypeRepository) {
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.leaveTypeRepository = leaveTypeRepository;
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
}
