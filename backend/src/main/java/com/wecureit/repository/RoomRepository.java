package com.wecureit.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wecureit.entity.Room;

public interface RoomRepository extends JpaRepository<Room, UUID> {

    List<Room> findByFacilityIdAndActiveTrue(UUID facilityId);

    List<Room> findByFacilityIdAndSpecialityCodeAndActiveTrue(
        UUID facilityId,
        String specialityCode
    );
}
