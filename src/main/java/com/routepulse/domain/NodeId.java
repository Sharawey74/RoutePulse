package com.routepulse.domain;

/**
 * Value object uniquely identifying a graph node in the delivery network.
 * Wraps a primitive int to give semantic meaning and prevent primitive obsession.
 *
 * @param value the integer node identifier (0-indexed; 0 is always the depot)
 */
public record NodeId(int value) {

    /** Canonical depot node identifier. The depot is always node 0. */
    public static final NodeId DEPOT = new NodeId(0);

    public NodeId {
        if (value < 0) {
            throw new IllegalArgumentException(
                    "NodeId value must be non-negative, got: " + value);
        }
    }
}
