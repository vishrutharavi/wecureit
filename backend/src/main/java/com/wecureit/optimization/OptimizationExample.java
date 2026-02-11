package com.wecureit.optimization;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Example usage of Branch and Bound optimizers
 */
public class OptimizationExample {

    /**
     * Example: Optimize appointment scheduling
     */
    public static void exampleAppointmentScheduling() {
        AppointmentScheduleOptimizer optimizer = new AppointmentScheduleOptimizer();

        // Create sample appointment requests
        List<AppointmentScheduleOptimizer.AppointmentRequest> requests = new ArrayList<>();
        UUID doctorId = UUID.randomUUID();
        UUID patientId1 = UUID.randomUUID();
        UUID patientId2 = UUID.randomUUID();

        requests.add(new AppointmentScheduleOptimizer.AppointmentRequest(
                patientId1, doctorId, 30, LocalDateTime.now().plusHours(1)
        ));
        requests.add(new AppointmentScheduleOptimizer.AppointmentRequest(
                patientId2, doctorId, 45, LocalDateTime.now().plusHours(2)
        ));

        // Create doctor availabilities
        List<AppointmentScheduleOptimizer.DoctorAvailabilitySlot> availabilities = new ArrayList<>();
        availabilities.add(new AppointmentScheduleOptimizer.DoctorAvailabilitySlot(
                doctorId, LocalTime.of(9, 0), LocalTime.of(17, 0)
        ));

        // Solve
        AppointmentScheduleOptimizer.ScheduleState solution = optimizer.solve(requests, availabilities);

        System.out.println("Appointment Scheduling Results:");
        System.out.println("Best value: " + optimizer.getBestValue());
        System.out.println("Nodes explored: " + optimizer.getNodesExplored());
        System.out.println("Nodes pruned: " + optimizer.getNodesPruned());
        System.out.println("Scheduled appointments: " + optimizer.getScheduledAppointments(solution).size());
    }

    /**
     * Example: Optimize room allocation
     */
    public static void exampleRoomAllocation() {
        RoomAllocationOptimizer optimizer = new RoomAllocationOptimizer();

        // Create sample appointments
        List<RoomAllocationOptimizer.Appointment> appointments = new ArrayList<>();
        appointments.add(new RoomAllocationOptimizer.Appointment(
                UUID.randomUUID(), "consultation", 30, 5
        ));
        appointments.add(new RoomAllocationOptimizer.Appointment(
                UUID.randomUUID(), "examination", 45, 4
        ));
        appointments.add(new RoomAllocationOptimizer.Appointment(
                UUID.randomUUID(), "consultation", 20, 3
        ));

        // Create available rooms
        List<RoomAllocationOptimizer.Room> rooms = new ArrayList<>();
        rooms.add(new RoomAllocationOptimizer.Room(
                UUID.randomUUID(), "Room 101", "consultation"
        ));
        rooms.add(new RoomAllocationOptimizer.Room(
                UUID.randomUUID(), "Room 102", "examination"
        ));
        rooms.add(new RoomAllocationOptimizer.Room(
                UUID.randomUUID(), "Room 103", "consultation"
        ));

        // Solve
        RoomAllocationOptimizer.AllocationState solution = optimizer.solve(appointments, rooms);

        System.out.println("\nRoom Allocation Results:");
        System.out.println("Best value: " + optimizer.getBestValue());
        System.out.println("Nodes explored: " + optimizer.getNodesExplored());
        System.out.println("Nodes pruned: " + optimizer.getNodesPruned());
        System.out.println("Allocated appointments: " + optimizer.getAllocatedAppointments(solution).size());
    }

    public static void main(String[] args) {
        exampleAppointmentScheduling();
        exampleRoomAllocation();
    }
}
