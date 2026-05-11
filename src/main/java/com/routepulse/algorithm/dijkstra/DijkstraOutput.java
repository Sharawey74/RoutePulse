package com.routepulse.algorithm.dijkstra;

import com.routepulse.algorithm.api.AlgorithmOutput;
import com.routepulse.domain.NodeId;

import java.util.Map;

/**
 * Immutable output produced by a single Dijkstra algorithm run.
 * Contains the full shortest-path distance map and predecessor map for
 * the source node specified in the corresponding {@link DijkstraInput}.
 *
 * <p>Consumers use {@code distanceMap} to update the DistanceMatrix row,
 * and {@code predecessorMap} to reconstruct optimal paths if needed.
 *
 * @param sourceNode      the node from which all distances were computed
 * @param distanceMap     maps each NodeId to its shortest distance from sourceNode;
 *                        {@code Double.MAX_VALUE} means the node is unreachable
 * @param predecessorMap  maps each NodeId to its predecessor on the shortest path;
 *                        null entry means the node is the source or unreachable
 * @param metrics         execution statistics for this Dijkstra run
 */
public record DijkstraOutput(
        NodeId sourceNode,
        Map<NodeId, Double> distanceMap,
        Map<NodeId, NodeId> predecessorMap,
        DijkstraMetrics metrics) implements AlgorithmOutput {

    public DijkstraOutput {
        if (distanceMap == null) {
            throw new IllegalArgumentException("DijkstraOutput distanceMap must not be null");
        }
        if (predecessorMap == null) {
            throw new IllegalArgumentException("DijkstraOutput predecessorMap must not be null");
        }
        // Defensive copies to guarantee output immutability
        distanceMap = Map.copyOf(distanceMap);
        predecessorMap = Map.copyOf(predecessorMap);
    }

    /**
     * Returns the shortest distance to the given destination node.
     *
     * @param destination target node
     * @return shortest distance, or {@code Double.MAX_VALUE} if unreachable
     */
    public double distanceTo(NodeId destination) {
        return distanceMap.getOrDefault(destination, Double.MAX_VALUE);
    }

    /**
     * Returns true if the given node is reachable from the source node.
     *
     * @param node target node to check
     * @return true if a finite path exists from sourceNode to node
     */
    public boolean isReachable(NodeId node) {
        double distance = distanceTo(node);
        return distance < Double.MAX_VALUE;
    }
}
