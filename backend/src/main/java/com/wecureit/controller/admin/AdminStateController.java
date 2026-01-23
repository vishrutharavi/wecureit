package com.wecureit.controller.admin;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wecureit.dto.response.StateResponse;
import com.wecureit.service.StateService;

@RestController
@RequestMapping("/api/admin/states")
public class AdminStateController {

    private final StateService stateService;

    public AdminStateController(StateService stateService) {
        this.stateService = stateService;
    }

    @GetMapping
    public List<StateResponse> getStates() {
        return stateService.getAllStates();
    }
}
