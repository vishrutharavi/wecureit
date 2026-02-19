package com.wecureit.dto.response;

import java.util.*;

public class AvailabilityResponse {
    private UUID id;
    private String workDate;
    private String startTime;
    private String endTime;
    private String facilityId;
    private String facilityName;
    private String facilityAddress;
    private String facilityState;
    private String specialityCode;
    private boolean bookable;
    
    // availability counts provided so frontend can show accurate room numbers
    private Integer roomsTotal;
    private Integer occupiedRooms;
    private Integer availableRooms;
    // list of occupied appointment ranges (ISO local time strings) within this availability
    private List<String> occupiedAppointments;

    // getters/setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getFacilityId() { return facilityId; }
    public void setFacilityId(String facilityId) { this.facilityId = facilityId; }

    public String getFacilityName() { return facilityName; }
    public void setFacilityName(String facilityName) { this.facilityName = facilityName; }

    public String getFacilityAddress() { return facilityAddress; }
    public void setFacilityAddress(String facilityAddress) { this.facilityAddress = facilityAddress; }

    public String getFacilityState() { return facilityState; }
    public void setFacilityState(String facilityState) { this.facilityState = facilityState; }

    public String getWorkDate() { return workDate; }
    public void setWorkDate(String workDate) { this.workDate = workDate; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public String getSpecialityCode() { return specialityCode; }
    public void setSpecialityCode(String specialityCode) { this.specialityCode = specialityCode; }

    public boolean isBookable() { return bookable; }
    public void setBookable(boolean bookable) { this.bookable = bookable; }

    public Integer getRoomsTotal() { return roomsTotal; }
    public void setRoomsTotal(Integer roomsTotal) { this.roomsTotal = roomsTotal; }

    public Integer getOccupiedRooms() { return occupiedRooms; }
    public void setOccupiedRooms(Integer occupiedRooms) { this.occupiedRooms = occupiedRooms; }

    public Integer getAvailableRooms() { return availableRooms; }
    public void setAvailableRooms(Integer availableRooms) { this.availableRooms = availableRooms; }

    public List<String> getOccupiedAppointments() { return occupiedAppointments; }
    public void setOccupiedAppointments(java.util.List<String> occupiedAppointments) { this.occupiedAppointments = occupiedAppointments; }
}
