package com.wecureit.dto.response;

import java.util.UUID;

public class RoomResponse {

    public UUID id;
    public String roomNumber;
    public String specialityCode;
    // human-friendly name for frontend; use 'specialty' to match frontend property
    public String specialty;
    // `name` is the display label for the room (previously derived from roomName)
    public String name;
    public Boolean active;

    public RoomResponse(UUID id, String roomNumber, String specialityCode, String specialty, Boolean active) {
        this.id = id;
        this.roomNumber = roomNumber;
        this.specialityCode = specialityCode;
        this.specialty = specialty;
        // prior behavior used roomName as display name; now use roomNumber as fallback display name
        this.name = roomNumber;
        this.active = active;
    }
}
