package com.routepulse.simulation.engine;

/**
 * Marker interface representing the live simulation state layer.
 * Implemented by the concrete state aggregator (added in Day 3) that holds
 * references to GraphStore, DistanceMatrix, RouteRegistry, CargoRegistry,
 * OrderRegistry, CourierRegistry, ShadowRouteRegistry, EventLogStore, and MetricsStore.
 *
 * <p>MutationApplier receives this interface so it is decoupled from the
 * concrete state implementation — only Day 3 wires in the real aggregator.
 */
public interface SimulationState {
    // Populated in Day 3 with accessor methods for all state layer objects.
}
