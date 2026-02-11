# Branch and Bound Optimization Integration Guide

This guide explains how to use the integrated Branch and Bound optimization framework in your WecureIT project.

## What Was Added

### 1. Optimization Framework (`/backend/src/main/java/com/wecureit/optimization/`)
- **BranchAndBound.java** - Generic B&B algorithm framework
- **AppointmentScheduleOptimizer.java** - Schedule optimization
- **RoomAllocationOptimizer.java** - Room allocation optimization
- **DoctorWorkloadBalancer.java** - Workload distribution optimization
- **OptimizationExample.java** - Usage examples
- **README.md** - Detailed documentation

### 2. Integration Service (`OptimizationService.java`)
Wraps the optimizers with business logic integration:
- `optimizeAppointmentScheduling()` - Optimize doctor schedules
- `optimizeDoctorWorkload()` - Balance workload across doctors
- `optimizeRoomAllocation()` - Allocate rooms to appointments

### 3. Enhanced Services
Added optimization methods to:
- **DoctorAvailabilityService** - `optimizeScheduling()`
- **AppointmentService** - `optimizeWorkloadDistribution()`, `optimizeRoomAllocation()`
- **PatientBookingService** - `getOptimizedAvailabilitySlots()`

### 4. REST Controller (`OptimizationController.java`)
Exposes optimization endpoints:
- `GET /api/optimization/schedule/{doctorId}/{workDate}` - Optimize a doctor's schedule
- `POST /api/optimization/workload` - Optimize workload distribution
- `POST /api/optimization/rooms` - Optimize room allocation
- `GET /api/optimization/metrics/{doctorId}/{workDate}` - Get optimization metrics

### 5. Enhanced DTOs
- **BookingAvailabilityResponse** - Added OptimizationMetrics inner class to show B&B statistics

## Usage Examples

### Example 1: Optimize Doctor Schedule

Via REST API:
```bash
curl -X GET http://localhost:8080/api/optimization/schedule/550e8400-e29b-41d4-a716-446655440000/2024-02-10
```

Via Service:
```java
@Autowired
private OptimizationService optimizationService;

public void optimizeSchedule() {
    UUID doctorId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    LocalDate workDate = LocalDate.of(2024, 2, 10);
    
    OptimizationService.SchedulingOptimizationResult result = 
        optimizationService.optimizeAppointmentScheduling(doctorId, workDate);
    
    System.out.println("Appointments scheduled: " + result.appointmentsScheduled);
    System.out.println("Nodes explored: " + result.nodesExplored);
    System.out.println("Nodes pruned: " + result.nodesPruned);
}
```

### Example 2: Optimize Workload Distribution

Via REST API:
```bash
curl -X POST http://localhost:8080/api/optimization/workload \
  -H "Content-Type: application/json" \
  -d '{
    "appointmentIds": [
      "550e8400-e29b-41d4-a716-446655440000",
      "550e8400-e29b-41d4-a716-446655440001"
    ],
    "doctorIds": [
      "550e8400-e29b-41d4-a716-446655440010",
      "550e8400-e29b-41d4-a716-446655440011"
    ]
  }'
```

Via Service:
```java
@Autowired
private AppointmentService appointmentService;

public void balanceWorkload() {
    List<Long> appointmentIds = Arrays.asList(1L, 2L, 3L, 4L);
    List<UUID> doctorIds = Arrays.asList(
        UUID.fromString("550e8400-e29b-41d4-a716-446655440010"),
        UUID.fromString("550e8400-e29b-41d4-a716-446655440011")
    );
    
    OptimizationService.WorkloadOptimizationResult result = 
        appointmentService.optimizeWorkloadDistribution(appointmentIds, doctorIds);
    
    System.out.println("Max workload: " + result.maxWorkload);
    System.out.println("Distribution: " + result.workloadDistribution);
}
```

### Example 3: Optimize Room Allocation

Via REST API:
```bash
curl -X POST http://localhost:8080/api/optimization/rooms \
  -H "Content-Type: application/json" \
  -d '{
    "appointmentIds": [
      "550e8400-e29b-41d4-a716-446655440000",
      "550e8400-e29b-41d4-a716-446655440001"
    ],
    "facilityId": "550e8400-e29b-41d4-a716-446655440020"
  }'
```

Via Service:
```java
@Autowired
private AppointmentService appointmentService;

public void allocateRooms() {
    List<UUID> appointmentIds = Arrays.asList(
        UUID.fromString("550e8400-e29b-41d4-a716-446655440000")
    );
    UUID facilityId = UUID.fromString("550e8400-e29b-41d4-a716-446655440020");
    
    OptimizationService.RoomAllocationOptimizationResult result = 
        appointmentService.optimizeRoomAllocation(appointmentIds, facilityId);
    
    System.out.println("Appointments allocated: " + result.appointmentsAllocated);
}
```

### Example 4: Get Optimized Availability Slots

