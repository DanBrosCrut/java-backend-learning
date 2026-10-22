package com.example.moysklad.api.controller;

import com.example.moysklad.api.dto.AvailabilityRequest;
import com.example.moysklad.api.dto.AvailabilityResponse;
import com.example.moysklad.api.service.AvailabilityService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing the product availability check.
 */
@RestController
@RequestMapping("/api/availability")
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    /**
     * Creates the controller with the availability service.
     *
     * @param availabilityService the availability service
     */
    public AvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    /**
     * Checks whether the requested positions are available in stock.
     *
     * @param request the availability check request
     * @return the availability result
     */
    @PostMapping
    public AvailabilityResponse checkAvailability(@RequestBody AvailabilityRequest request) {
        return availabilityService.checkAvailability(request);
    }
}
