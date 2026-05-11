package com.routepulse.domain;

/**
 * Represents a courier vehicle participating in the delivery simulation.
 * Courier state (position, status) is mutable only via MutationApplier.
 * This record captures a point-in-time snapshot of a courier.
 *
 * @param id          unique courier identifier
 * @param currentNode the graph node the courier is currently at or heading toward
 * @param capacity    maximum weight and volume this courier's vehicle can carry
 * @param status      operational status of this courier
 */
public record Courier(
        CourierId id,
        NodeId currentNode,
        CargoCapacity capacity,
        CourierStatus status) {

    public Courier {
        if (status == null) {
            throw new IllegalArgumentException("Courier status must not be null");
        }
    }

    /** Returns true if this courier can accept new orders. */
    public boolean isActive() {
        return status == CourierStatus.ACTIVE;
    }
}
