package com.routepulse.domain;

/**
 * Enumeration of courier operational status values.
 * A courier can only receive new orders when ACTIVE.
 */
public enum CourierStatus {
    /** Courier is operational and eligible to receive orders. */
    ACTIVE,
    /** Courier has broken down and is temporarily out of service. */
    BROKEN_DOWN,
    /** Courier has completed all assigned deliveries for the shift. */
    SHIFT_COMPLETE
}
