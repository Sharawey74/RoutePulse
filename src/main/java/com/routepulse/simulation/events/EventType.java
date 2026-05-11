package com.routepulse.simulation.events;

/**
 * Enumeration of all discrete event types that can occur during the simulation.
 * Each type maps to a specific algorithm chain in the EventDispatcher.
 *
 * <p>Algorithm chains (wired from Day 3 onward):
 * <ul>
 *   <li>{@link #NEW_ORDER}           → GreedyInsertion → BinPackingValidation → ShadowUpdate</li>
 *   <li>{@link #ROAD_WEIGHT_CHANGE}  → Dijkstra(affected sources) → DistanceMatrix update</li>
 *   <li>{@link #ROAD_REMOVAL}        → Dijkstra(affected couriers) → Route re-check</li>
 *   <li>{@link #VEHICLE_BREAKDOWN}   → CourierDeactivate → BinPackingFFD → GreedyInsertion</li>
 *   <li>{@link #PRIORITY_ESCALATION} → GreedyInsertion(priority-weighted)</li>
 *   <li>{@link #QUIET_PERIOD}        → DPReoptimiser(per eligible courier) → ShadowComparison</li>
 * </ul>
 */
public enum EventType {

    /** A new delivery order has entered the system and must be assigned to a courier. */
    NEW_ORDER,

    /**
     * The traversal weight of a road segment has changed (congestion, road works).
     * Triggers Dijkstra re-run from all couriers whose current routes use this edge.
     */
    ROAD_WEIGHT_CHANGE,

    /**
     * A road segment has been completely removed (closure, accident).
     * Triggers Dijkstra re-run for all couriers whose routes pass through this edge.
     */
    ROAD_REMOVAL,

    /**
     * A courier vehicle has broken down.
     * Triggers deactivation, FFD bin-packing redistribution, and greedy re-insertion.
     */
    VEHICLE_BREAKDOWN,

    /**
     * An existing order has been escalated to HIGH priority.
     * Triggers priority-weighted greedy re-insertion.
     */
    PRIORITY_ESCALATION,

    /**
     * A quiet period has been detected — no disruption events for N consecutive ticks.
     * Triggers DP re-optimization for all couriers with more than 3 remaining stops.
     */
    QUIET_PERIOD
}
