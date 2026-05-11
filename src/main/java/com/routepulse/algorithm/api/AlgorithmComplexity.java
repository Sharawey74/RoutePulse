package com.routepulse.algorithm.api;

/**
 * Value object describing the time complexity of an algorithm module.
 * Used for metrics reporting and educational output in the simulation report.
 *
 * @param name         human-readable algorithm name (e.g., "Dijkstra's Algorithm")
 * @param bigONotation formal Big-O notation string (e.g., "O((V + E) log V)")
 */
public record AlgorithmComplexity(String name, String bigONotation) {
}
