package com.wecureit.controller.patient;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/patient")
public class PatientController {

    @GetMapping("/me")
    public String me(Authentication authentication) {
        return "Hello patient with UID: " + authentication.getName();
    }
}
