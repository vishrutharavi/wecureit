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
        // default roomName to roomNumber when not provided to satisfy DB not-null constraint
        String roomName = req.getRoomName();
        if (roomName == null || roomName.isBlank()) {
            roomName = req.getRoomNumber();
        }
        r.setRoomName(roomName);
        r.setRoomNumber(req.getRoomNumber());
        r.setSpecialityCode(req.getSpecialityCode());

        return roomRepo.save(r);
    }

    @Transactional
    public void deactivateRoom(UUID roomId) {
        Room r = roomRepo.findById(roomId)
            .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        r.setActive(false);
        roomRepo.save(r);
    }
}

