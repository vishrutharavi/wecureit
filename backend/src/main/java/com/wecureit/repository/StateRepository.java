package com.wecureit.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wecureit.entity.State;

public interface StateRepository extends JpaRepository<State, String> {
    Optional<State> findByStateNameIgnoreCase(String stateName);
    boolean existsByStateNameIgnoreCase(String stateName);

}
