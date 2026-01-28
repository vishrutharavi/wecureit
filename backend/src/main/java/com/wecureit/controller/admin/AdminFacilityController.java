package com.wecureit.controller.admin;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wecureit.dto.request.CreateFacilityRequest;
import com.wecureit.dto.request.UpdateFacilityRequest;
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
    
    @PutMapping("/updateFacility/{id}")
    public FacilityResponse updateFacility(
            @PathVariable("id") java.util.UUID id,
            @Valid @RequestBody UpdateFacilityRequest request
    ) {
        // debug: print authentication info and incoming payload to help diagnose persistence issues
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        System.out.println("AdminFacilityController.updateFacility - authentication=" + auth + " authorities=" + (auth != null ? auth.getAuthorities() : "null"));
        System.out.println("AdminFacilityController.updateFacility - id=" + id + " payload.name=" + request.name + " city=" + request.city + " state=" + request.stateCode + " zip=" + request.zipCode);

        FacilityResponse resp = facilityService.updateFacility(id, request);
    System.out.println("AdminFacilityController.updateFacility - updated facility id=" + resp.id + " name=" + resp.name + " city=" + resp.city + " state=" + resp.state);
        return resp;
    }

    @GetMapping
    public List<FacilityResponse> getFacilities() {
        return facilityService.getAllFacilities();
    }

    @PatchMapping("/{id}/delete")
    public void deleteFacility(@PathVariable("id") java.util.UUID id) {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        System.out.println("AdminFacilityController.deleteFacility - authentication=" + auth + " authorities=" + (auth != null ? auth.getAuthorities() : "null") + " id=" + id);
        facilityService.deactivateFacility(id);
    }
}

