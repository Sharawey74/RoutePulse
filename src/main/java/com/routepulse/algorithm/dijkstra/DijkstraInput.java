package com.routepulse.algorithm.dijkstra;

import com.routepulse.algorithm.api.AlgorithmInput;
import com.routepulse.domain.Edge;
import com.routepulse.domain.NodeId;

import java.util.List;
import java.util.Map;

/**
 * Immutable input snapshot for the Dijkstra algorithm module.
 * Captures the graph topology and source node at a point in time.
 * The algorithm module never reads from the live GraphStore directly.
 *
 * @param adjacencySnapshot a deep copy of the adjacency list at the time of invocation
 * @param sourceNode        the node from which all shortest paths are computed
 * @param allNodes          the complete set of node IDs in the graph at invocation time
 */
public record DijkstraInput(
        Map<NodeId, List<Edge>> adjacencySnapshot,
        NodeId sourceNode,
        java.util.Set<NodeId> allNodes) implements AlgorithmInput {

    public DijkstraInput {
        if (adjacencySnapshot == null) {
            throw new IllegalArgumentException("DijkstraInput adjacencySnapshot must not be null");
        }
        if (sourceNode == null) {
            throw new IllegalArgumentException("DijkstraInput sourceNode must not be null");
        }
        if (allNodes == null || allNodes.isEmpty()) {
            throw new IllegalArgumentException("DijkstraInput allNodes must not be null or empty");
        }
        // Defensive copies to guarantee immutability of this snapshot
        adjacencySnapshot = Map.copyOf(adjacencySnapshot);
        allNodes = java.util.Set.copyOf(allNodes);
    }
}
