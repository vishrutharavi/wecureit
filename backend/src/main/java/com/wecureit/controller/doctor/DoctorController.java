package com.wecureit.controller.doctor;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/doctor")
public class DoctorController {
    
    @GetMapping("/me")
    public String me(Authentication authentication) {
        return "Hello doctor with UID: " + authentication.getName();
    }
}
