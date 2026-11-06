package com.company.leavemanagement.service;

import com.company.leavemanagement.dto.request.PublicHolidayRequest;
import com.company.leavemanagement.dto.response.PublicHolidayResponse;
import com.company.leavemanagement.entity.PublicHoliday;
import com.company.leavemanagement.exception.BusinessRuleException;
import com.company.leavemanagement.exception.ResourceNotFoundException;
import com.company.leavemanagement.repository.PublicHolidayRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
public class PublicHolidayService {

    private static final int MIN_YEAR = 2000;
    private static final int MAX_YEAR = 2100;

    private final PublicHolidayRepository publicHolidayRepository;

    public PublicHolidayService(PublicHolidayRepository publicHolidayRepository)  {
        this.publicHolidayRepository = publicHolidayRepository;
    }

    @Transactional(readOnly = true)
    public List<PublicHolidayResponse> getHolidaysOfYear(Integer year) {
        int selectedYear = (year != null) ? year : LocalDate.now().getYear();
        validateYear(selectedYear);

        LocalDate firstDay = LocalDate.of(selectedYear, 1, 1);
        LocalDate lastDay = LocalDate.of(selectedYear, 12,31);

        return publicHolidayRepository.findByHolidayDateBetween(firstDay, lastDay)
                .stream()
                .sorted(Comparator.comparing(PublicHoliday::getHolidayDate))
                .map(PublicHolidayResponse::from)
                .toList();
    }

    @Transactional
    public PublicHolidayResponse addHoliday(PublicHolidayRequest request) {
        if(publicHolidayRepository.existsByHolidayDate(request.holidayDate())) {
            throw new BusinessRuleException("A holiday already exists on this date");
        }
        PublicHoliday holiday = new PublicHoliday(request.holidayDate(), request.holidayName().trim());
        return PublicHolidayResponse.from(publicHolidayRepository.save(holiday));
    }

    @Transactional
    public void deleteHoliday(Long id) {
        PublicHoliday holiday = publicHolidayRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Holiday not found with id " + id));
        publicHolidayRepository.delete(holiday);
    }

    private void validateYear(int year) {
        if (year < MIN_YEAR || year > MAX_YEAR) {
            throw new BusinessRuleException("Years must be between " + MIN_YEAR + " and " + MAX_YEAR);
        }
    }

}
