package com.routepulse.domain;

/**
 * Represents a single delivery order to be fulfilled during the simulation shift.
 * An Order transitions through its lifecycle via the OrderRegistry,
 * mutated only by MutationApplier in response to algorithm outputs.
 *
 * @param id           unique order identifier (e.g., "ORD-001")
 * @param deliveryNode the graph node where this order must be delivered
 * @param weight       cargo weight (simulation units; 0.15–0.2 small, 0.6–0.7 large)
 * @param volume       cargo volume (simulation units; mirrors weight distribution)
 * @param priority     order priority level (affects greedy insertion cost weighting)
 */
public record Order(
        String id,
        NodeId deliveryNode,
        double weight,
        double volume,
        OrderPriority priority) {

    public Order {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Order id must not be null or blank");
        }
        if (weight <= 0) {
            throw new IllegalArgumentException("Order weight must be positive, got: " + weight);
        }
        if (volume <= 0) {
            throw new IllegalArgumentException("Order volume must be positive, got: " + volume);
        }
        if (priority == null) {
            throw new IllegalArgumentException("Order priority must not be null");
        }
    }
}
