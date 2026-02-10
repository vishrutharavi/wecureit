package com.wecureit.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "appointment_history")
public class AppointmentHistory {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "appointment_id", columnDefinition = "uuid")
    private java.util.UUID appointmentId;

    @Column(name = "doctor_availability_id", columnDefinition = "uuid")
    private UUID doctorAvailabilityId;

    @Column(name = "patient_id", columnDefinition = "uuid")
    private UUID patientId;

    @Column(name = "cancelled_by")
    private String cancelledBy;

    @Column(name = "status")
    private String status;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public java.util.UUID getAppointmentId() { return appointmentId; }
    public void setAppointmentId(java.util.UUID appointmentId) { this.appointmentId = appointmentId; }

    public UUID getDoctorAvailabilityId() { return doctorAvailabilityId; }
    public void setDoctorAvailabilityId(UUID doctorAvailabilityId) { this.doctorAvailabilityId = doctorAvailabilityId; }

    public UUID getPatientId() { return patientId; }
    public void setPatientId(UUID patientId) { this.patientId = patientId; }

    public String getCancelledBy() { return cancelledBy; }
    public void setCancelledBy(String cancelledBy) { this.cancelledBy = cancelledBy; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
