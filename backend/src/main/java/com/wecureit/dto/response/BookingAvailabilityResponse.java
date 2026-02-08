package com.wecureit.dto.response;

import java.util.List;
import java.util.UUID;

public class BookingAvailabilityResponse {
    private UUID doctorId;
    private UUID facilityId;
    private String workDate; // YYYY-MM-DD
    private List<BookingAvailabilitySlot> slots;

    public BookingAvailabilityResponse() {}

    public UUID getDoctorId() { return doctorId; }
    public void setDoctorId(UUID doctorId) { this.doctorId = doctorId; }

    public UUID getFacilityId() { return facilityId; }
    public void setFacilityId(UUID facilityId) { this.facilityId = facilityId; }

    public String getWorkDate() { return workDate; }
    public void setWorkDate(String workDate) { this.workDate = workDate; }

    public List<BookingAvailabilitySlot> getSlots() { return slots; }
    public void setSlots(List<BookingAvailabilitySlot> slots) { this.slots = slots; }
}
