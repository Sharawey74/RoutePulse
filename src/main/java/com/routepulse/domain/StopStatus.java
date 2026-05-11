package com.routepulse.domain;

/**
 * Enumeration of lifecycle states for a delivery stop within a courier's route.
 * Stops advance through these states in order during simulation execution.
 */
public enum StopStatus {
    /** Stop is planned but the courier has not yet departed toward it. */
    PENDING,
    /** The courier is currently en route to this stop. */
    IN_TRANSIT,
    /** The stop has been successfully completed — order delivered. */
    DELIVERED
}
