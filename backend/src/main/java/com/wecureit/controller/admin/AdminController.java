package com.wecureit.controller.admin;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    
    @GetMapping("/me")
    public String me(Authentication authentication) {
        return "Hello admin with UID: " + authentication.getName();
    }
    
}
