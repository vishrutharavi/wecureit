package com.wecureit.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateRoomRequest {

    @NotNull
    private UUID facilityId;

    // roomName is optional; if omitted the service will default it to roomNumber
    // roomName removed — DB no longer stores a separate room_name column

    @NotBlank
    private String roomNumber;

    @NotBlank
    private String specialityCode;

    public UUID getFacilityId() {
        return facilityId;
    }

    public void setFacilityId(UUID facilityId) {
        this.facilityId = facilityId;
    }

    // roomName getters/setters removed

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public String getSpecialityCode() {
        return specialityCode;
    }

    public void setSpecialityCode(String specialityCode) {
        this.specialityCode = specialityCode;
    }
}
