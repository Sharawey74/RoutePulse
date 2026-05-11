package com.routepulse.domain;

/**
 * Value object uniquely identifying a courier in the simulation.
 * Uses a String identifier to allow meaningful labels (e.g., "C1", "C2").
 *
 * @param value the string courier identifier, non-null and non-blank
 */
public record CourierId(String value) {

    public CourierId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("CourierId value must not be null or blank");
        }
    }
}
