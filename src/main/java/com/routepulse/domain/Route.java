package com.routepulse.domain;

import java.util.List;

/**
 * Represents the ordered sequence of delivery stops assigned to a single courier.
 * Routes are immutable value objects — any modification (insertion, reordering)
 * produces a new Route instance via MutationApplier.
 *
 * @param courierId the courier this route belongs to
 * @param stops     ordered list of stops; index 0 is the next stop to visit
 */
public record Route(CourierId courierId, List<Stop> stops) {

    public Route {
        if (stops == null) {
            throw new IllegalArgumentException("Route stops list must not be null");
        }
        // Defensive copy to preserve immutability
        stops = List.copyOf(stops);
    }

    /**
     * Returns the number of remaining (non-delivered) stops on this route.
     * This is the key metric the DP stop cap is evaluated against.
     *
     * @return count of stops with status PENDING or IN_TRANSIT
     */
    public int remainingStopCount() {
        return (int) stops.stream()
                .filter(s -> s.status() != StopStatus.DELIVERED)
                .count();
    }

    /** Returns true if this route has no stops assigned. */
    public boolean isEmpty() {
        return stops.isEmpty();
    }
}
