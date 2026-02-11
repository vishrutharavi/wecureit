package com.wecureit.optimization;

import java.util.List;
import java.util.PriorityQueue;

/**
 * Generic Branch and Bound framework for solving optimization problems.
 * 
 * Usage:
 *   1. Extend this class or create a subclass for your specific problem
 *   2. Implement getProblemState() to return initial state
 *   3. Implement isTerminal() to check if solution is complete
 *   4. Implement getChildren() to generate child nodes (branches)
 *   5. Implement evaluate() to calculate upper/lower bounds
 *   6. Implement cost() to calculate actual cost of a solution
 * 
 * @param <S> Type representing the state/node of the search tree
 */
public abstract class BranchAndBound<S> {

    protected static class Node<S> implements Comparable<Node<S>> {
        public S state;
        public double bound;        // Bound value (upper bound for maximization, lower bound for minimization)
        public double cost;         // Current cost/value
        public int depth;

        public Node(S state, double bound, double cost, int depth) {
            this.state = state;
            this.bound = bound;
            this.cost = cost;
            this.depth = depth;
        }

        @Override
        public int compareTo(Node<S> other) {
            // Min-heap for minimization (better bound on top)
            return Double.compare(this.bound, other.bound);
        }
    }

    protected double bestKnownValue = Double.POSITIVE_INFINITY;
    protected S bestSolution = null;
    protected int nodesExplored = 0;
    protected int nodesPruned = 0;
    protected boolean maximizing = false;

    /**
     * Main branch and bound algorithm
     * @return Best solution found
     */
    public S solve() {
        bestKnownValue = maximizing ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
        bestSolution = null;
        nodesExplored = 0;
        nodesPruned = 0;

        PriorityQueue<Node<S>> queue = new PriorityQueue<>();
        S initialState = getProblemState();
        double initialBound = evaluate(initialState);
        queue.add(new Node<>(initialState, initialBound, 0, 0));

        while (!queue.isEmpty()) {
            Node<S> node = queue.poll();
            nodesExplored++;

            // Pruning: skip if bound is worse than best known
            if (shouldPrune(node.bound)) {
                nodesPruned++;
                continue;
            }

            // Check if this is a terminal solution
            if (isTerminal(node.state)) {
                double solutionCost = cost(node.state);
                if (isBetter(solutionCost, bestKnownValue)) {
                    bestKnownValue = solutionCost;
                    bestSolution = node.state;
                }
                continue;
            }

            // Generate and enqueue child nodes
            List<S> children = getChildren(node.state);
            for (S child : children) {
                double childBound = evaluate(child);
                double childCost = cost(child);
                queue.add(new Node<>(child, childBound, childCost, node.depth + 1));
            }
        }

        return bestSolution;
    }

    /**
     * Check if current bound should be pruned
     */
    protected boolean shouldPrune(double bound) {
        if (maximizing) {
            return bound < bestKnownValue;
        } else {
            return bound > bestKnownValue;
        }
    }

    /**
     * Check if solution1 is better than solution2
     */
    protected boolean isBetter(double value1, double value2) {
        if (maximizing) {
            return value1 > value2;
        } else {
            return value1 < value2;
        }
    }

    // Abstract methods to be implemented by subclasses

    /**
     * @return Initial problem state
     */
    protected abstract S getProblemState();

    /**
     * @return true if this state represents a complete solution
     */
    protected abstract boolean isTerminal(S state);

    /**
     * @return List of child states (branches) from current state
     */
    protected abstract List<S> getChildren(S state);

    /**
     * Calculate bound (estimate of best possible value from this state)
     * @return Upper bound for maximization, lower bound for minimization
     */
    protected abstract double evaluate(S state);

    /**
     * Calculate actual cost/value of a complete solution
     * @return Cost of the solution
     */
    protected abstract double cost(S state);

    // Getters
    public double getBestValue() {
        return bestKnownValue;
    }

    public int getNodesExplored() {
        return nodesExplored;
    }

    public int getNodesPruned() {
        return nodesPruned;
    }
}
