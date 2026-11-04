package com.company.leavemanagement.service;

import com.company.leavemanagement.dto.response.DepartmentResponse;
import com.company.leavemanagement.repository.DepartmentRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> getAllDepartments() {
        return departmentRepository.findAll(Sort.by("departmentName"))
                .stream()
                .map(DepartmentResponse::from)
                .toList();
    }
}
