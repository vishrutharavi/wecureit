package com.wecureit.controller.admin;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wecureit.dto.request.CreateRoomRequest;
import com.wecureit.dto.request.UpdateRoomRequest;
import com.wecureit.entity.Room;
import com.wecureit.repository.RoomRepository;
import com.wecureit.service.RoomService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/rooms")
@PreAuthorize("hasRole('ADMIN')")
public class AdminRoomController {

    private final RoomService service;
    private final RoomRepository repo;

    public AdminRoomController(RoomService service, RoomRepository repo) {
        this.service = service;
        this.repo = repo;
    }

    /** Add room */
    @PostMapping
    public Room createRoom(@Valid @RequestBody CreateRoomRequest req) {
        return service.createRoom(req);
    }

    /** Rooms by facility */
    @GetMapping("/facility/{facilityId}")
    public List<Room> getRoomsByFacility(@PathVariable UUID facilityId) {
        return repo.findByFacilityIdAndActiveTrue(facilityId);
    }

    /** Disable room */
    @PatchMapping("/{roomId}/deactivate")
    public ResponseEntity<?> deactivate(@PathVariable UUID roomId) {
        service.deactivateRoom(roomId);
        return ResponseEntity.ok(Map.of("message", "Room deactivated"));
    }

    /** Update room */
    @PutMapping("/{roomId}")
    public Room updateRoom(@PathVariable UUID roomId, @RequestBody UpdateRoomRequest req) {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        System.out.println("AdminRoomController.updateRoom - authentication=" + auth + " authorities=" + (auth != null ? auth.getAuthorities() : "null"));
    System.out.println("AdminRoomController.updateRoom - roomId=" + roomId + " payload.roomNumber=" + req.roomNumber + " speciality=" + req.specialityCode);
        return service.updateRoom(roomId, req);
    }
}
