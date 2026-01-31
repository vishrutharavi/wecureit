package com.wecureit.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "doctor_availability")
public class DoctorAvailability {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "doctor_id", nullable = false)
    private UUID doctorId;

    @Column(name = "facility_id", nullable = false)
    private UUID facilityId;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "speciality_code", length = 4)
    private String specialityCode;

    @Column(name = "room_assigned_id", columnDefinition = "uuid")
    private UUID roomAssignedId;

    @Column(name = "room_assignment_status", length = 20)
    private String roomAssignmentStatus = "PENDING"; // PENDING, ASSIGNED, NONE

    @Column(name = "allow_walk_in")
    private Boolean allowWalkIn = false;

    @Column(name = "is_bookable")
    private Boolean isBookable = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public UUID getId() { return id; }

    public UUID getDoctorId() { return doctorId; }
    public void setDoctorId(UUID doctorId) { this.doctorId = doctorId; }

    public UUID getFacilityId() { return facilityId; }
    public void setFacilityId(UUID facilityId) { this.facilityId = facilityId; }

    public LocalDate getWorkDate() { return workDate; }
    public void setWorkDate(LocalDate workDate) { this.workDate = workDate; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public String getSpecialityCode() { return specialityCode; }
    public void setSpecialityCode(String specialityCode) { this.specialityCode = specialityCode; }

    public UUID getRoomAssignedId() { return roomAssignedId; }
    public void setRoomAssignedId(UUID roomAssignedId) { this.roomAssignedId = roomAssignedId; }

    public String getRoomAssignmentStatus() { return roomAssignmentStatus; }
    public void setRoomAssignmentStatus(String roomAssignmentStatus) { this.roomAssignmentStatus = roomAssignmentStatus; }

    public Boolean getAllowWalkIn() { return allowWalkIn; }
    public void setAllowWalkIn(Boolean allowWalkIn) { this.allowWalkIn = allowWalkIn; }

    public Boolean getIsBookable() { return isBookable; }
    public void setIsBookable(Boolean isBookable) { this.isBookable = isBookable; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
