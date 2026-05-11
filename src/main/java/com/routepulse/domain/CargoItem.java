package com.routepulse.domain;

/**
 * Represents a single cargo item loaded onto a courier vehicle.
 * A CargoItem is the physical representation of an Order on a vehicle —
 * it records the order it belongs to along with its physical dimensions.
 *
 * @param orderId the identifier of the Order this cargo item belongs to
 * @param weight  item weight in simulation units (must be positive)
 * @param volume  item volume in simulation units (must be positive)
 */
public record CargoItem(String orderId, double weight, double volume) {

    public CargoItem {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("CargoItem orderId must not be null or blank");
        }
        if (weight <= 0) {
            throw new IllegalArgumentException("CargoItem weight must be positive, got: " + weight);
        }
        if (volume <= 0) {
            throw new IllegalArgumentException("CargoItem volume must be positive, got: " + volume);
        }
    }
}
