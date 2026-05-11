package com.routepulse.algorithm.dijkstra;

import com.routepulse.algorithm.api.AlgorithmComplexity;
import com.routepulse.algorithm.api.AlgorithmModule;
import com.routepulse.domain.Edge;
import com.routepulse.domain.NodeId;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Pure-function implementation of Dijkstra's single-source shortest-path algorithm.
 * Implements the Strategy pattern via {@link AlgorithmModule}.
 *
 * <p><strong>Algorithm:</strong> Standard single-source Dijkstra using a binary min-heap
 * (Java {@code PriorityQueue}). Time complexity: O((V + E) log V).
 * Space complexity: O(V) for the distance and predecessor maps.
 *
 * <p><strong>Purity contract:</strong> This class has no mutable instance state.
 * Every call to {@link #solve(DijkstraInput)} is completely self-contained.
 * It reads only from the immutable {@link DijkstraInput} snapshot — never from
 * the live {@code GraphStore} or any other shared mutable state.
 *
 * <p><strong>Design rules enforced:</strong>
 * <ul>
 *   <li>This class does NOT import from {@code simulation} or {@code api} (REST) packages.</li>
 *   <li>Full single-source re-run only — no incremental Dijkstra.</li>
 *   <li>Always runs from the source node through all reachable nodes.</li>
 * </ul>
 */
@Component
public class DijkstraModule implements AlgorithmModule<DijkstraInput, DijkstraOutput> {

    private static final AlgorithmComplexity COMPLEXITY =
            new AlgorithmComplexity("Dijkstra's Algorithm", "O((V + E) log V)");

    @Override
    public String algorithmName() {
        return COMPLEXITY.name();
    }

    @Override
    public AlgorithmComplexity reportedComplexity() {
        return COMPLEXITY;
    }

    /**
     * Executes Dijkstra's algorithm on the graph snapshot provided in the input.
     *
     * <p>Algorithm steps:
     * <ol>
     *   <li>Initialize all distances to {@code Double.MAX_VALUE} (unreachable).</li>
     *   <li>Set source distance to 0 and enqueue it.</li>
     *   <li>Extract the minimum-distance node from the priority queue.</li>
     *   <li>For each neighbor, relax the edge if a shorter path is found.</li>
     *   <li>Repeat until the queue is empty.</li>
     * </ol>
     *
     * @param input immutable graph snapshot and source node
     * @return {@link DijkstraOutput} containing full distance and predecessor maps
     */
    @Override
    public DijkstraOutput solve(DijkstraInput input) {
        long startTime = System.currentTimeMillis();

        Map<NodeId, Double> distMap = initializeDistances(input);
        Map<NodeId, NodeId> predecessorMap = new HashMap<>();

        // Min-heap ordered by tentative distance: (distance, nodeId)
        PriorityQueue<NodeDistance> minHeap = new PriorityQueue<>();
        minHeap.offer(new NodeDistance(input.sourceNode(), 0.0));

        int nodesVisited = 0;
        int edgesRelaxed = 0;

        while (!minHeap.isEmpty()) {
            NodeDistance current = minHeap.poll();

            // Skip if we have already found a shorter path to this node
            if (current.distance() > distMap.get(current.nodeId())) {
                continue;
            }

            nodesVisited++;
            List<Edge> neighbors = input.adjacencySnapshot()
                    .getOrDefault(current.nodeId(), List.of());

            for (Edge edge : neighbors) {
                edgesRelaxed++;
                double newDistance = distMap.get(current.nodeId()) + edge.weight();

                if (newDistance < distMap.getOrDefault(edge.to(), Double.MAX_VALUE)) {
                    distMap.put(edge.to(), newDistance);
                    predecessorMap.put(edge.to(), current.nodeId());
                    minHeap.offer(new NodeDistance(edge.to(), newDistance));
                }
            }
        }

        long executionTimeMs = System.currentTimeMillis() - startTime;
        DijkstraMetrics metrics = new DijkstraMetrics(
                algorithmName(),
                input.sourceNode().value(),
                nodesVisited,
                edgesRelaxed,
                executionTimeMs);

        return new DijkstraOutput(input.sourceNode(), distMap, predecessorMap, metrics);
    }

    // ── Private helpers ─────────────────────────────────────────────────────

    /**
     * Initializes the distance map with MAX_VALUE for all nodes except the source (0.0).
     *
     * @param input the Dijkstra input containing all known nodes
     * @return mutable distance map ready for relaxation
     */
    private Map<NodeId, Double> initializeDistances(DijkstraInput input) {
        Map<NodeId, Double> distMap = new HashMap<>();
        for (NodeId node : input.allNodes()) {
            distMap.put(node, Double.MAX_VALUE);
        }
        distMap.put(input.sourceNode(), 0.0);
        return distMap;
    }

    /**
     * Internal value type for the priority queue entry.
     * Ordered ascending by distance so the min-heap always yields the closest node.
     *
     * @param nodeId   the graph node
     * @param distance the tentative shortest distance to this node
     */
    private record NodeDistance(NodeId nodeId, double distance)
            implements Comparable<NodeDistance> {

        @Override
        public int compareTo(NodeDistance other) {
            return Double.compare(this.distance, other.distance);
        }
    }
}
