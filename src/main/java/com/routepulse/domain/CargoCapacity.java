package com.routepulse.domain;

/**
 * Value object describing the cargo capacity constraints of a courier vehicle.
 * Both weight and volume must be satisfied simultaneously for a valid load.
 *
 * @param maxWeight maximum cargo weight the vehicle can carry (simulation units)
 * @param maxVolume maximum cargo volume the vehicle can carry (simulation units)
 */
public record CargoCapacity(double maxWeight, double maxVolume) {

    public CargoCapacity {
        if (maxWeight <= 0) {
            throw new IllegalArgumentException("maxWeight must be positive, got: " + maxWeight);
        }
        if (maxVolume <= 0) {
            throw new IllegalArgumentException("maxVolume must be positive, got: " + maxVolume);
        }
    }
}
