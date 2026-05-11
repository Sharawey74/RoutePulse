package com.routepulse.domain;

/**
 * Value object representing a non-negative distance between two graph nodes.
 * Distances are always in abstract simulation units (not real-world kilometres).
 * {@code Double.MAX_VALUE} represents an unreachable node (no path exists).
 *
 * @param kilometers the distance value in simulation units (≥ 0, or Double.MAX_VALUE)
 */
public record Distance(double kilometers) {

    /** Sentinel value representing an unreachable destination — no path exists. */
    public static final Distance UNREACHABLE = new Distance(Double.MAX_VALUE);

    /** Zero distance — used for same-node lookups. */
    public static final Distance ZERO = new Distance(0.0);

    public Distance {
        if (kilometers < 0) {
            throw new IllegalArgumentException(
                    "Distance must be non-negative, got: " + kilometers);
        }
    }

    /** Returns true if this distance represents an unreachable node. */
    public boolean isUnreachable() {
        return kilometers == Double.MAX_VALUE;
    }
}