```java
@Autowired
private PatientBookingService patientBookingService;

public void getOptimizedSlots() {
    UUID doctorId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    UUID facilityId = UUID.fromString("550e8400-e29b-41d4-a716-446655440020");
    LocalDate workDate = LocalDate.of(2024, 2, 10);
    Integer duration = 30; // 30 minutes
    
    com.wecureit.dto.response.BookingAvailabilityResponse response = 
        patientBookingService.getOptimizedAvailabilitySlots(
            doctorId, facilityId, workDate, duration
        );
    
    // Response includes optimization metrics
    if (response.getOptimizationMetrics() != null) {
        System.out.println("Scheduled: " + response.getOptimizationMetrics().getAppointmentsScheduled());
        System.out.println("Pending: " + response.getOptimizationMetrics().getAppointmentsPending());
    }
}
```

## Performance Characteristics

### Time Complexity
- **Best case**: O(b^d) where branches are heavily pruned early
- **Average case**: O(b^(d*α)) where 0 < α < 1 due to pruning
- **Worst case**: O(b^d) when no pruning occurs (rare)

Where:
- b = branching factor (average children per node)
- d = depth of search tree

### Space Complexity
- O(d × b) for the priority queue in worst case

### Optimization Metrics
Each result includes:
- `nodesExplored` - Total nodes explored in search tree
- `nodesPruned` - Nodes eliminated through bounds pruning
- `appointmentsScheduled` / `appointmentsAllocated` - Results
- Pruning ratio = nodesPruned / (nodesExplored + nodesPruned)

## Tuning for Better Performance

### 1. Tighten Bounds
In the optimizer classes, the `evaluate()` method returns bounds. Tighter bounds = more pruning.

Example improvement for AppointmentScheduleOptimizer:
```java
@Override
protected double evaluate(ScheduleState state) {
    // Current: optimistic bound
    int scheduled = count(state.schedule, scheduled_results);
    
    // Better: consider slot compatibility
    int maxPossible = countCompatible(state.requests, state.availabilities);
    return scheduled + Math.min(maxPossible, remaining.size());
}
```

### 2. Order Branches Intelligently
In `getChildren()`, order by likelihood of success:
```java
// Order by appointment priority (high priority first)
Collections.sort(children, (a, b) -> 
    Integer.compare(b.priority, a.priority)
);
```

### 3. Add Timeouts
For real-time scenarios, add timeout handling:
```java
if (System.currentTimeMillis() - startTime > TIMEOUT_MS) {
    return bestSolution; // Return best found so far
}
```

### 4. Use Smaller Subproblems
Instead of optimizing all at once, divide:
```java
// Optimize per-facility, per-date, etc.
for (UUID facilityId : facilityIds) {
    optimizeFor(facilityId);
}
```

## Monitoring and Logging

All optimization operations are logged at INFO and ERROR levels:

```
2024-02-10 10:30:45.123 INFO  OptimizationService: Optimized appointment scheduling for doctor 550e8400... on 2024-02-10: 8 of 10 scheduled (explored 127 nodes)
2024-02-10 10:31:02.456 INFO  OptimizationService: Optimized doctor workload: max workload = 450 units across 3 doctors (explored 84 nodes)
```

Check application logs for:
- Successful optimizations
- Error messages
- Performance metrics

## Integration Points

### Current Integrations
1. **DoctorAvailabilityService** - Uses in `optimizeScheduling()`
2. **AppointmentService** - Uses for workload and room optimization  
3. **PatientBookingService** - Uses for optimized slot selection

### Future Integration Opportunities
1. **Admin Dashboard** - Show optimization metrics
2. **Batch Jobs** - Run nightly optimization
3. **Alerts** - Notify when imbalance detected
4. **Reports** - Optimization statistics
5. **Machine Learning** - Feed optimization results to ML models

## Testing

Run the example class:
```bash
cd /Users/ullasbc/Documents/Projects/wecureit-bb/backend
mvn clean compile
mvn exec:java -Dexec.mainClass="com.wecureit.optimization.OptimizationExample"
```

## Troubleshooting

### Issue: Slow optimization
**Solution**: 
- Check branching factor - too many branches = slow
- Review `evaluate()` method bounds
- Add timeout for real-time use

### Issue: Sub-optimal solutions
**Solution**:
- Verify cost function in `cost()` method is correct
- Check pruning logic isn't too aggressive
- Increase search depth if time allows

### Issue: Memory errors
**Solution**:
- Reduce problem size
- Use subproblems instead of monolithic
- Check for memory leaks in state objects

## References

- Source: `/backend/src/main/java/com/wecureit/optimization/`
- Services: `/backend/src/main/java/com/wecureit/service/OptimizationService.java`
- Controller: `/backend/src/main/java/com/wecureit/controller/OptimizationController.java`
- Framework: Knuth's Dancing Links, Introduction to Algorithms (CLRS)
