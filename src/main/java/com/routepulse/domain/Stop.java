package com.routepulse.domain;

/**
 * Represents a single planned delivery stop within a courier's route.
 * A Stop links an Order to its delivery node and tracks fulfillment status.
 *
 * @param orderId the identifier of the order to be delivered at this stop
 * @param nodeId  the graph node where the delivery must occur
 * @param status  current lifecycle status of this stop
 */
public record Stop(String orderId, NodeId nodeId, StopStatus status) {

    public Stop {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("Stop orderId must not be null or blank");
        }
        if (status == null) {
            throw new IllegalArgumentException("Stop status must not be null");
        }
    }

    /**
     * Returns a new Stop with the given status, preserving orderId and nodeId.
     *
     * @param newStatus the new lifecycle status
     * @return a new Stop instance with updated status
     */
    public Stop withStatus(StopStatus newStatus) {
        return new Stop(orderId, nodeId, newStatus);
    }
}
