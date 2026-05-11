package com.routepulse.algorithm.api;

/**
 * Marker interface for algorithm execution metrics records.
 * Each algorithm module produces a concrete MetricsRecord containing
 * execution time, nodes visited, and any algorithm-specific counters.
 */
public interface MetricsRecord {

    /** Returns the wall-clock execution time of the algorithm in milliseconds. */
    long executionTimeMs();

    /** Returns the human-readable name of the algorithm that produced this record. */
    String algorithmName();
}
