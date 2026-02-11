# Branch and Bound Optimization Framework

This package contains a generic Branch and Bound algorithm implementation with practical applications for healthcare scheduling in the WecureIT system.

## Overview

Branch and Bound is an optimization algorithm that systematically explores a search tree while pruning branches that cannot lead to better solutions than the current best. It's ideal for:

- **Combinatorial optimization** problems
- **Resource allocation** and scheduling
- **NP-hard problems** where we need good solutions in reasonable time

## Components

### 1. `BranchAndBound<S>` (Generic Framework)

The base abstract class providing the core B&B algorithm. Extend this class to solve your specific optimization problems.

**Key Methods to Implement:**
- `getProblemState()` - Returns initial problem state
- `isTerminal(S state)` - Checks if state is a complete solution
- `getChildren(S state)` - Generates child nodes (branches)
- `evaluate(S state)` - Calculates bound (upper/lower estimate)
- `cost(S state)` - Calculates actual cost of complete solution

**Complexity:** 
- Time: O(branches^depth) in worst case, but pruning significantly reduces nodes explored
- Space: O(depth) for queue

### 2. `AppointmentScheduleOptimizer`

Optimizes appointment scheduling by maximizing the number of appointments scheduled within doctor availabilities.

**Problem:** Given appointment requests and doctor time slots, find the optimal schedule.

**Usage:**
```java
AppointmentScheduleOptimizer optimizer = new AppointmentScheduleOptimizer();

List<AppointmentScheduleOptimizer.AppointmentRequest> requests = new ArrayList<>();
requests.add(new AppointmentScheduleOptimizer.AppointmentRequest(
    patientId, doctorId, 30, LocalDateTime.now().plusHours(1)
));

List<AppointmentScheduleOptimizer.DoctorAvailabilitySlot> availabilities = new ArrayList<>();
availabilities.add(new AppointmentScheduleOptimizer.DoctorAvailabilitySlot(
    doctorId, LocalTime.of(9, 0), LocalTime.of(17, 0)
));

AppointmentScheduleOptimizer.ScheduleState solution = optimizer.solve(requests, availabilities);
System.out.println("Scheduled: " + optimizer.getBestValue());
System.out.println("Nodes explored: " + optimizer.getNodesExplored());
```

**Optimization Goal:** Maximize appointments scheduled

### 3. `RoomAllocationOptimizer`

Allocates appointments to available rooms considering room type requirements and priority.

**Problem:** Given appointments with room requirements and available rooms, find optimal allocation.

**Usage:**
```java
RoomAllocationOptimizer optimizer = new RoomAllocationOptimizer();

List<RoomAllocationOptimizer.Appointment> appointments = new ArrayList<>();
appointments.add(new RoomAllocationOptimizer.Appointment(
    appointmentId, "consultation", 30, 5 // priority
));

List<RoomAllocationOptimizer.Room> rooms = new ArrayList<>();
rooms.add(new RoomAllocationOptimizer.Room(
    roomId, "Room 101", "consultation"
));

RoomAllocationOptimizer.AllocationState solution = optimizer.solve(appointments, rooms);
List<RoomAllocationOptimizer.Appointment> allocated = optimizer.getAllocatedAppointments(solution);
```

**Optimization Goal:** Maximize allocated appointments by priority

### 4. `DoctorWorkloadBalancer`

Distributes work items across doctors to minimize workload imbalance while respecting specialization.

**Problem:** Given work items and doctors with specializations, find assignment that balances workload.

**Usage:**
```java
DoctorWorkloadBalancer optimizer = new DoctorWorkloadBalancer();

List<DoctorWorkloadBalancer.WorkItem> workItems = new ArrayList<>();
workItems.add(new DoctorWorkloadBalancer.WorkItem(
    itemId, "cardiology", 100 // workUnits
));

List<DoctorWorkloadBalancer.Doctor> doctors = new ArrayList<>();
doctors.add(new DoctorWorkloadBalancer.Doctor(
    doctorId, "Dr. Smith", "cardiology"
));

DoctorWorkloadBalancer.WorkloadState solution = optimizer.solve(workItems, doctors);
Map<UUID, Integer> distribution = optimizer.getWorkloadDistribution(solution);
Map<UUID, List<WorkItem>> assignments = optimizer.getAssignments(solution);
```

**Optimization Goal:** Minimize maximum workload across doctors

## Integration with DoctorAvailabilityService

### Example: Integrate Appointment Scheduler

```java
@Service
public class DoctorAvailabilityService {
    
    // ... existing code ...
    
    public AppointmentScheduleOptimizer.ScheduleState optimizeSchedule(
            UUID doctorId, LocalDate workDate) {
        
        // Get pending appointment requests
        List<AppointmentScheduleOptimizer.AppointmentRequest> requests = 
            appointmentRepository.findPendingForDoctor(doctorId);
        
        // Get doctor availabilities
        List<DoctorAvailability> availabilities = 
            availabilityRepo.findByDoctorIdAndWorkDate(doctorId, workDate);
        
        List<AppointmentScheduleOptimizer.DoctorAvailabilitySlot> slots = 
            availabilities.stream()
                .map(a -> new AppointmentScheduleOptimizer.DoctorAvailabilitySlot(
                    a.getDoctorId(), a.getStartTime(), a.getEndTime()))
                .collect(Collectors.toList());
        
        // Solve
        AppointmentScheduleOptimizer optimizer = new AppointmentScheduleOptimizer();
        AppointmentScheduleOptimizer.ScheduleState solution = optimizer.solve(requests, slots);
        
        logger.info("Optimized schedule: {} of {} scheduled (explored {} nodes)",
            (int)optimizer.getBestValue(), requests.size(), optimizer.getNodesExplored());
        
        return solution;
    }
}
```

## Performance Tuning

### 1. Prune Aggressively
- Tight bounds = more pruning = faster execution
- Implement realistic lower/upper bounds in `evaluate()`

### 2. Order Branches Intelligently
- In `getChildren()`, order options by likelihood of success
- Best branches first reduces nodes explored

### 3. Set Timeouts
For real-time systems, you can add a timeout mechanism:

```java
private long startTime;
private long timeoutMs = 5000; // 5 seconds

protected boolean shouldPrune(double bound) {
    if (System.currentTimeMillis() - startTime > timeoutMs) {
        return true; // Force timeout
    }
    return super.shouldPrune(bound);
}
```

### 4. Use Best-First Search (Default)
Priority queue ensures best-bound nodes are explored first, enabling earlier pruning

## Common Issues

1. **Too slow execution:** Tighten bounds in `evaluate()` method
2. **Sub-optimal solutions:** Increase search depth or verify cost function
3. **Memory issues:** Large branching factor = many nodes; reduce problem size or use bounds-only search
4. **No solution found:** Check if one exists; verify constraint satisfaction in `getChildren()`

## Testing

See `OptimizationExample.java` for complete examples of all optimizers.

Run with:
```bash
mvn clean compile exec:java -Dexec.mainClass="com.wecureit.optimization.OptimizationExample"
```

## References

- Branch and Bound algorithms: Introduction to Algorithms (CLRS)
- Bounded search: Optimal algorithms for combinatorial optimization
- Healthcare scheduling: Operations Research in Healthcare Management
