package com.wecureit.controller.admin;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wecureit.dto.request.CreateFacilityRequest;
import com.wecureit.dto.response.FacilityResponse;
import com.wecureit.service.AdminFacilityService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/facilities")
public class AdminFacilityController {


    private final AdminFacilityService facilityService;

    public AdminFacilityController(AdminFacilityService facilityService) {
        this.facilityService = facilityService;
    }

    @PostMapping
    public FacilityResponse createFacility(
            @Valid @RequestBody CreateFacilityRequest request
    ) {
        return facilityService.createFacility(request);
    }

    @GetMapping
    public List<FacilityResponse> getFacilities() {
        return facilityService.getAllFacilities();
    }
}

