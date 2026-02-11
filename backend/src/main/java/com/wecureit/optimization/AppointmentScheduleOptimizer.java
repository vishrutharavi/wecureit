package com.wecureit.optimization;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Appointment scheduling optimizer using Branch and Bound.
 * 
 * Problem: Given a set of appointment requests and doctor availabilities,
 * find an optimal schedule that maximizes appointments scheduled while
 * minimizing gaps and travel time between appointments.
 */
public class AppointmentScheduleOptimizer extends BranchAndBound<AppointmentScheduleOptimizer.ScheduleState> {

    public static class AppointmentRequest {
        public UUID patientId;
        public UUID doctorId;
        public int durationMinutes;
        public LocalDateTime requestedDateTime;

        public AppointmentRequest(UUID patientId, UUID doctorId, int durationMinutes, LocalDateTime requestedDateTime) {
            this.patientId = patientId;
            this.doctorId = doctorId;
            this.durationMinutes = durationMinutes;
            this.requestedDateTime = requestedDateTime;
        }
    }

    public static class DoctorAvailabilitySlot {
        public UUID doctorId;
        public LocalTime startTime;
        public LocalTime endTime;

        public DoctorAvailabilitySlot(UUID doctorId, LocalTime startTime, LocalTime endTime) {
            this.doctorId = doctorId;
            this.startTime = startTime;
            this.endTime = endTime;
        }
    }

    public static class ScheduleState {
        public List<AppointmentRequest> requests;
        public List<DoctorAvailabilitySlot> availabilities;
        public List<Integer> schedule; // indices of scheduled requests, -1 means unscheduled
        public int nextRequestIndex;

        public ScheduleState(List<AppointmentRequest> requests, List<DoctorAvailabilitySlot> availabilities) {
            this.requests = requests;
            this.availabilities = availabilities;
            this.schedule = new ArrayList<>();
            this.nextRequestIndex = 0;
            for (int i = 0; i < requests.size(); i++) {
                schedule.add(-1);
            }
        }

        public ScheduleState copy() {
            ScheduleState copy = new ScheduleState(requests, availabilities);
            copy.schedule = new ArrayList<>(this.schedule);
            copy.nextRequestIndex = this.nextRequestIndex;
            return copy;
        }
    }

    public AppointmentScheduleOptimizer() {
        this.maximizing = true; // Maximize number of scheduled appointments
    }

    @Override
    protected ScheduleState getProblemState() {
        return new ScheduleState(currentRequests != null ? currentRequests : new ArrayList<>(), 
                                 currentAvailabilities != null ? currentAvailabilities : new ArrayList<>());
    }

    public ScheduleState solve(List<AppointmentRequest> requests, List<DoctorAvailabilitySlot> availabilities) {
        this.bestKnownValue = Double.NEGATIVE_INFINITY;
        this.bestSolution = null;
        this.nodesExplored = 0;
        this.nodesPruned = 0;

        // Store state for use in getProblemState()
        this.currentRequests = requests;
        this.currentAvailabilities = availabilities;
        return super.solve();
    }
    
    private List<AppointmentRequest> currentRequests;
    private List<DoctorAvailabilitySlot> currentAvailabilities;

    @Override
    protected boolean isTerminal(ScheduleState state) {
        return state.nextRequestIndex >= state.requests.size();
    }

    @Override
    protected List<ScheduleState> getChildren(ScheduleState state) {
        List<ScheduleState> children = new ArrayList<>();

        if (isTerminal(state)) {
            return children;
        }

        AppointmentRequest currentRequest = state.requests.get(state.nextRequestIndex);

        // Option 1: Try to schedule the current request
        for (DoctorAvailabilitySlot slot : state.availabilities) {
            if (slot.doctorId.equals(currentRequest.doctorId)) {
                if (canFitAppointment(slot, currentRequest)) {
                    ScheduleState child = state.copy();
                    child.schedule.set(state.nextRequestIndex, state.nextRequestIndex);
                    child.nextRequestIndex++;
                    children.add(child);
                }
            }
        }

        // Option 2: Skip scheduling the current request
        ScheduleState skipChild = state.copy();
        skipChild.schedule.set(state.nextRequestIndex, -1);
        skipChild.nextRequestIndex++;
        children.add(skipChild);

        return children;
    }

    @Override
    protected double evaluate(ScheduleState state) {
        // Upper bound: maximum possible appointments = number of requests scheduled so far + remaining requests
        int scheduled = 0;
        for (int idx : state.schedule) {
            if (idx != -1) scheduled++;
        }
        return scheduled + (state.requests.size() - state.nextRequestIndex);
    }

    @Override
    protected double cost(ScheduleState state) {
        // Cost = number of successfully scheduled appointments
        int scheduled = 0;
        for (int idx : state.schedule) {
            if (idx != -1) scheduled++;
        }
        return scheduled;
    }

    private boolean canFitAppointment(DoctorAvailabilitySlot slot, AppointmentRequest request) {
        // Check if appointment duration can fit in the available slot
        long availableMinutes = java.time.Duration
                .between(slot.startTime, slot.endTime)
                .toMinutes();
        return availableMinutes >= request.durationMinutes;
    }

    public List<AppointmentRequest> getScheduledAppointments(ScheduleState solution) {
        List<AppointmentRequest> scheduled = new ArrayList<>();
        if (solution == null) return scheduled;

        for (int i = 0; i < solution.schedule.size(); i++) {
            if (solution.schedule.get(i) != -1) {
                scheduled.add(solution.requests.get(i));
            }
        }
        return scheduled;
    }
}
