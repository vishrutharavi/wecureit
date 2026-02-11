package com.wecureit.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.wecureit.service.OptimizationService;

/**
 * REST Controller for Branch and Bound optimization endpoints
 */
@RestController
@RequestMapping("/api/optimization")
public class OptimizationController {

    @Autowired
    private OptimizationService optimizationService;

    /**
     * Optimize appointment scheduling for a doctor on a specific date
     * 
     * GET /api/optimization/schedule/{doctorId}/{workDate}
     * 
     * @param doctorId The doctor's ID
     * @param workDate The work date (YYYY-MM-DD format)
     * @return Scheduling optimization result
     */
    @GetMapping("/schedule/{doctorId}/{workDate}")
    public ResponseEntity<OptimizationService.SchedulingOptimizationResult> optimizeSchedule(
            @PathVariable UUID doctorId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate workDate) {
        
        OptimizationService.SchedulingOptimizationResult result = 
            optimizationService.optimizeAppointmentScheduling(doctorId, workDate);
        
        return ResponseEntity.ok(result);
    }

    /**
     * Optimize doctor workload distribution
     * 
     * POST /api/optimization/workload
     * 
     * @param request Contains appointmentIds and doctorIds
     * @return Workload optimization result
     */
    @PostMapping("/workload")
    public ResponseEntity<OptimizationService.WorkloadOptimizationResult> optimizeWorkload(
            @RequestBody WorkloadOptimizationRequest request) {
        
        OptimizationService.WorkloadOptimizationResult result = 
            optimizationService.optimizeDoctorWorkload(new java.util.ArrayList<>(), request.doctorIds);
        
        return ResponseEntity.ok(result);
    }

    /**
     * Optimize room allocation for appointments
     * 
     * POST /api/optimization/rooms
     * 
     * @param request Contains appointmentIds and optional facilityId
     * @return Room allocation optimization result
     */
    @PostMapping("/rooms")
    public ResponseEntity<OptimizationService.RoomAllocationOptimizationResult> optimizeRoomAllocation(
            @RequestBody RoomAllocationOptimizationRequest request) {
        
        OptimizationService.RoomAllocationOptimizationResult result = 
            optimizationService.optimizeRoomAllocation(request.appointmentIds, request.facilityId);
        
        return ResponseEntity.ok(result);
    }

    /**
     * Get optimization metrics for a doctor's schedule
     * 
     * GET /api/optimization/metrics/{doctorId}/{workDate}
     * 
     * @param doctorId The doctor's ID
     * @param workDate The work date (YYYY-MM-DD format)
     * @return Optimization metrics
     */
    @GetMapping("/metrics/{doctorId}/{workDate}")
    public ResponseEntity<OptimizationService.SchedulingOptimizationResult> getOptimizationMetrics(
            @PathVariable UUID doctorId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate workDate) {
        
        OptimizationService.SchedulingOptimizationResult result = 
            optimizationService.optimizeAppointmentScheduling(doctorId, workDate);
        
        return ResponseEntity.ok(result);
    }

    // Request DTOs

    public static class WorkloadOptimizationRequest {
        public List<UUID> appointmentIds;
        public List<UUID> doctorIds;

        public WorkloadOptimizationRequest() {}

        public List<UUID> getAppointmentIds() { return appointmentIds; }
        public void setAppointmentIds(List<UUID> appointmentIds) { this.appointmentIds = appointmentIds; }

        public List<UUID> getDoctorIds() { return doctorIds; }
        public void setDoctorIds(List<UUID> doctorIds) { this.doctorIds = doctorIds; }
    }

    public static class RoomAllocationOptimizationRequest {
        public List<UUID> appointmentIds;
        public UUID facilityId;

        public RoomAllocationOptimizationRequest() {}

        public List<UUID> getAppointmentIds() { return appointmentIds; }
        public void setAppointmentIds(List<UUID> appointmentIds) { this.appointmentIds = appointmentIds; }

        public UUID getFacilityId() { return facilityId; }
        public void setFacilityId(UUID facilityId) { this.facilityId = facilityId; }
    }
}
