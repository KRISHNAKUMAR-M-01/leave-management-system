package com.company.leavemanagement.controller;

import com.company.leavemanagement.dto.request.PublicHolidayRequest;
import com.company.leavemanagement.dto.response.PublicHolidayResponse;
import com.company.leavemanagement.service.PublicHolidayService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public-holidays")
public class PublicHolidayController {

    private final PublicHolidayService publicHolidayService;

    public PublicHolidayController(PublicHolidayService publicHolidayService) {
        this.publicHolidayService = publicHolidayService;
    }

    @GetMapping
    public List<PublicHolidayResponse> getHolidays(@RequestParam(required = false) Integer year) {
        return publicHolidayService.getHolidaysOfYear(year);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PublicHolidayResponse addHoliday(@Valid @RequestBody PublicHolidayRequest request) {
        return publicHolidayService.addHoliday(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteHoliday(@PathVariable Long id) {
        publicHolidayService.deleteHoliday(id);
    }
}
