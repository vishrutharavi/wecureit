package com.wecureit.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wecureit.dto.request.CreateRoomRequest;
import com.wecureit.entity.Room;
import com.wecureit.repository.RoomRepository;

@Service
public class RoomService {

    private final RoomRepository roomRepo;

    public RoomService(RoomRepository roomRepo) {
        this.roomRepo = roomRepo;
    }

    @Transactional
    public Room createRoom(CreateRoomRequest req) {

        Room r = new Room();
        r.setFacilityId(req.getFacilityId());
        // DB no longer stores separate room_name; rely on roomNumber as the canonical label
        r.setRoomNumber(req.getRoomNumber());
        r.setSpecialityCode(req.getSpecialityCode());

        return roomRepo.save(r);
    }

    @Transactional
    public Room updateRoom(UUID roomId, com.wecureit.dto.request.UpdateRoomRequest request) {
        Room room = roomRepo.findById(roomId).orElseThrow(() -> new RuntimeException("Room not found"));
        if (request.roomNumber != null) {
            room.setRoomNumber(request.roomNumber);
        }
        if (request.specialityCode != null) {
            room.setSpecialityCode(request.specialityCode);
        }
        return roomRepo.save(room);
    }

    @Transactional
    public void deactivateRoom(UUID roomId) {
        Room r = roomRepo.findById(roomId)
            .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        r.setActive(false);
        roomRepo.save(r);
    }
}

