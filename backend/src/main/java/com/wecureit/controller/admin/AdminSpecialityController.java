package com.wecureit.controller.admin;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wecureit.dto.response.SpecialityResponse;
import com.wecureit.service.SpecialityService;

@RestController
@RequestMapping("/api/admin/specialities")
public class AdminSpecialityController {

    private final SpecialityService specialityService;

    public AdminSpecialityController(SpecialityService specialityService) {
        this.specialityService = specialityService;
    }

    @GetMapping
    public List<SpecialityResponse> getSpecialities() {
        return specialityService.getAllSpecialities();
    }
}

