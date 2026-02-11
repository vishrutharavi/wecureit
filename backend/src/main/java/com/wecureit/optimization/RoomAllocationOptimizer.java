package com.wecureit.optimization;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Room allocation optimizer using Branch and Bound.
 * 
 * Problem: Given a set of appointments and available rooms in a facility,
 * find an optimal room assignment that minimizes room conflicts and maximizes
 * room utilization.
 */
public class RoomAllocationOptimizer extends BranchAndBound<RoomAllocationOptimizer.AllocationState> {

    public static class Room {
        public UUID id;
        public String name;
        public String type; // "consultation", "examination", "procedure"
        
        public Room(UUID id, String name, String type) {
            this.id = id;
            this.name = name;
            this.type = type;
        }
    }

    public static class Appointment {
        public UUID id;
        public String requiredRoomType;
        public int durationMinutes;
        public int priority; // Higher = more important
        
        public Appointment(UUID id, String requiredRoomType, int durationMinutes, int priority) {
            this.id = id;
            this.requiredRoomType = requiredRoomType;
            this.durationMinutes = durationMinutes;
            this.priority = priority;
        }
    }

    public static class AllocationState {
        public List<Appointment> appointments;
        public List<Room> rooms;
        public List<UUID> allocation; // room IDs for each appointment, null if unallocated
        public int nextAppointmentIndex;
        
        public AllocationState(List<Appointment> appointments, List<Room> rooms) {
            this.appointments = appointments;
            this.rooms = rooms;
            this.allocation = new ArrayList<>();
            this.nextAppointmentIndex = 0;
            for (int i = 0; i < appointments.size(); i++) {
                allocation.add(null);
            }
        }
        
        public AllocationState copy() {
            AllocationState copy = new AllocationState(appointments, rooms);
            copy.allocation = new ArrayList<>(this.allocation);
            copy.nextAppointmentIndex = this.nextAppointmentIndex;
            return copy;
        }
    }

    public RoomAllocationOptimizer() {
        this.maximizing = true; // Maximize number of allocated appointments
    }

    @Override
    protected AllocationState getProblemState() {
        return new AllocationState(new ArrayList<>(), new ArrayList<>());
    }

    public AllocationState solve(List<Appointment> appointments, List<Room> rooms) {
        this.bestKnownValue = Double.NEGATIVE_INFINITY;
        this.bestSolution = null;
        this.nodesExplored = 0;
        this.nodesPruned = 0;

        return super.solve();
    }

    @Override
    protected boolean isTerminal(AllocationState state) {
        return state.nextAppointmentIndex >= state.appointments.size();
    }

    @Override
    protected List<AllocationState> getChildren(AllocationState state) {
        List<AllocationState> children = new ArrayList<>();

        if (isTerminal(state)) {
            return children;
        }

        Appointment currentAppt = state.appointments.get(state.nextAppointmentIndex);

        // Try allocating to each suitable room
        for (Room room : state.rooms) {
            if (isRoomSuitable(room, currentAppt) && !isRoomAllocated(state, room)) {
                AllocationState child = state.copy();
                child.allocation.set(state.nextAppointmentIndex, room.id);
                child.nextAppointmentIndex++;
                children.add(child);
            }
        }

        // Option: Don't allocate this appointment
        AllocationState skipChild = state.copy();
        skipChild.allocation.set(state.nextAppointmentIndex, null);
        skipChild.nextAppointmentIndex++;
        children.add(skipChild);

        return children;
    }

    @Override
    protected double evaluate(AllocationState state) {
        // Upper bound: allocated appointments + remaining appointments
        int allocated = 0;
        for (UUID roomId : state.allocation) {
            if (roomId != null) allocated++;
        }
        return allocated + (state.appointments.size() - state.nextAppointmentIndex);
    }

    @Override
    protected double cost(AllocationState state) {
        // Cost = number of allocated appointments adjusted by priority
        double totalScore = 0;
        for (int i = 0; i < state.allocation.size(); i++) {
            if (state.allocation.get(i) != null) {
                totalScore += state.appointments.get(i).priority;
            }
        }
        return totalScore;
    }

    private boolean isRoomSuitable(Room room, Appointment appt) {
        return room.type.equalsIgnoreCase(appt.requiredRoomType);
    }

    private boolean isRoomAllocated(AllocationState state, Room room) {
        for (UUID allocatedRoomId : state.allocation) {
            if (allocatedRoomId != null && allocatedRoomId.equals(room.id)) {
                return true;
            }
        }
        return false;
    }

    public List<Appointment> getAllocatedAppointments(AllocationState solution) {
        List<Appointment> allocated = new ArrayList<>();
        if (solution == null) return allocated;

        for (int i = 0; i < solution.allocation.size(); i++) {
            if (solution.allocation.get(i) != null) {
                allocated.add(solution.appointments.get(i));
            }
        }
        return allocated;
    }
}
