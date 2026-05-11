package com.routepulse.simulation.events;

import com.routepulse.domain.CourierId;
import com.routepulse.domain.NodeId;
import com.routepulse.domain.Order;

/**
 * Sealed interface for typed event payload data.
 * Each permitted subtype carries the exact data required by its event handler.
 * The EventDispatcher pattern-matches on the concrete type to extract payload fields.
 *
 * <p>Payload records are immutable by design — they are created once when the event
 * is scheduled and never modified thereafter.
 */
public sealed interface EventPayload
        permits EventPayload.NewOrderPayload,
                EventPayload.RoadWeightChangePayload,
                EventPayload.RoadRemovalPayload,
                EventPayload.VehicleBreakdownPayload,
                EventPayload.PriorityEscalationPayload,
                EventPayload.QuietPeriodPayload {

    /**
     * Payload for a NEW_ORDER event.
     * The order has been validated and is ready for greedy insertion.
     *
     * @param order the fully constructed delivery order to be assigned
     */
    record NewOrderPayload(Order order) implements EventPayload {
        public NewOrderPayload {
            if (order == null) throw new IllegalArgumentException("NewOrderPayload order must not be null");
        }
    }

    /**
     * Payload for a ROAD_WEIGHT_CHANGE event.
     * The specified edge weight will be updated; Dijkstra re-runs from all
     * affected courier source nodes.
     *
     * @param from      source node of the affected edge
     * @param to        destination node of the affected edge
     * @param newWeight the new traversal cost (1–20 per committed scale)
     */
    record RoadWeightChangePayload(NodeId from, NodeId to, int newWeight) implements EventPayload {
        public RoadWeightChangePayload {
            if (newWeight < 1 || newWeight > 20) {
                throw new IllegalArgumentException("RoadWeightChangePayload newWeight must be 1–20, got: " + newWeight);
            }
        }
    }

    /**
     * Payload for a ROAD_REMOVAL event.
     * The specified directed edge is permanently closed for the remainder of the shift.
     * Dijkstra re-runs for all couriers whose routes pass through this edge.
     *
     * @param from source node of the removed edge
     * @param to   destination node of the removed edge
     */
    record RoadRemovalPayload(NodeId from, NodeId to) implements EventPayload {
    }

    /**
     * Payload for a VEHICLE_BREAKDOWN event.
     * The specified courier is deactivated; all pending orders are redistributed
     * via FFD bin-packing and greedy re-insertion.
     *
     * @param courierId the courier whose vehicle has broken down
     */
    record VehicleBreakdownPayload(CourierId courierId) implements EventPayload {
        public VehicleBreakdownPayload {
            if (courierId == null) throw new IllegalArgumentException("VehicleBreakdownPayload courierId must not be null");
        }
    }

    /**
     * Payload for a PRIORITY_ESCALATION event.
     * The specified order's priority is raised to HIGH; it will be re-inserted
     * using priority-weighted cost in the greedy algorithm.
     *
     * @param orderId the identifier of the order to escalate
     */
    record PriorityEscalationPayload(String orderId) implements EventPayload {
        public PriorityEscalationPayload {
            if (orderId == null || orderId.isBlank()) {
                throw new IllegalArgumentException("PriorityEscalationPayload orderId must not be blank");
            }
        }
    }

    /**
     * Payload for a QUIET_PERIOD event.
     * Carries the number of ticks that have elapsed without a disruption event,
     * used to confirm the quiet period threshold was met.
     *
     * @param quietTickCount the number of consecutive quiet ticks that triggered this event
     */
    record QuietPeriodPayload(int quietTickCount) implements EventPayload {
        public QuietPeriodPayload {
            if (quietTickCount < 1) {
                throw new IllegalArgumentException("QuietPeriodPayload quietTickCount must be positive, got: " + quietTickCount);
            }
        }
    }
}
