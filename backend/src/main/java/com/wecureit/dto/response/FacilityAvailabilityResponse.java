package com.wecureit.dto.response;

import java.util.UUID;

public class FacilityAvailabilityResponse {

    public UUID facilityId;
    public int roomsTotal;
    public int occupiedRooms;
    public int availableRooms;

    public FacilityAvailabilityResponse() {}

    public FacilityAvailabilityResponse(UUID facilityId, int roomsTotal, int occupiedRooms) {
        this.facilityId = facilityId;
        this.roomsTotal = roomsTotal;
        this.occupiedRooms = occupiedRooms;
        this.availableRooms = Math.max(0, roomsTotal - occupiedRooms);
    }
}
