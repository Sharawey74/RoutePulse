package com.routepulse.domain;

/**
 * Represents a single graph node (delivery location or depot) in the delivery network.
 * Canvas coordinates (x, y) are used exclusively for frontend rendering — they carry
 * no algorithmic meaning. All algorithmic distances use the precomputed DistanceMatrix.
 *
 * @param id     unique node identifier
 * @param label  human-readable name displayed on the map canvas (e.g., "Depot", "A1")
 * @param x      canvas x-coordinate for frontend rendering (pixels, non-negative)
 * @param y      canvas y-coordinate for frontend rendering (pixels, non-negative)
 */
public record Node(NodeId id, String label, int x, int y) {

    public Node {
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("Node label must not be null or blank");
        }
        if (x < 0 || y < 0) {
            throw new IllegalArgumentException(
                    "Canvas coordinates must be non-negative, got x=" + x + " y=" + y);
        }
    }
}
