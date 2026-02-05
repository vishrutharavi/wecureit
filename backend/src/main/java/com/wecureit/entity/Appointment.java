package com.wecureit.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "appointments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date")
    private LocalDate date;

    @Column(name = "duration")
    private Long duration;

    @ManyToOne
    @JoinColumn(name = "patient_id", referencedColumnName = "id")
    private Patient patient;

    @ManyToOne
    @JoinColumn(name = "doctor_availability_id", referencedColumnName = "id")
    private DoctorAvailability doctorAvailability;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @ManyToOne
    @JoinColumn(name = "facility_id", referencedColumnName = "id")
    private Facility facility;

    @ManyToOne
    @JoinColumn(name = "speciality_id", referencedColumnName = "speciality_code")
    private Speciality speciality;

    @ManyToOne
    @JoinColumn(name = "room_schedule_id", referencedColumnName = "id")
    private RoomSchedule roomSchedule;

    @Column(name = "is_active")
    private Boolean isActive = Boolean.TRUE;

    @Column(name = "chief_complaints", columnDefinition = "TEXT")
    private String chiefComplaints;

}
