package com.routepulse.algorithm.dijkstra;

import com.routepulse.algorithm.api.MetricsRecord;

/**
 * Execution metrics produced by a single run of the Dijkstra algorithm module.
 * Captured and stored by the EventLogStore after each invocation for report generation.
 *
 * @param algorithmName    always "Dijkstra's Algorithm"
 * @param sourceNodeId     the integer value of the source node that was processed
 * @param nodesVisited     number of nodes extracted from the priority queue during this run
 * @param edgesRelaxed     number of edge relaxation operations performed
 * @param executionTimeMs  wall-clock time from invocation to completion
 */
public record DijkstraMetrics(
        String algorithmName,
        int sourceNodeId,
        int nodesVisited,
        int edgesRelaxed,
        long executionTimeMs) implements MetricsRecord {
}
