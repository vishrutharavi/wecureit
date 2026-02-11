package com.wecureit.dto.response;

import java.util.List;
import java.util.UUID;

public class BookingAvailabilityResponse {
    private UUID doctorId;
    private UUID facilityId;
    private String workDate; // YYYY-MM-DD
    private List<BookingAvailabilitySlot> slots;
    private OptimizationMetrics optimizationMetrics;

    public BookingAvailabilityResponse() {}

    public UUID getDoctorId() { return doctorId; }
    public void setDoctorId(UUID doctorId) { this.doctorId = doctorId; }

    public UUID getFacilityId() { return facilityId; }
    public void setFacilityId(UUID facilityId) { this.facilityId = facilityId; }

    public String getWorkDate() { return workDate; }
    public void setWorkDate(String workDate) { this.workDate = workDate; }

    public List<BookingAvailabilitySlot> getSlots() { return slots; }
    public void setSlots(List<BookingAvailabilitySlot> slots) { this.slots = slots; }

    public OptimizationMetrics getOptimizationMetrics() { return optimizationMetrics; }
    public void setOptimizationMetrics(OptimizationMetrics optimizationMetrics) { this.optimizationMetrics = optimizationMetrics; }

    /**
     * Optimization metrics from Branch and Bound algorithm
     */
    public static class OptimizationMetrics {
        public int appointmentsScheduled;
        public int appointmentsPending;
        public int nodesExplored;
        public int nodesPruned;

        public OptimizationMetrics(int appointmentsScheduled, int appointmentsPending, int nodesExplored, int nodesPruned) {
            this.appointmentsScheduled = appointmentsScheduled;
            this.appointmentsPending = appointmentsPending;
            this.nodesExplored = nodesExplored;
            this.nodesPruned = nodesPruned;
        }

        public int getAppointmentsScheduled() { return appointmentsScheduled; }
        public int getAppointmentsPending() { return appointmentsPending; }
        public int getNodesExplored() { return nodesExplored; }
        public int getNodesPruned() { return nodesPruned; }
    }
}
