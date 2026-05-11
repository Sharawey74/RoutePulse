package com.routepulse.domain;

/**
 * Represents a directed weighted edge in the delivery network graph.
 * All edges are directed — a road from A to B does not imply a road from B to A.
 * Edge weights represent traversal cost (1–20 per spec) and are mutable
 * via ROAD_WEIGHT_CHANGE events, applied exclusively through MutationApplier.
 *
 * @param from   source node identifier
 * @param to     destination node identifier
 * @param weight traversal cost (integer 1–20 per committed scale parameters)
 */
public record Edge(NodeId from, NodeId to, int weight) {

    /** Minimum legal edge weight per committed scale parameters. */
    private static final int MIN_WEIGHT = 1;
    /** Maximum legal edge weight per committed scale parameters. */
    private static final int MAX_WEIGHT = 20;

    public Edge {
        if (weight < MIN_WEIGHT || weight > MAX_WEIGHT) {
            throw new IllegalArgumentException(
                    "Edge weight must be between " + MIN_WEIGHT + " and " + MAX_WEIGHT
                    + ", got: " + weight);
        }
        if (from.equals(to)) {
            throw new IllegalArgumentException(
                    "Self-loop edges are not permitted: from=" + from + " to=" + to);
        }
    }

    /**
     * Returns a new Edge with the updated weight, preserving from and to.
     * This method does not mutate — callers must replace the edge in GraphStore.
     *
     * @param newWeight the new edge weight (must be within 1–20)
     * @return a new Edge instance with the same endpoints and new weight
     */
    public Edge withWeight(int newWeight) {
        return new Edge(from, to, newWeight);
    }
}
