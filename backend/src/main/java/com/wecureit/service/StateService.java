package com.wecureit.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.wecureit.dto.response.StateResponse;
import com.wecureit.repository.StateRepository;

@Service
public class StateService {

    private final StateRepository stateRepository;

    public StateService(StateRepository stateRepository) {
        this.stateRepository = stateRepository;
    }

    public List<StateResponse> getAllStates() {
        return stateRepository.findAll()
                .stream()
                .map(s -> new StateResponse(
                        s.getStateCode(),
                        s.getStateName()
                ))
                .toList();
    }
}
