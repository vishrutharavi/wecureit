package com.wecureit.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wecureit.entity.AppointmentHistory;

public interface AppointmentHistoryRepository extends JpaRepository<AppointmentHistory, UUID> {

}
