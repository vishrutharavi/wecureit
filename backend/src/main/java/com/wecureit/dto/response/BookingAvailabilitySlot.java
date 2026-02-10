package com.wecureit.dto.response;

public class BookingAvailabilitySlot {
    private String startAt; // ISO local datetime
    private String endAt;   // ISO local datetime
    private String status;  // AVAILABLE | BOOKED | BREAK_ENFORCED | UNAVAILABLE
    private String availabilityId; // optional id of the doctor_availability window (UUID as string)

    public BookingAvailabilitySlot() {}

    public BookingAvailabilitySlot(String startAt, String endAt, String status) {
        this.startAt = startAt;
        this.endAt = endAt;
        this.status = status;
    }

    public BookingAvailabilitySlot(String startAt, String endAt, String status, String availabilityId) {
        this.startAt = startAt;
        this.endAt = endAt;
        this.status = status;
        this.availabilityId = availabilityId;
    }

    public String getStartAt() { return startAt; }
    public void setStartAt(String startAt) { this.startAt = startAt; }

    public String getEndAt() { return endAt; }
    public void setEndAt(String endAt) { this.endAt = endAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAvailabilityId() { return availabilityId; }
    public void setAvailabilityId(String availabilityId) { this.availabilityId = availabilityId; }
}
