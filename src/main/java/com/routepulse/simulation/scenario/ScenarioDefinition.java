package com.routepulse.simulation.scenario;

import com.routepulse.domain.NodeId;

import java.util.List;

/**
 * Immutable data structure representing a complete scenario configuration.
 * Loaded from {@code src/main/resources/scenarios/{name}.json} by {@link ScenarioLoader}.
 *
 * <p><strong>Day 2:</strong> Structure defined. JSON loading deferred to Day 3
 * when the state layer is available to wire in courier/order initialization.
 *
 * @param scenarioName      human-readable scenario identifier (e.g., "rush_hour")
 * @param graphResource     classpath path to the graph JSON (e.g., "graphs/demo_graph.json")
 * @param courierCount      number of active couriers to initialize (4–6 per committed scale)
 * @param courierCapacity   cargo capacity for all couriers (weight and volume)
 * @param orderSchedule     time-ordered list of order events to inject during the simulation
 * @param disruptionSchedule time-ordered list of road/vehicle disruption events
 */
public record ScenarioDefinition(
        String scenarioName,
        String graphResource,
        int courierCount,
        CourierCapacityConfig courierCapacity,
        List<ScheduledOrderEvent> orderSchedule,
        List<ScheduledDisruptionEvent> disruptionSchedule) {

    public ScenarioDefinition {
        if (scenarioName == null || scenarioName.isBlank()) {
            throw new IllegalArgumentException("ScenarioDefinition scenarioName must not be blank");
        }
        if (courierCount < 4 || courierCount > 6) {
            throw new IllegalArgumentException(
                    "ScenarioDefinition courierCount must be 4–6 per committed scale, got: " + courierCount);
        }
        // Defensive copies
        orderSchedule     = List.copyOf(orderSchedule);
        disruptionSchedule = List.copyOf(disruptionSchedule);
    }

    /**
     * Courier capacity configuration shared across all couriers in a scenario.
     *
     * @param maxWeight maximum cargo weight capacity per courier vehicle
     * @param maxVolume maximum cargo volume capacity per courier vehicle
     */
    public record CourierCapacityConfig(double maxWeight, double maxVolume) {
    }

    /**
     * A single order injection event in the scenario timeline.
     *
     * @param atTick          the tick at which this order arrives
     * @param deliveryNodeId  the destination node for this order
     * @param weight          order cargo weight
     * @param volume          order cargo volume
     * @param priorityHigh    true if this order starts as HIGH priority
     */
    public record ScheduledOrderEvent(
            int atTick,
            int deliveryNodeId,
            double weight,
            double volume,
            boolean priorityHigh) {
    }

    /**
     * A single disruption event in the scenario timeline.
     *
     * @param atTick       the tick at which this disruption occurs
     * @param type         the disruption type: "ROAD_WEIGHT_CHANGE", "ROAD_REMOVAL", or "VEHICLE_BREAKDOWN"
     * @param fromNodeId   from-node for road events (0 for vehicle events)
     * @param toNodeId     to-node for road events (0 for vehicle events)
     * @param newWeight    new edge weight for ROAD_WEIGHT_CHANGE events (0 for others)
     * @param courierId    courier identifier for VEHICLE_BREAKDOWN events (null for road events)
     */
    public record ScheduledDisruptionEvent(
            int atTick,
            String type,
            int fromNodeId,
            int toNodeId,
            int newWeight,
            String courierId) {
    }
}
