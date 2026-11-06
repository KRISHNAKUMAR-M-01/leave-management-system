package com.company.leavemanagement.service;

import com.company.leavemanagement.dto.response.LeaveTypeResponse;
import com.company.leavemanagement.repository.LeaveTypeRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LeaveTypeService {
    private final LeaveTypeRepository leaveTypeRepository;

    public LeaveTypeService(LeaveTypeRepository leaveTypeRepository) {
        this.leaveTypeRepository = leaveTypeRepository;
    }

    @Transactional
    public List<LeaveTypeResponse> getAllLeaveTypes() {
        return leaveTypeRepository.findAll(Sort.by("leaveTypeName"))
                .stream()
                .map(LeaveTypeResponse::from)
                .toList();
    }
}
