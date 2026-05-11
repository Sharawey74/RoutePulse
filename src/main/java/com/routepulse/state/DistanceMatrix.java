package com.routepulse.state;

import com.routepulse.domain.NodeId;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Precomputed 30×30 shortest-path distance matrix for the delivery network.
 * Provides O(1) distance lookups between any two nodes, avoiding redundant
 * Dijkstra executions during route cost calculations.
 *
 * <p>The matrix is populated at simulation init by running Dijkstra from all nodes.
 * After a ROAD_WEIGHT_CHANGE or ROAD_REMOVAL event, only the affected source rows
 * are recomputed and updated via {@link #updateRow(NodeId, double[])}.
 *
 * <p><strong>Mutation contract:</strong> Only {@code MutationApplier} may call
 * {@link #updateRow(NodeId, double[])}. All other code uses read-only access.
 */
@Component
public class DistanceMatrix {

    /** Maximum number of nodes supported — matches committed scale parameter (30×30). */
    private static final int MAX_NODES = 30;

    /** The backing 2D array. matrix[from][to] = shortest path distance from 'from' to 'to'. */
    private final double[][] matrix;

    public DistanceMatrix() {
        matrix = new double[MAX_NODES][MAX_NODES];
        // Initialize all distances to infinity — unreachable by default
        for (double[] row : matrix) {
            Arrays.fill(row, Double.MAX_VALUE);
        }
        // Distance from any node to itself is zero
        for (int i = 0; i < MAX_NODES; i++) {
            matrix[i][i] = 0.0;
        }
    }

    /**
     * Returns the precomputed shortest-path distance from {@code from} to {@code to}.
     * Returns {@code Double.MAX_VALUE} if no path exists (node unreachable).
     *
     * @param from source node identifier
     * @param to   destination node identifier
     * @return shortest-path distance, or {@code Double.MAX_VALUE} if unreachable
     * @throws IllegalArgumentException if either node ID exceeds the matrix bounds
     */
    public double get(NodeId from, NodeId to) {
        validateIndex(from.value(), "from");
        validateIndex(to.value(), "to");
        return matrix[from.value()][to.value()];
    }

    /**
     * Replaces all distances for the given source node with the newly computed values.
     * Called by MutationApplier after Dijkstra completes for a single source node.
     *
     * @param source       the source node whose row is being updated
     * @param newDistances array of length MAX_NODES containing new shortest-path distances
     * @throws IllegalArgumentException if the array length does not match MAX_NODES
     */
    public void updateRow(NodeId source, double[] newDistances) {
        validateIndex(source.value(), "source");
        if (newDistances.length != MAX_NODES) {
            throw new IllegalArgumentException(
                    "newDistances array must have length " + MAX_NODES
                    + ", got: " + newDistances.length);
        }
        System.arraycopy(newDistances, 0, matrix[source.value()], 0, MAX_NODES);
    }

    /**
     * Returns a defensive copy of the full distance matrix.
     * Used for algorithm input snapshot construction — never exposes the live array.
     *
     * @return a deep copy of the 30×30 distance matrix
     */
    public double[][] snapshot() {
        double[][] copy = new double[MAX_NODES][MAX_NODES];
        for (int i = 0; i < MAX_NODES; i++) {
            System.arraycopy(matrix[i], 0, copy[i], 0, MAX_NODES);
        }
        return copy;
    }

    /** Returns the configured maximum node capacity of this matrix. */
    public int capacity() {
        return MAX_NODES;
    }

    // ── Private helpers ─────────────────────────────────────────────────────

    private void validateIndex(int index, String paramName) {
        if (index < 0 || index >= MAX_NODES) {
            throw new IllegalArgumentException(
                    paramName + " node index " + index + " is out of bounds [0, " + MAX_NODES + ")");
        }
    }
}
