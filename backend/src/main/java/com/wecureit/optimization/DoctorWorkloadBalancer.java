package com.wecureit.optimization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Doctor Workload Balancer using Branch and Bound.
 * 
 * Problem: Given a set of appointments and available doctors,
 * find an assignment that minimizes the maximum workload (or variance)
 * across doctors while respecting specialization requirements.
 */
public class DoctorWorkloadBalancer extends BranchAndBound<DoctorWorkloadBalancer.WorkloadState> {

    public static class Doctor {
        public UUID id;
        public String name;
        public String specialization;
        
        public Doctor(UUID id, String name, String specialization) {
            this.id = id;
            this.name = name;
            this.specialization = specialization;
        }
    }

    public static class WorkItem {
        public UUID id;
        public String requiredSpecialization;
        public int workUnits; // duration, complexity, etc.
        
        public WorkItem(UUID id, String requiredSpecialization, int workUnits) {
            this.id = id;
            this.requiredSpecialization = requiredSpecialization;
            this.workUnits = workUnits;
        }
    }

    public static class WorkloadState {
        public List<WorkItem> workItems;
        public List<Doctor> doctors;
        public List<UUID> assignment; // doctor IDs for each work item
        public int nextWorkIndex;
        
        public WorkloadState(List<WorkItem> workItems, List<Doctor> doctors) {
            this.workItems = workItems;
            this.doctors = doctors;
            this.assignment = new ArrayList<>();
            this.nextWorkIndex = 0;
            for (int i = 0; i < workItems.size(); i++) {
                assignment.add(null);
            }
        }
        
        public WorkloadState copy() {
            WorkloadState copy = new WorkloadState(workItems, doctors);
            copy.assignment = new ArrayList<>(this.assignment);
            copy.nextWorkIndex = this.nextWorkIndex;
            return copy;
        }
        
        public Map<UUID, Integer> calculateWorkloads() {
            Map<UUID, Integer> workloads = new HashMap<>();
            for (Doctor doc : doctors) {
                workloads.put(doc.id, 0);
            }
            for (int i = 0; i < nextWorkIndex; i++) {
                UUID doctorId = assignment.get(i);
                if (doctorId != null) {
                    workloads.put(doctorId, workloads.get(doctorId) + workItems.get(i).workUnits);
                }
            }
            return workloads;
        }
    }

    public DoctorWorkloadBalancer() {
        this.maximizing = false; // Minimize maximum workload (or variance)
    }

    @Override
    protected WorkloadState getProblemState() {
        return new WorkloadState(new ArrayList<>(), new ArrayList<>());
    }

    public WorkloadState solve(List<WorkItem> workItems, List<Doctor> doctors) {
        this.bestKnownValue = Double.POSITIVE_INFINITY;
        this.bestSolution = null;
        this.nodesExplored = 0;
        this.nodesPruned = 0;

        return super.solve();
    }

    @Override
    protected boolean isTerminal(WorkloadState state) {
        return state.nextWorkIndex >= state.workItems.size();
    }

    @Override
    protected List<WorkloadState> getChildren(WorkloadState state) {
        List<WorkloadState> children = new ArrayList<>();

        if (isTerminal(state)) {
            return children;
        }

        WorkItem currentWork = state.workItems.get(state.nextWorkIndex);

        // Try assigning to each qualified doctor
        for (Doctor doctor : state.doctors) {
            if (isQualified(doctor, currentWork)) {
                WorkloadState child = state.copy();
                child.assignment.set(state.nextWorkIndex, doctor.id);
                child.nextWorkIndex++;
                children.add(child);
            }
        }

        return children;
    }

    @Override
    protected double evaluate(WorkloadState state) {
        // Lower bound: calculate current max workload + minimum possible for remaining items
        Map<UUID, Integer> currentWorkloads = state.calculateWorkloads();
        int maxWorkload = currentWorkloads.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        
        // Add minimum possible load from remaining items
        int remainingWork = 0;
        for (int i = state.nextWorkIndex; i < state.workItems.size(); i++) {
            remainingWork += state.workItems.get(i).workUnits;
        }
        
        int numDoctors = state.doctors.size();
        int minAdditionalLoad = remainingWork / numDoctors;
        
        return maxWorkload + minAdditionalLoad;
    }

    @Override
    protected double cost(WorkloadState state) {
        // Cost = maximum workload across all doctors (we want to minimize this)
        Map<UUID, Integer> workloads = state.calculateWorkloads();
        return workloads.values().stream().mapToInt(Integer::intValue).max().orElse(0);
    }

    private boolean isQualified(Doctor doctor, WorkItem work) {
        return doctor.specialization.equalsIgnoreCase(work.requiredSpecialization);
    }

    public Map<UUID, List<WorkItem>> getAssignments(WorkloadState solution) {
        Map<UUID, List<WorkItem>> assignments = new HashMap<>();
        
        if (solution == null) return assignments;

        // Initialize empty lists for each doctor
        for (Doctor doc : solution.doctors) {
            assignments.put(doc.id, new ArrayList<>());
        }

        // Assign work items to doctors
        for (int i = 0; i < solution.assignment.size(); i++) {
            UUID doctorId = solution.assignment.get(i);
            if (doctorId != null) {
                assignments.get(doctorId).add(solution.workItems.get(i));
            }
        }

        return assignments;
    }

    public Map<UUID, Integer> getWorkloadDistribution(WorkloadState solution) {
        if (solution == null) return new HashMap<>();
        return solution.calculateWorkloads();
    }
}
