package com.wecureit.controller.patient;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wecureit.dto.request.AppointmentRequest;
import com.wecureit.dto.response.AppointmentResponse;
import com.wecureit.entity.Appointment;
import com.wecureit.service.AppointmentService;
import com.wecureit.repository.RoomRepository;
import com.wecureit.entity.Room;

@RestController
@RequestMapping("/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final RoomRepository roomRepository;

    public AppointmentController(AppointmentService appointmentService, RoomRepository roomRepository) {
        this.appointmentService = appointmentService;
        this.roomRepository = roomRepository;
    }

    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody AppointmentRequest req) {
        try {
            Appointment saved = appointmentService.createAppointment(req);
            URI location = URI.create("/appointments/" + saved.getId());
            return ResponseEntity.created(location).body(toResponse(saved));
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.status(409).body(java.util.Map.of("error", iae.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<AppointmentResponse> getById(@PathVariable Long id) {
        Appointment appt = appointmentService.findById(id);
        return ResponseEntity.ok(toResponse(appt));
    }

    @GetMapping("/byPatient")
    public ResponseEntity<List<AppointmentResponse>> byPatient(@RequestParam(name = "patientId") UUID patientId) {
        var list = appointmentService.findByPatientId(patientId);
        var out = new ArrayList<AppointmentResponse>();
        for (Appointment a : list) out.add(toResponse(a));
        return ResponseEntity.ok(out);
    }

    private AppointmentResponse toResponse(Appointment a) {
        var resp = new AppointmentResponse();
        resp.setId(a.getId());
        resp.setDate(a.getDate());
        resp.setDuration(a.getDuration());
        resp.setStartTime(a.getStartTime());
        resp.setEndTime(a.getEndTime());
        resp.setIsActive(a.getIsActive());
        resp.setChiefComplaints(a.getChiefComplaints());
        if (a.getPatient() != null) resp.setPatientId(a.getPatient().getId());
        if (a.getDoctorAvailability() != null) resp.setDoctorAvailabilityId(a.getDoctorAvailability().getId());
        if (a.getFacility() != null) resp.setFacilityId(a.getFacility().getId());
        if (a.getSpeciality() != null) resp.setSpecialityId(a.getSpeciality().getSpecialityCode());
        if (a.getRoomSchedule() != null) {
            resp.setRoomScheduleId(a.getRoomSchedule().getId());
            try {
                var roomId = a.getRoomSchedule().getRoomId();
                if (roomId != null) {
                    Room r = roomRepository.findById(roomId).orElse(null);
                    if (r != null) resp.setRoomNumber(r.getRoomNumber());
                }
            } catch (Exception e) {
                // don't fail response if room lookup has an issue; log if needed
            }
        }
        return resp;
    }
}
