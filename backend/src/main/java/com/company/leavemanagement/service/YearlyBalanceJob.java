package com.company.leavemanagement.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class YearlyBalanceJob {

    private static final Logger log = LoggerFactory.getLogger(YearlyBalanceJob.class);

    private final LeaveBalanceService leaveBalanceService;

    public YearlyBalanceJob(LeaveBalanceService leaveBalanceService) {
        this.leaveBalanceService = leaveBalanceService;
    }

    @Scheduled(cron = "0 0 0 1 1 *")
    public void createBalancesForNewYear() {
        int year = LocalDate.now().getYear();
        int created = leaveBalanceService.createBalancesForYear(year);
        log.info("Yearly balance reset for {}: {} balance rows created", year, created);
    }
}